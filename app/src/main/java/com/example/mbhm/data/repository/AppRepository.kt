package com.example.mbhm.data.repository

import android.content.Context
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.*
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

    suspend fun registerUser(
        username: String,
        password: String,
        role: Role,
        guardianDetails: com.example.mbhm.model.GuardianDetails? = null,
        boarderDetails: com.example.mbhm.model.BoarderDetails? = null
    ): UserEntity? {
        val trimmed = username.trim()
        if (trimmed.isEmpty() || password.isEmpty()) return null
        if (db.userDao().getByUsername(trimmed) != null) return null

        val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val boarderId = if (role == Role.BOARDER) UUID.randomUUID().toString() else null
        val newUser = UserEntity(
            username = trimmed,
            passwordHash = hashedPassword,
            role = role,
            boarderId = boarderId
        )
        db.userDao().insert(newUser)

        // A boarder login requires a matching boarder profile with full
        // contact info, emergency contact, guardian contact, and room details.
        if (role == Role.BOARDER && boarderId != null && boarderDetails != null) {
            val display = boarderDetails.name.ifBlank { trimmed }
            val initials = display.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
                .ifBlank { "?" }

            val boarder = Boarder(
                id = boarderId,
                name = display,
                room = boarderDetails.room.ifBlank { "—" },
                floor = boarderDetails.floor.ifBlank { "—" },
                initials = initials,
                avatarColor = "#4A7C59",
                phone = boarderDetails.phone.ifBlank { "—" },
                email = boarderDetails.email.ifBlank { "—" },
                joinDate = boarderDetails.joinDate.ifBlank { "—" },
                monthlyRate = boarderDetails.monthlyRate.toIntOrNull() ?: 0,
                dueDay = boarderDetails.dueDay.toIntOrNull() ?: 5,
                gracePeriodDays = 3,
                paymentStatus = PayStatus.PENDING,
                latePenaltyType = PenaltyType.FLAT,
                latePenaltyAmount = 0,
                emergencyContact = EmergencyContact(
                    name = boarderDetails.emergencyContactName,
                    relationship = boarderDetails.emergencyContactRelationship,
                    phone = boarderDetails.emergencyContactPhone,
                    secondaryPhone = boarderDetails.emergencyContactSecondaryPhone.ifBlank { null },
                    address = boarderDetails.emergencyContactAddress.ifBlank { null }
                ),
                guardianContact = GuardianContact(
                    name = boarderDetails.guardianContactName,
                    relationship = boarderDetails.guardianContactRelationship,
                    phone = boarderDetails.guardianContactPhone,
                    altPhone = boarderDetails.guardianContactAltPhone.ifBlank { null },
                    address = boarderDetails.guardianContactAddress.ifBlank { null }
                ),
                job = boarderDetails.job.ifBlank { "—" }
            )
            db.boarderDao().insert(BoarderEntity.fromDomain(boarder))
        }

        // TODO: Guardian profile creation will be handled through the Guardian
        // More → Edit Profile screen, not through self-registration.
        return newUser
    }

    // ─── Database Bootstrap ────────────────────────────────────────────────────

        /**
         * No-op: the app does not seed any mock data. Guardians and boarders
         * create all records through the UI.
         */
        suspend fun seedIfEmpty() { }

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
        db.paymentRecordDao().getAll().let { flow ->
            // Note: In practice you'd fetch by ID first
        }
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

    // ─── Schedule & Settings ───────────────────────────────────────────────────

    suspend fun getWorshipSchedule(): WorshipSchedule? =
        db.worshipScheduleDao().get()?.toDomain()

    suspend fun insertWorshipSchedule(schedule: WorshipSchedule) {
        db.worshipScheduleDao().insert(WorshipScheduleEntity.fromDomain(schedule))
    }

    suspend fun getBillingSettings(): BillingSettings? =
        db.billingSettingsDao().get()?.toDomain()

    suspend fun insertBillingSettings(settings: BillingSettings) {
        db.billingSettingsDao().insert(BillingSettingsEntity.fromDomain(settings))
    }
}
