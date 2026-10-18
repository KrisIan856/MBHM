package com.example.mbhm.data.repository

import android.content.Context
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.*
import com.example.mbhm.data.sync.SyncPayload
import com.example.mbhm.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AppRepository private constructor(context: Context) {
    private val db = AppDatabase.getInstance(context)
    val boarders = BoarderRepository(db)
    val payments = PaymentRepository(db)
    val attendance = AttendanceRepository(db)
    val leaveNotices = LeaveNoticeRepository(db.leaveNoticeDao())

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                AppRepository(context).also { INSTANCE = it }
            }
        }
    }

    // ─── Authentication ────────────────────────────────────────────────────────

    suspend fun getUserByUsername(username: String): UserEntity? {
        return db.userDao().getByUsername(username.trim())
    }

    suspend fun authenticateUser(username: String, password: String): UserEntity? {
        val user = db.userDao().getByUsername(username.trim()) ?: return null
        val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        return if (result.verified) user else null
    }

    /**
     * Only a guardian can self-register. The guardian's phone number becomes the
     * contact number shown to every boarder.
     */
    suspend fun registerUser(
        username: String,
        password: String,
        guardianDetails: com.example.mbhm.model.GuardianDetails
    ): UserEntity? {
        val trimmed = username.trim()
        if (trimmed.isEmpty() || password.isEmpty()) return null
        if (db.userDao().getByUsername(trimmed) != null) return null

        val newUser = UserEntity(
            username = trimmed,
            passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray()),
            role = Role.GUARDIAN,
            boarderId = null,
            displayName = guardianDetails.fullName.takeIf { it.isNotBlank() }
        )
        db.userDao().insert(newUser)

        db.guardianProfileDao().insert(
            GuardianProfileEntity.fromDomain(
                GuardianProfile(
                    name = guardianDetails.fullName,
                    phone = guardianDetails.phone,
                    email = "",
                    houseName = guardianDetails.houseName,
                    houseAddress = guardianDetails.houseAddress
                )
            )
        )
        return newUser
    }

    /** True when the ID number is already used by another boarder account. */
    suspend fun isIdNumberTaken(idNumber: String): Boolean =
        db.userDao().getByUsername(idNumber.trim()) != null

    /**
     * Guardian-managed boarder account creation: only the basic info is stored
     * (name, ID number, room) and the guardian issues the password.
     */
    suspend fun createBoarderAccount(
        input: NewBoarderInput,
        guardianProfile: GuardianProfile?
    ): String? {
        val idNumber = input.idNumber.trim()
        val name = input.name.trim()
        if (name.isEmpty() || idNumber.isEmpty()) return "Name and ID number are required"
        if (db.userDao().getByUsername(idNumber) != null) return "ID number is already in use"

        val room = input.room.trim()
        if (room.isEmpty()) return "Please select a room"

        val capacity = db.roomDao().getByName(room)?.maxCapacity ?: 0
        if (capacity > 0 && db.boarderDao().countInRoom(room) >= capacity) {
            return "Room $room is already full ($capacity/$capacity)"
        }

        val boarderId = UUID.randomUUID().toString()
        val hashed = BCrypt.withDefaults().hashToString(12, input.password.toCharArray())
        db.userDao().insert(
            UserEntity(
                username = idNumber,
                passwordHash = hashed,
                role = Role.BOARDER,
                boarderId = boarderId,
                displayName = name
            )
        )

        val billing = getBillingSettings()
        val initials = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { "—" }

        db.boarderDao().insert(
            BoarderEntity.fromDomain(
                Boarder(
                    id = boarderId,
                    name = name,
                    room = room,
                    floor = input.floor.trim().ifBlank { "—" },
                    initials = initials,
                    avatarColor = "#4A7C59",
                    phone = "",
                    email = "",
                    joinDate = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH)
                        .format(java.util.Date()),
                    monthlyRate = input.monthlyRate.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    dueDay = billing?.dueDay ?: 5,
                    gracePeriodDays = billing?.gracePeriodDays ?: 3,
                    paymentStatus = PayStatus.PENDING,
                    latePenaltyType = PenaltyType.FLAT,
                    latePenaltyAmount = 0,
                    emergencyContact = EmergencyContact(name = "", phone = ""),
                    guardianContact = GuardianContact(
                        name = guardianProfile?.name.orEmpty().ifBlank { "Guardian" },
                        phone = guardianProfile?.phone.orEmpty()
                    ),
                    job = input.job.trim().ifBlank { "—" },
                    idNumber = idNumber
                )
            )
        )
        return null
    }

    /** Guardian-side password reset for a boarder account (username == ID number). */
    suspend fun setAccountPassword(username: String, newPassword: String): Boolean {
        val user = db.userDao().getByUsername(username.trim()) ?: return false
        db.userDao().insert(
            user.copy(
                passwordHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
            )
        )
        return true
    }

    // ─── Rooms, Jobs & Guardian Profile ────────────────────────────────────────

    fun observeRooms(): Flow<List<HouseRoom>> =
        db.roomDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getRooms(): List<HouseRoom> = db.roomDao().getAllOnce().map { it.toDomain() }

    suspend fun saveRoom(room: HouseRoom) = db.roomDao().insert(RoomEntity.fromDomain(room))

    suspend fun deleteRoom(name: String) = db.roomDao().deleteByName(name)

    fun observeJobs(): Flow<List<HouseJob>> =
        db.jobDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getJobs(): List<HouseJob> = db.jobDao().getAllOnce().map { it.toDomain() }

    suspend fun saveJob(job: HouseJob) = db.jobDao().insert(JobEntity.fromDomain(job))

    suspend fun deleteJob(name: String) = db.jobDao().deleteByName(name)

    suspend fun getGuardianProfile(): GuardianProfile? =
        db.guardianProfileDao().get()?.toDomain()

    fun observeGuardianProfile(): Flow<GuardianProfile?> =
        db.guardianProfileDao().observe().map { it?.toDomain() }

    suspend fun saveGuardianProfile(profile: GuardianProfile) {
        db.guardianProfileDao().insert(GuardianProfileEntity.fromDomain(profile))
    }

    // ─── Database Bootstrap ────────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        if (db.billingSettingsDao().get() == null) {
            db.billingSettingsDao().insert(
                BillingSettingsEntity.fromDomain(
                    BillingSettings(
                        dueDay = 5,
                        gracePeriodDays = 3,
                        penaltyType = PenaltyType.FLAT,
                        penaltyAmount = 200
                    )
                )
            )
        }
        if (db.worshipScheduleDao().get() == null) {
            db.worshipScheduleDao().insert(
                WorshipScheduleEntity.fromDomain(
                    WorshipSchedule(
                        morningStart = "6:00 AM",
                        morningEnd = "8:00 AM",
                        morningQRMins = 120,
                        eveningStart = "8:00 PM",
                        eveningEnd = "10:00 PM",
                        eveningQRMins = 120,
                        absenceThreshold = 3
                    )
                )
            )
        }
        if (db.roomDao().getAllOnce().isEmpty()) {
            db.roomDao().insertAll(
                listOf(
                    RoomEntity("101", "1st Floor", 4),
                    RoomEntity("102", "1st Floor", 4),
                    RoomEntity("201", "2nd Floor", 4),
                    RoomEntity("301", "3rd Floor", 4)
                )
            )
        }
        if (db.jobDao().getAllOnce().isEmpty()) {
            db.jobDao().insertAll(
                listOf(JobEntity("Student"), JobEntity("Employee"), JobEntity("Other"))
            )
        }
    }

    suspend fun ensureTodaySessions(today: String, schedule: WorshipSchedule) {
        val pairs = listOf(
            SessionType.MORNING to (schedule.morningStart to schedule.morningEnd),
            SessionType.EVENING to (schedule.eveningStart to schedule.eveningEnd)
        )
        for ((type, window) in pairs) {
            if (db.attendanceSessionDao().getByDateAndType(today, type.name) == null) {
                db.attendanceSessionDao().insert(
                    AttendanceSessionEntity.fromDomain(
                        AttendanceSession(
                            id = "sess-$today-${type.name}",
                            type = type,
                            date = today,
                            windowStart = window.first,
                            windowEnd = window.second,
                            qrValidityMins = if (type == SessionType.MORNING) schedule.morningQRMins else schedule.eveningQRMins
                        )
                    )
                )
            }
        }
    }

    /**
     * Marks every boarder Absent for any session whose check-in window has already
     * ended and who still has no record for that session (today and any earlier day).
     * Runs against the device clock, so it can be called repeatedly (idempotent).
     */
    suspend fun markMissedAttendees(today: String, schedule: WorshipSchedule) {
        ensureTodaySessions(today, schedule)

        val now = System.currentTimeMillis()
        val sessions = db.attendanceSessionDao().getAllOnce()
        if (sessions.isEmpty()) return

        val boarders = db.boarderDao().getAllOnce()
        if (boarders.isEmpty()) return

        val recorded = db.attendanceRecordDao().getAllOnce()
        val timeFmt = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.ENGLISH)

        val pending = sessions.mapNotNull { sess ->
            val endMillis = runCatching { timeFmt.parse("${sess.date} ${sess.windowEnd}")?.time }
                .getOrNull() ?: return@mapNotNull null
            if (now <= endMillis) return@mapNotNull null

            val recordedIds = recorded.filter { it.sessionId == sess.id }.map { it.boarderId }.toSet()
            sess to boarders.filter { it.id !in recordedIds }
        }

        val toInsert = pending.flatMap { (sess, missed) ->
            missed.map { b ->
                AttendanceRecordEntity(
                    id = "auto-${sess.id}-${b.id}",
                    boarderId = b.id,
                    sessionId = sess.id,
                    status = AttendanceStatus.ABSENT,
                    checkInTime = null,
                    method = AttendanceMethod.MANUAL,
                    overrideReason = "Auto-marked absent · window ended ${sess.windowEnd}",
                    overriddenBy = "System"
                )
            }
        }

        if (toInsert.isNotEmpty()) db.attendanceRecordDao().insertAll(toInsert)
    }

    // ─── Boarder Operations ────────────────────────────────────────────────────

    fun getAllBoarders(): Flow<List<Boarder>> =
        db.boarderDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getBoarderById(id: String): Boarder? =
        db.boarderDao().getById(id)?.toDomain()

    suspend fun insertBoarder(boarder: Boarder) {
        db.boarderDao().insert(BoarderEntity.fromDomain(boarder))
    }

    suspend fun updateBoarder(boarder: Boarder) {
        db.boarderDao().update(BoarderEntity.fromDomain(boarder))
    }

    suspend fun deleteBoarder(id: String) {
        db.boarderDao().getById(id)?.let { db.boarderDao().delete(it) }
    }

    // ─── Payment Operations ────────────────────────────────────────────────────

    fun getAllPayments(): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getAll().map { list -> list.map { it.toDomain() } }

    fun getPaymentsByBoarder(boarderId: String): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getByBoarder(boarderId).map { list -> list.map { it.toDomain() } }

    suspend fun getPaymentByBoarderAndPeriod(boarderId: String, period: String): PaymentRecord? =
        db.paymentRecordDao().getByBoarderAndPeriod(boarderId, period)?.toDomain()

    suspend fun insertPayment(payment: PaymentRecord) {
        db.paymentRecordDao().insert(PaymentRecordEntity.fromDomain(payment))
    }

    suspend fun updatePayment(payment: PaymentRecord) {
        db.paymentRecordDao().update(PaymentRecordEntity.fromDomain(payment))
    }

    suspend fun deletePayment(id: String) {
        db.paymentRecordDao().deleteById(id)
    }

    suspend fun upsertPayments(records: List<PaymentRecord>) {
        db.paymentRecordDao().insertAll(records.map { PaymentRecordEntity.fromDomain(it) })
    }

    // ─── Attendance Operations ────────────────────────────────────────────────

    fun getAllAttendanceSessions(): Flow<List<AttendanceSession>> =
        db.attendanceSessionDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun insertAttendanceSession(session: AttendanceSession) {
        db.attendanceSessionDao().insert(AttendanceSessionEntity.fromDomain(session))
    }

    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>> =
        db.attendanceRecordDao().getAll().map { list -> list.map { it.toDomain() } }

    fun getAttendanceByBoarder(boarderId: String): Flow<List<AttendanceRecord>> =
        db.attendanceRecordDao().getByBoarder(boarderId).map { list -> list.map { it.toDomain() } }

    suspend fun insertAttendanceRecord(record: AttendanceRecord) {
        db.attendanceRecordDao().insert(AttendanceRecordEntity.fromDomain(record))
    }

    suspend fun updateAttendanceRecord(record: AttendanceRecord) {
        db.attendanceRecordDao().update(AttendanceRecordEntity.fromDomain(record))
    }

    suspend fun upsertAttendanceRecords(records: List<AttendanceRecord>) {
        db.attendanceRecordDao().insertAll(records.map { AttendanceRecordEntity.fromDomain(it) })
    }

    suspend fun upsertBoarders(boarders: List<Boarder>) {
        db.boarderDao().insertAll(boarders.map { BoarderEntity.fromDomain(it) })
    }

    // ─── Announcement Operations ───────────────────────────────────────────────

    fun getAllAnnouncements(): Flow<List<Announcement>> =
        db.announcementDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun insertAnnouncement(announcement: Announcement) {
        db.announcementDao().insert(AnnouncementEntity.fromDomain(announcement))
    }

    suspend fun updateAnnouncement(announcement: Announcement) {
        db.announcementDao().update(AnnouncementEntity.fromDomain(announcement))
    }

    suspend fun deleteAnnouncement(id: String) {
        db.announcementDao().getById(id)?.let { db.announcementDao().delete(it) }
    }

    suspend fun upsertAnnouncements(announcements: List<Announcement>) {
        db.announcementDao().insertAll(announcements.map { AnnouncementEntity.fromDomain(it) })
    }

    // ─── Visitor Operations ────────────────────────────────────────────────────

    fun getAllVisitors(): Flow<List<VisitorEntry>> =
        db.visitorEntryDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun insertVisitor(entry: VisitorEntry) {
        db.visitorEntryDao().insert(VisitorEntryEntity.fromDomain(entry))
    }

    suspend fun updateVisitor(entry: VisitorEntry) {
        db.visitorEntryDao().update(VisitorEntryEntity.fromDomain(entry))
    }

    // ─── Incident Operations ───────────────────────────────────────────────────

    fun getAllIncidents(): Flow<List<Incident>> =
        db.incidentDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun insertIncident(incident: Incident) {
        db.incidentDao().insert(IncidentEntity.fromDomain(incident))
    }

    suspend fun updateIncident(incident: Incident) {
        db.incidentDao().update(IncidentEntity.fromDomain(incident))
    }

    suspend fun upsertIncidents(incidents: List<Incident>) {
        db.incidentDao().insertAll(incidents.map { IncidentEntity.fromDomain(it) })
    }

    // ─── Maintenance Operations ────────────────────────────────────────────────

    fun getAllMaintenanceReports(): Flow<List<MaintenanceReport>> =
        db.maintenanceReportDao().getAll().map { list -> list.map { it.toDomain() } }

    fun getMaintenanceByBoarder(boarderId: String): Flow<List<MaintenanceReport>> =
        db.maintenanceReportDao().getByBoarder(boarderId).map { list -> list.map { it.toDomain() } }

    suspend fun insertMaintenanceReport(report: MaintenanceReport) {
        db.maintenanceReportDao().insert(MaintenanceReportEntity.fromDomain(report))
    }

    suspend fun updateMaintenanceReport(report: MaintenanceReport) {
        db.maintenanceReportDao().update(MaintenanceReportEntity.fromDomain(report))
    }

    suspend fun upsertMaintenanceReports(reports: List<MaintenanceReport>) {
        db.maintenanceReportDao().insertAll(reports.map { MaintenanceReportEntity.fromDomain(it) })
    }

    // ─── Curfew Operations ─────────────────────────────────────────────────────

    fun getAllCurfewRecords(): Flow<List<CurfewRecord>> =
        db.curfewRecordDao().getAll().map { list -> list.map { it.toDomain() } }

    fun getCurfewByBoarder(boarderId: String): Flow<List<CurfewRecord>> =
        db.curfewRecordDao().getByBoarder(boarderId).map { list -> list.map { it.toDomain() } }

    suspend fun insertCurfewRecord(record: CurfewRecord) {
        db.curfewRecordDao().insert(CurfewRecordEntity.fromDomain(record))
    }

    suspend fun updateCurfewRecord(record: CurfewRecord) {
        db.curfewRecordDao().update(CurfewRecordEntity.fromDomain(record))
    }

    suspend fun upsertCurfewRecords(records: List<CurfewRecord>) {
        db.curfewRecordDao().insertAll(records.map { CurfewRecordEntity.fromDomain(it) })
    }

    suspend fun upsertLeaveNotices(notices: List<LeaveNotice>) {
        db.leaveNoticeDao().insertAll(notices.map { LeaveNoticeEntity.fromDomain(it) })
    }

    fun observeLeaveNotices(): Flow<List<LeaveNotice>> =
        db.leaveNoticeDao().getAll().map { list -> list.map { it.toDomain() } }

    // ─── Schedule & Settings ───────────────────────────────────────────────────

    suspend fun getWorshipSchedule(): WorshipSchedule? =
        db.worshipScheduleDao().get()?.toDomain()

    suspend fun insertWorshipSchedule(schedule: WorshipSchedule) {
        db.worshipScheduleDao().insert(WorshipScheduleEntity.fromDomain(schedule))
    }

    fun observeWorshipSchedule(): Flow<WorshipSchedule?> =
        db.worshipScheduleDao().observe().map { it?.toDomain() }

    suspend fun getBillingSettings(): BillingSettings? =
        db.billingSettingsDao().get()?.toDomain()

    suspend fun insertBillingSettings(settings: BillingSettings) {
        db.billingSettingsDao().insert(BillingSettingsEntity.fromDomain(settings))
    }

    fun observeBillingSettings(): Flow<BillingSettings?> =
        db.billingSettingsDao().observe().map { it?.toDomain() }

    // ─── Absence Alert Operations ──────────────────────────────────────────────

    fun getAllAbsenceAlerts(): Flow<List<AbsenceAlertRecord>> =
        db.absenceAlertDao().getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAbsenceAlertsByBoarder(boarderId: String): List<AbsenceAlertRecord> =
        db.absenceAlertDao().getByBoarder(boarderId).map { it.toDomain() }

    suspend fun upsertAbsenceAlerts(alerts: List<AbsenceAlertRecord>) {
        db.absenceAlertDao().insertAll(alerts.map { AbsenceAlertEntity.fromDomain(it) })
    }

    suspend fun deleteAbsenceAlert(id: String) {
        db.absenceAlertDao().deleteById(id)
    }

    // ─── Device Sync ─────────────────────────────────────────────────────────

    suspend fun exportSyncPayload(deviceName: String): SyncPayload = SyncPayload(
        deviceName = deviceName,
        users = db.userDao().getAllOnce(),
        boarders = db.boarderDao().getAllOnce(),
        paymentRecords = db.paymentRecordDao().getAllOnce(),
        attendanceSessions = db.attendanceSessionDao().getAllOnce(),
        attendanceRecords = db.attendanceRecordDao().getAllOnce(),
        announcements = db.announcementDao().getAllOnce(),
        visitorEntries = db.visitorEntryDao().getAllOnce(),
        incidents = db.incidentDao().getAllOnce(),
        maintenanceReports = db.maintenanceReportDao().getAllOnce(),
        curfewRecords = db.curfewRecordDao().getAllOnce(),
        worshipSchedules = db.worshipScheduleDao().get()?.let { listOf(it) } ?: emptyList(),
        billingSettings = db.billingSettingsDao().get()?.let { listOf(it) } ?: emptyList(),
        leaveNotices = db.leaveNoticeDao().getAllOnce(),
        absenceAlerts = db.absenceAlertDao().getAllOnce(),
        rooms = db.roomDao().getAllOnce(),
        jobs = db.jobDao().getAllOnce(),
        guardianProfile = db.guardianProfileDao().get()?.let { listOf(it) } ?: emptyList()
    )

    suspend fun importSyncPayload(payload: SyncPayload): Int {
        var imported = 0

        val users = payload.users.orEmpty()
        if (users.isNotEmpty()) {
            db.userDao().insertAll(users); imported += users.size
        }
        val boarders = payload.boarders.orEmpty()
        if (boarders.isNotEmpty()) {
            db.boarderDao().insertAll(boarders); imported += boarders.size
        }
        val paymentRecords = payload.paymentRecords.orEmpty()
        if (paymentRecords.isNotEmpty()) {
            db.paymentRecordDao().insertAll(paymentRecords); imported += paymentRecords.size
        }
        val attendanceSessions = payload.attendanceSessions.orEmpty()
        if (attendanceSessions.isNotEmpty()) {
            db.attendanceSessionDao().insertAll(attendanceSessions); imported += attendanceSessions.size
        }
        val attendanceRecords = payload.attendanceRecords.orEmpty()
        if (attendanceRecords.isNotEmpty()) {
            db.attendanceRecordDao().insertAll(attendanceRecords); imported += attendanceRecords.size
        }
        val announcements = payload.announcements.orEmpty()
        if (announcements.isNotEmpty()) {
            db.announcementDao().insertAll(announcements); imported += announcements.size
        }
        val visitorEntries = payload.visitorEntries.orEmpty()
        if (visitorEntries.isNotEmpty()) {
            db.visitorEntryDao().insertAll(visitorEntries); imported += visitorEntries.size
        }
        val incidents = payload.incidents.orEmpty()
        if (incidents.isNotEmpty()) {
            db.incidentDao().insertAll(incidents); imported += incidents.size
        }
        val maintenanceReports = payload.maintenanceReports.orEmpty()
        if (maintenanceReports.isNotEmpty()) {
            db.maintenanceReportDao().insertAll(maintenanceReports); imported += maintenanceReports.size
        }
        val curfewRecords = payload.curfewRecords.orEmpty()
        if (curfewRecords.isNotEmpty()) {
            db.curfewRecordDao().insertAll(curfewRecords); imported += curfewRecords.size
        }
        val leaveNotices = payload.leaveNotices.orEmpty()
        if (leaveNotices.isNotEmpty()) {
            db.leaveNoticeDao().insertAll(leaveNotices); imported += leaveNotices.size
        }
        val absenceAlerts = payload.absenceAlerts.orEmpty()
        if (absenceAlerts.isNotEmpty()) {
            db.absenceAlertDao().insertAll(absenceAlerts); imported += absenceAlerts.size
        }
        payload.worshipSchedules.orEmpty().firstOrNull()?.let {
            db.worshipScheduleDao().insert(it); imported += 1
        }
        payload.billingSettings.orEmpty().firstOrNull()?.let {
            db.billingSettingsDao().insert(it); imported += 1
        }
        payload.guardianProfile.orEmpty().firstOrNull()?.let {
            db.guardianProfileDao().insert(it); imported += 1
        }
        val rooms = payload.rooms.orEmpty()
        if (rooms.isNotEmpty()) {
            db.roomDao().insertAll(rooms); imported += rooms.size
        }
        val jobs = payload.jobs.orEmpty()
        if (jobs.isNotEmpty()) {
            db.jobDao().insertAll(jobs); imported += jobs.size
        }

        return imported
    }

    suspend fun deleteSyncedRecords(deletes: Map<String, List<String>>?) {
        deletes ?: return
        deletes["users"]?.forEach { db.userDao().deleteById(it) }
        deletes["boarders"]?.forEach { db.boarderDao().deleteById(it) }
        deletes["paymentRecords"]?.forEach { db.paymentRecordDao().deleteById(it) }
        deletes["attendanceSessions"]?.forEach { db.attendanceSessionDao().deleteById(it) }
        deletes["attendanceRecords"]?.forEach { db.attendanceRecordDao().deleteById(it) }
        deletes["announcements"]?.forEach { db.announcementDao().deleteById(it) }
        deletes["visitorEntries"]?.forEach { db.visitorEntryDao().deleteById(it) }
        deletes["incidents"]?.forEach { db.incidentDao().deleteById(it) }
        deletes["maintenanceReports"]?.forEach { db.maintenanceReportDao().deleteById(it) }
        deletes["curfewRecords"]?.forEach { db.curfewRecordDao().deleteById(it) }
        deletes["worshipSchedules"]?.forEach { db.worshipScheduleDao().deleteById(it) }
        deletes["billingSettings"]?.forEach { db.billingSettingsDao().deleteById(it) }
        deletes["leaveNotices"]?.forEach { db.leaveNoticeDao().deleteById(it) }
        deletes["absenceAlerts"]?.forEach { db.absenceAlertDao().deleteById(it) }
        deletes["rooms"]?.forEach { db.roomDao().deleteByName(it) }
        deletes["jobs"]?.forEach { db.jobDao().deleteByName(it) }
    }
}
