package com.example.mbhm.model

enum class Role { GUARDIAN, BOARDER }

enum class PayStatus { PAID, PARTIAL, PENDING, OVERDUE }

enum class ReceiptStatus { PENDING_REVIEW, VERIFIED, REJECTED }

enum class IncidentType { MISSED_CURFEW, SOS, MISSED_WORSHIP, MAINTENANCE, OTHER }

enum class CurfewStatus { COMPLIANT, LATE, ABSENT, PENDING }

enum class LeaveNoticeStatus { PENDING, APPROVED, REJECTED, DEPARTED, COMPLETED }

enum class PayMethod(val label: String) {
    CASH("Cash"),
    GCASH("GCash"),
    BANK_TRANSFER("Bank Transfer"),
    ONLINE("Online")
}

enum class RecordedBy { BOARDER, GUARDIAN }

enum class AttendanceStatus { PRESENT, ABSENT }

enum class AttendanceMethod { QR, MANUAL }

enum class SessionType { MORNING, EVENING }

enum class Priority { NORMAL, CRITICAL }

enum class PenaltyType { FLAT, PERCENTAGE }

enum class MaintCategory(val label: String) {
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    FURNITURE("Furniture"),
    APPLIANCE("Appliance"),
    OTHER("Other")
}

enum class MaintStatus { OPEN, IN_PROGRESS, RESOLVED }

data class EmergencyContact(
    val name: String,
    val phone: String,
    val secondaryPhone: String? = null,
    val address: String? = null
)

data class GuardianContact(
    val name: String,
    val phone: String,
    val altPhone: String? = null,
    val address: String? = null
)

/** A room the guardian manages, including its maximum boarder capacity. */
data class HouseRoom(
    val name: String,
    val floor: String,
    val maxCapacity: Int
)

/** A job / occupation option offered in the boarder form dropdown. */
data class HouseJob(
    val name: String
)

/** The single guardian profile shared with every boarder. */
data class GuardianProfile(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val houseName: String = "",
    val houseAddress: String = ""
)

data class Boarder(
    val id: String,
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
    val emergencyContact: EmergencyContact,
    val guardianContact: GuardianContact,
    val job: String,
    val idNumber: String = ""
) {
    val firstName: String get() = name.substringBefore(' ')
}

data class LeaveNotice(
    val id: String,
    val boarderId: String,
    val leaveDate: String,
    val expectedDepartureTime: String,
    val expectedReturnDate: String,
    val expectedReturnTime: String,
    val reason: String,
    val destination: String,
    val additionalNotes: String? = null,
    val status: LeaveNoticeStatus = LeaveNoticeStatus.PENDING,
    val submittedAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val departedAt: String? = null,
    val arrivedAt: String? = null
)

data class PaymentRecord(
    val id: String,
    val boarderId: String,
    val period: String,
    val amount: Int,
    val paidDate: String? = null,
    val method: PayMethod,
    val status: PayStatus,
    val receiptStatus: ReceiptStatus? = null,
    val receiptFileName: String? = null,
    val rejectionReason: String? = null,
    val amountPaid: Int? = null,
    val guardianReceiptFileName: String? = null,
    val recordedBy: RecordedBy,
    val verifiedAt: String? = null,
    val verifiedBy: String? = null,
    val notes: String? = null
)

data class AttendanceSession(
    val id: String,
    val type: SessionType,
    val date: String,
    val windowStart: String,
    val windowEnd: String,
    val qrValidityMins: Int
)

data class AttendanceRecord(
    val id: String,
    val boarderId: String,
    val sessionId: String,
    val status: AttendanceStatus,
    val checkInTime: String? = null,
    val method: AttendanceMethod,
    val overrideReason: String? = null,
    val overriddenBy: String? = null
)

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val priority: Priority,
    /** null means "all boarders" */
    val recipients: List<String>? = null,
    val sentAt: String,
    val sentBy: String,
    val readBy: List<String>,
    val acknowledgedBy: List<String>
) {
    fun isFor(boarderId: String): Boolean =
        recipients == null || recipients.contains(boarderId)
}

data class VisitorEntry(
    val id: String,
    val visitorName: String,
    val boarderId: String,
    val purpose: String,
    val timeIn: String,
    val timeOut: String? = null,
    val date: String
)

data class Incident(
    val id: String,
    val type: IncidentType,
    val boarderId: String? = null,
    val title: String,
    val description: String,
    val timestamp: String,
    val resolved: Boolean
)

data class MaintenanceReport(
    val id: String,
    val boarderId: String,
    val room: String,
    val category: MaintCategory,
    val description: String,
    val status: MaintStatus,
    val submittedAt: String,
    val resolvedAt: String? = null
)

data class CurfewRecord(
    val id: String,
    val boarderId: String,
    val date: String,
    val curfewTime: String,
    val checkedInAt: String? = null,
    val status: CurfewStatus
)

data class WorshipSchedule(
    val morningStart: String,
    val morningEnd: String,
    val morningQRMins: Int,
    val eveningStart: String,
    val eveningEnd: String,
    val eveningQRMins: Int,
    val absenceThreshold: Int
)

/**
 * One absence-alert notice sent to a boarder's guardian.
 *
 * [absenceCountAtSend] is the boarder's absence total when the alert went out. A further
 * alert is only warranted once that total rises, so a guardian who keeps missing without
 * being re-notified every day and is re-notified as soon as they miss again.
 */
data class AbsenceAlertRecord(
    val id: String,
    val boarderId: String,
    val absenceCountAtSend: Int,
    val thresholdAtSend: Int,
    val sentOn: String,
    val sentAt: String? = null
)

data class ReminderSettings(
    val before3: Boolean = true,
    val before1: Boolean = true,
    val onDay: Boolean = true,
    val after1: Boolean = true,
    val after3: Boolean = true
)

data class ChannelSettings(
    val inApp: Boolean = true,
    val sms: Boolean = false,
    val email: Boolean = false
)

data class BillingSettings(
    val dueDay: Int,
    val gracePeriodDays: Int,
    val penaltyType: PenaltyType,
    val penaltyAmount: Int,
    val reminders: ReminderSettings = ReminderSettings(),
    val channels: ChannelSettings = ChannelSettings()
)

data class GuardianDetails(
    val fullName: String = "",
    val phone: String = "",
    val houseName: String = "",
    val houseAddress: String = ""
)

/**
 * Everything a guardian fills in when creating a boarder account.
 * Only the boarder's basic info is collected here — the guardian issues
 * the ID number and creates the password.
 */
data class NewBoarderInput(
    val name: String = "",
    val idNumber: String = "",
    val room: String = "",
    val floor: String = "",
    val job: String = "",
    val monthlyRate: String = "",
    val password: String = "",
    val confirm: String = ""
)
