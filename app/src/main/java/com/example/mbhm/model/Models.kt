package com.example.mbhm.model

enum class Role { GUARDIAN, BOARDER }

enum class PayStatus { PAID, PARTIAL, PENDING, OVERDUE }

enum class ReceiptStatus { PENDING_REVIEW, VERIFIED, REJECTED }

enum class IncidentType { MISSED_CURFEW, SOS, MISSED_WORSHIP, MAINTENANCE, OTHER }

enum class CurfewStatus { COMPLIANT, LATE, ABSENT, PENDING }

enum class LeaveNoticeStatus { PENDING, APPROVED, REJECTED, COMPLETED }

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
    val relationship: String,
    val phone: String,
    val secondaryPhone: String? = null,
    val address: String? = null
)

data class GuardianContact(
    val name: String,
    val relationship: String,
    val phone: String,
    val altPhone: String? = null,
    val address: String? = null
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
    val job: String
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
    val submittedAt: String
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

data class BoarderDetails(
    val name: String = "",
    val room: String = "",
    val floor: String = "",
    val phone: String = "",
    val email: String = "",
    val joinDate: String = "",
    val monthlyRate: String = "",
    val dueDay: String = "5",
    val gracePeriodDays: String = "5",
    val latePenaltyType: String = "FLAT",
    val latePenaltyAmount: String = "0",
    val job: String = "",
    val emergencyContactName: String = "",
    val emergencyContactRelationship: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactSecondaryPhone: String = "",
    val emergencyContactAddress: String = "",
    val guardianContactName: String = "",
    val guardianContactRelationship: String = "",
    val guardianContactPhone: String = "",
    val guardianContactAltPhone: String = "",
    val guardianContactAddress: String = ""
)
