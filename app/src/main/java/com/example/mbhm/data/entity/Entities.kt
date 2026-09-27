package com.example.mbhm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.mbhm.model.*
import com.example.mbhm.data.converter.Converters

@Entity(tableName = "boarders")
@TypeConverters(Converters::class)
data class BoarderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val room: String,
    val floor: String,
    val initials: String,
    val avatarColor: String,
    val phone: String,
    val email: String,
    val joinDate: String,
    val monthlyRate: Int,
    val dueDay: Int,
    val gracePeriodDays: Int,
    val paymentStatus: PayStatus,
    val latePenaltyType: PenaltyType,
    val latePenaltyAmount: Int,
    val emergencyContactName: String,
    val emergencyContactRelationship: String,
    val emergencyContactPhone: String,
    val emergencyContactSecondaryPhone: String? = null,
    val emergencyContactAddress: String? = null,
    val guardianContactName: String,
    val guardianContactRelationship: String,
    val guardianContactPhone: String,
    val guardianContactAltPhone: String? = null,
    val guardianContactAddress: String? = null,
    val job: String
) {
    val firstName: String get() = name.substringBefore(' ')

    fun toDomain(): Boarder = Boarder(
        id = id,
        name = name,
        room = room,
        floor = floor,
        initials = initials,
        avatarColor = avatarColor,
        phone = phone,
        email = email,
        joinDate = joinDate,
        monthlyRate = monthlyRate,
        dueDay = dueDay,
        gracePeriodDays = gracePeriodDays,
        paymentStatus = paymentStatus,
        latePenaltyType = latePenaltyType,
        latePenaltyAmount = latePenaltyAmount,
        emergencyContact = EmergencyContact(
            name = emergencyContactName,
            relationship = emergencyContactRelationship,
            phone = emergencyContactPhone,
            secondaryPhone = emergencyContactSecondaryPhone,
            address = emergencyContactAddress
        ),
        guardianContact = GuardianContact(
            name = guardianContactName,
            relationship = guardianContactRelationship,
            phone = guardianContactPhone,
            altPhone = guardianContactAltPhone,
            address = guardianContactAddress
        ),
        job = job
    )

    companion object {
        fun fromDomain(boarder: Boarder): BoarderEntity = BoarderEntity(
            id = boarder.id,
            name = boarder.name,
            room = boarder.room,
            floor = boarder.floor,
            initials = boarder.initials,
            avatarColor = boarder.avatarColor,
            phone = boarder.phone,
            email = boarder.email,
            joinDate = boarder.joinDate,
            monthlyRate = boarder.monthlyRate,
            dueDay = boarder.dueDay,
            gracePeriodDays = boarder.gracePeriodDays,
            paymentStatus = boarder.paymentStatus,
            latePenaltyType = boarder.latePenaltyType,
            latePenaltyAmount = boarder.latePenaltyAmount,
            emergencyContactName = boarder.emergencyContact.name,
            emergencyContactRelationship = boarder.emergencyContact.relationship,
            emergencyContactPhone = boarder.emergencyContact.phone,
            emergencyContactSecondaryPhone = boarder.emergencyContact.secondaryPhone,
            emergencyContactAddress = boarder.emergencyContact.address,
            guardianContactName = boarder.guardianContact.name,
            guardianContactRelationship = boarder.guardianContact.relationship,
            guardianContactPhone = boarder.guardianContact.phone,
            guardianContactAltPhone = boarder.guardianContact.altPhone,
            guardianContactAddress = boarder.guardianContact.address,
            job = boarder.job
        )
    }
}

@Entity(tableName = "payment_records")
@TypeConverters(Converters::class)
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val period: String,
    val amount: Int,
    val paidDate: String?,
    val method: PayMethod,
    val status: PayStatus,
    val receiptStatus: ReceiptStatus?,
    val receiptFileName: String?,
    val rejectionReason: String?,
    val amountPaid: Int?,
    val guardianReceiptFileName: String?,
    val recordedBy: RecordedBy,
    val verifiedAt: String?,
    val verifiedBy: String?,
    val notes: String?
) {
    fun toDomain(): PaymentRecord = PaymentRecord(
        id = id,
        boarderId = boarderId,
        period = period,
        amount = amount,
        paidDate = paidDate,
        method = method,
        status = status,
        receiptStatus = receiptStatus,
        receiptFileName = receiptFileName,
        rejectionReason = rejectionReason,
        amountPaid = amountPaid,
        guardianReceiptFileName = guardianReceiptFileName,
        recordedBy = recordedBy,
        verifiedAt = verifiedAt,
        verifiedBy = verifiedBy,
        notes = notes
    )

    companion object {
        fun fromDomain(record: PaymentRecord): PaymentRecordEntity = PaymentRecordEntity(
            id = record.id,
            boarderId = record.boarderId,
            period = record.period,
            amount = record.amount,
            paidDate = record.paidDate,
            method = record.method,
            status = record.status,
            receiptStatus = record.receiptStatus,
            receiptFileName = record.receiptFileName,
            rejectionReason = record.rejectionReason,
            amountPaid = record.amountPaid,
            guardianReceiptFileName = record.guardianReceiptFileName,
            recordedBy = record.recordedBy,
            verifiedAt = record.verifiedAt,
            verifiedBy = record.verifiedBy,
            notes = record.notes
        )
    }
}

@Entity(tableName = "attendance_sessions")
@TypeConverters(Converters::class)
data class AttendanceSessionEntity(
    @PrimaryKey val id: String,
    val type: SessionType,
    val date: String,
    val windowStart: String,
    val windowEnd: String,
    val qrValidityMins: Int
) {
    fun toDomain(): AttendanceSession = AttendanceSession(
        id = id,
        type = type,
        date = date,
        windowStart = windowStart,
        windowEnd = windowEnd,
        qrValidityMins = qrValidityMins
    )

    companion object {
        fun fromDomain(session: AttendanceSession): AttendanceSessionEntity = AttendanceSessionEntity(
            id = session.id,
            type = session.type,
            date = session.date,
            windowStart = session.windowStart,
            windowEnd = session.windowEnd,
            qrValidityMins = session.qrValidityMins
        )
    }
}

@Entity(tableName = "attendance_records",
    indices = [androidx.room.Index(value = ["boarderId", "sessionId"], unique = true)])
@TypeConverters(Converters::class)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val sessionId: String,
    val status: AttendanceStatus,
    val checkInTime: String?,
    val method: AttendanceMethod,
    val overrideReason: String?,
    val overriddenBy: String?
) {
    fun toDomain(): AttendanceRecord = AttendanceRecord(
        id = id,
        boarderId = boarderId,
        sessionId = sessionId,
        status = status,
        checkInTime = checkInTime,
        method = method,
        overrideReason = overrideReason,
        overriddenBy = overriddenBy
    )

    companion object {
        fun fromDomain(record: AttendanceRecord): AttendanceRecordEntity = AttendanceRecordEntity(
            id = record.id,
            boarderId = record.boarderId,
            sessionId = record.sessionId,
            status = record.status,
            checkInTime = record.checkInTime,
            method = record.method,
            overrideReason = record.overrideReason,
            overriddenBy = record.overriddenBy
        )
    }
}

@Entity(tableName = "announcements")
@TypeConverters(Converters::class)
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val priority: Priority,
    val recipients: String?, // JSON array of boarder IDs, null = all
    val sentAt: String,
    val sentBy: String,
    val readBy: String, // JSON array
    val acknowledgedBy: String // JSON array
) {
    fun toDomain(): Announcement = Announcement(
        id = id,
        title = title,
        body = body,
        priority = priority,
        recipients = Converters.stringToList(recipients),
        sentAt = sentAt,
        sentBy = sentBy,
readBy = Converters.stringToList(readBy) ?: emptyList(),
            acknowledgedBy = Converters.stringToList(acknowledgedBy) ?: emptyList()
    )

    companion object {
        fun fromDomain(announcement: Announcement): AnnouncementEntity = AnnouncementEntity(
            id = announcement.id,
            title = announcement.title,
            body = announcement.body,
            priority = announcement.priority,
            recipients = Converters.listToString(announcement.recipients),
            sentAt = announcement.sentAt,
            sentBy = announcement.sentBy,
            readBy = Converters.listToString(announcement.readBy) ?: "[]",
            acknowledgedBy = Converters.listToString(announcement.acknowledgedBy) ?: "[]"
        )
    }
}

@Entity(tableName = "visitor_entries")
@TypeConverters(Converters::class)
data class VisitorEntryEntity(
    @PrimaryKey val id: String,
    val visitorName: String,
    val boarderId: String,
    val purpose: String,
    val timeIn: String,
    val timeOut: String?,
    val date: String
) {
    fun toDomain(): VisitorEntry = VisitorEntry(
        id = id,
        visitorName = visitorName,
        boarderId = boarderId,
        purpose = purpose,
        timeIn = timeIn,
        timeOut = timeOut,
        date = date
    )

    companion object {
        fun fromDomain(entry: VisitorEntry): VisitorEntryEntity = VisitorEntryEntity(
            id = entry.id,
            visitorName = entry.visitorName,
            boarderId = entry.boarderId,
            purpose = entry.purpose,
            timeIn = entry.timeIn,
            timeOut = entry.timeOut,
            date = entry.date
        )
    }
}

@Entity(tableName = "incidents")
@TypeConverters(Converters::class)
data class IncidentEntity(
    @PrimaryKey val id: String,
    val type: IncidentType,
    val boarderId: String?,
    val title: String,
    val description: String,
    val timestamp: String,
    val resolved: Boolean
) {
    fun toDomain(): Incident = Incident(
        id = id,
        type = type,
        boarderId = boarderId,
        title = title,
        description = description,
        timestamp = timestamp,
        resolved = resolved
    )

    companion object {
        fun fromDomain(incident: Incident): IncidentEntity = IncidentEntity(
            id = incident.id,
            type = incident.type,
            boarderId = incident.boarderId,
            title = incident.title,
            description = incident.description,
            timestamp = incident.timestamp,
            resolved = incident.resolved
        )
    }
}

@Entity(tableName = "maintenance_reports")
@TypeConverters(Converters::class)
data class MaintenanceReportEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val room: String,
    val category: MaintCategory,
    val description: String,
    val status: MaintStatus,
    val submittedAt: String,
    val resolvedAt: String?
) {
    fun toDomain(): MaintenanceReport = MaintenanceReport(
        id = id,
        boarderId = boarderId,
        room = room,
        category = category,
        description = description,
        status = status,
        submittedAt = submittedAt,
        resolvedAt = resolvedAt
    )

    companion object {
        fun fromDomain(report: MaintenanceReport): MaintenanceReportEntity = MaintenanceReportEntity(
            id = report.id,
            boarderId = report.boarderId,
            room = report.room,
            category = report.category,
            description = report.description,
            status = report.status,
            submittedAt = report.submittedAt,
            resolvedAt = report.resolvedAt
        )
    }
}

@Entity(tableName = "curfew_records")
@TypeConverters(Converters::class)
data class CurfewRecordEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val date: String,
    val curfewTime: String,
    val checkedInAt: String?,
    val status: CurfewStatus
) {
    fun toDomain(): CurfewRecord = CurfewRecord(
        id = id,
        boarderId = boarderId,
        date = date,
        curfewTime = curfewTime,
        checkedInAt = checkedInAt,
        status = status
    )

    companion object {
        fun fromDomain(record: CurfewRecord): CurfewRecordEntity = CurfewRecordEntity(
            id = record.id,
            boarderId = record.boarderId,
            date = record.date,
            curfewTime = record.curfewTime,
            checkedInAt = record.checkedInAt,
            status = record.status
        )
    }
}

@Entity(tableName = "worship_schedule")
@TypeConverters(Converters::class)
data class WorshipScheduleEntity(
    @PrimaryKey val id: String = "default",
    val morningStart: String,
    val morningEnd: String,
    val morningQRMins: Int,
    val eveningStart: String,
    val eveningEnd: String,
    val eveningQRMins: Int,
    val absenceThreshold: Int
) {
    fun toDomain(): WorshipSchedule = WorshipSchedule(
        morningStart = morningStart,
        morningEnd = morningEnd,
        morningQRMins = morningQRMins,
        eveningStart = eveningStart,
        eveningEnd = eveningEnd,
        eveningQRMins = eveningQRMins,
        absenceThreshold = absenceThreshold
    )

    companion object {
        fun fromDomain(schedule: WorshipSchedule): WorshipScheduleEntity = WorshipScheduleEntity(
            morningStart = schedule.morningStart,
            morningEnd = schedule.morningEnd,
            morningQRMins = schedule.morningQRMins,
            eveningStart = schedule.eveningStart,
            eveningEnd = schedule.eveningEnd,
            eveningQRMins = schedule.eveningQRMins,
            absenceThreshold = schedule.absenceThreshold
        )
    }
}

@Entity(tableName = "billing_settings")
@TypeConverters(Converters::class)
data class BillingSettingsEntity(
    @PrimaryKey val id: String = "default",
    val dueDay: Int,
    val gracePeriodDays: Int,
    val penaltyType: PenaltyType,
    val penaltyAmount: Int,
    val reminderBefore3: Boolean,
    val reminderBefore1: Boolean,
    val reminderOnDay: Boolean,
    val reminderAfter1: Boolean,
    val reminderAfter3: Boolean,
    val channelInApp: Boolean,
    val channelSms: Boolean,
    val channelEmail: Boolean
) {
    fun toDomain(): BillingSettings = BillingSettings(
        dueDay = dueDay,
        gracePeriodDays = gracePeriodDays,
        penaltyType = penaltyType,
        penaltyAmount = penaltyAmount,
        reminders = ReminderSettings(
            before3 = reminderBefore3,
            before1 = reminderBefore1,
            onDay = reminderOnDay,
            after1 = reminderAfter1,
            after3 = reminderAfter3
        ),
        channels = ChannelSettings(
            inApp = channelInApp,
            sms = channelSms,
            email = channelEmail
        )
    )

    companion object {
        fun fromDomain(settings: BillingSettings): BillingSettingsEntity = BillingSettingsEntity(
            dueDay = settings.dueDay,
            gracePeriodDays = settings.gracePeriodDays,
            penaltyType = settings.penaltyType,
            penaltyAmount = settings.penaltyAmount,
            reminderBefore3 = settings.reminders.before3,
            reminderBefore1 = settings.reminders.before1,
            reminderOnDay = settings.reminders.onDay,
            reminderAfter1 = settings.reminders.after1,
            reminderAfter3 = settings.reminders.after3,
            channelInApp = settings.channels.inApp,
            channelSms = settings.channels.sms,
            channelEmail = settings.channels.email
        )
    }
}
