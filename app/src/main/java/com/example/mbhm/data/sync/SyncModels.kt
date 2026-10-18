package com.example.mbhm.data.sync

import com.example.mbhm.data.entity.*

data class SyncPayload(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceName: String = "",
    val users: List<UserEntity> = emptyList(),
    val boarders: List<BoarderEntity> = emptyList(),
    val paymentRecords: List<PaymentRecordEntity> = emptyList(),
    val attendanceSessions: List<AttendanceSessionEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val announcements: List<AnnouncementEntity> = emptyList(),
    val visitorEntries: List<VisitorEntryEntity> = emptyList(),
    val incidents: List<IncidentEntity> = emptyList(),
    val maintenanceReports: List<MaintenanceReportEntity> = emptyList(),
    val curfewRecords: List<CurfewRecordEntity> = emptyList(),
    val worshipSchedules: List<WorshipScheduleEntity> = emptyList(),
    val billingSettings: List<BillingSettingsEntity> = emptyList(),
    val leaveNotices: List<LeaveNoticeEntity> = emptyList(),
    val absenceAlerts: List<AbsenceAlertEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val jobs: List<JobEntity> = emptyList(),
    val guardianProfile: List<GuardianProfileEntity> = emptyList()
)

data class SyncStatusResponse(
    val status: String,
    val deviceName: String,
    val serverTime: Long
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val itemsSynced: Int = 0
)

data class SyncPushRequest(
    val clientId: String,
    val payload: SyncPayload
)

data class RemotePullResponse(
    val since: Long = 0,
    val version: Long = 0,
    val upserts: SyncPayload? = null,
    val deletes: Map<String, List<String>>? = null
)

data class ServerStatusResponse(
    val ok: Boolean = false,
    val service: String? = null,
    val version: Long = 0,
    val records: Int = 0,
    val tables: Map<String, Int>? = null
)

data class SyncPushResponse(
    val ok: Boolean = false,
    val version: Long = 0,
    val changes: Int = 0
)

data class RemoteSyncOutcome(
    val success: Boolean,
    val message: String,
    val sent: Int = 0,
    val received: Int = 0,
    val removed: Int = 0,
    val serverVersion: Long = -1
)
