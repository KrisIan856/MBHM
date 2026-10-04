package com.example.mbhm.data.dao

import androidx.room.*
import com.example.mbhm.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoarderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(boarders: List<BoarderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(boarder: BoarderEntity)

    @Update
    suspend fun update(boarder: BoarderEntity)

    @Delete
    suspend fun delete(boarder: BoarderEntity)

    @Query("DELETE FROM boarders")
    suspend fun deleteAll()

    @Query("SELECT * FROM boarders ORDER BY name")
    fun getAll(): Flow<List<BoarderEntity>>

    @Query("SELECT * FROM boarders ORDER BY name")
    suspend fun getAllOnce(): List<BoarderEntity>

    @Query("SELECT * FROM boarders WHERE id = :id")
    suspend fun getById(id: String): BoarderEntity?

    @Query("SELECT * FROM boarders WHERE room = :room ORDER BY name")
    fun getByRoom(room: String): Flow<List<BoarderEntity>>

    @Query("SELECT * FROM boarders WHERE paymentStatus = :status")
    fun getByPaymentStatus(status: String): Flow<List<BoarderEntity>>

    @Query("SELECT * FROM boarders WHERE paymentStatus IN (:statuses)")
    fun getByPaymentStatuses(statuses: List<String>): Flow<List<BoarderEntity>>
}

@Dao
interface PaymentRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PaymentRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PaymentRecordEntity)

    @Update
    suspend fun update(record: PaymentRecordEntity)

    @Delete
    suspend fun delete(record: PaymentRecordEntity)

    @Query("DELETE FROM payment_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM payment_records ORDER BY period DESC")
    fun getAll(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records ORDER BY period DESC")
    suspend fun getAllOnce(): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records WHERE boarderId = :boarderId ORDER BY period DESC")
    fun getByBoarder(boarderId: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE boarderId = :boarderId AND period = :period")
    suspend fun getByBoarderAndPeriod(boarderId: String, period: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE status = :status")
    fun getByStatus(status: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE receiptStatus = :status")
    fun getByReceiptStatus(status: String): Flow<List<PaymentRecordEntity>>
}

@Dao
interface AttendanceSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<AttendanceSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AttendanceSessionEntity)

    @Update
    suspend fun update(session: AttendanceSessionEntity)

    @Delete
    suspend fun delete(session: AttendanceSessionEntity)

    @Query("DELETE FROM attendance_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC")
    fun getAll(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC")
    suspend fun getAllOnce(): List<AttendanceSessionEntity>

    @Query("SELECT * FROM attendance_sessions WHERE id = :id")
    suspend fun getById(id: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE date = :date AND type = :type")
    suspend fun getByDateAndType(date: String, type: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<AttendanceSessionEntity>>
}

@Dao
interface AttendanceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecordEntity)

    @Update
    suspend fun update(record: AttendanceRecordEntity)

    @Delete
    suspend fun delete(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getBySession(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE boarderId = :boarderId ORDER BY sessionId DESC")
    fun getByBoarder(boarderId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE boarderId = :boarderId AND sessionId = :sessionId")
    suspend fun getByBoarderAndSession(boarderId: String, sessionId: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records ORDER BY sessionId DESC")
    fun getAll(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY sessionId DESC")
    suspend fun getAllOnce(): List<AttendanceRecordEntity>
}

@Dao
interface AnnouncementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(announcements: List<AnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(announcement: AnnouncementEntity)

    @Update
    suspend fun update(announcement: AnnouncementEntity)

    @Delete
    suspend fun delete(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements")
    suspend fun deleteAll()

    @Query("SELECT * FROM announcements ORDER BY sentAt DESC")
    fun getAll(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY sentAt DESC")
    suspend fun getAllOnce(): List<AnnouncementEntity>

    @Query("SELECT * FROM announcements WHERE priority = :priority ORDER BY sentAt DESC")
    fun getByPriority(priority: String): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun getById(id: String): AnnouncementEntity?
}

@Dao
interface VisitorEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VisitorEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VisitorEntryEntity)

    @Update
    suspend fun update(entry: VisitorEntryEntity)

    @Delete
    suspend fun delete(entry: VisitorEntryEntity)

    @Query("DELETE FROM visitor_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM visitor_entries ORDER BY date DESC, timeIn DESC")
    fun getAll(): Flow<List<VisitorEntryEntity>>

    @Query("SELECT * FROM visitor_entries WHERE boarderId = :boarderId ORDER BY date DESC, timeIn DESC")
    fun getByBoarder(boarderId: String): Flow<List<VisitorEntryEntity>>

    @Query("SELECT * FROM visitor_entries WHERE date = :date ORDER BY timeIn DESC")
    fun getByDate(date: String): Flow<List<VisitorEntryEntity>>

    @Query("SELECT * FROM visitor_entries WHERE timeOut IS NULL")
    fun getActiveVisitors(): Flow<List<VisitorEntryEntity>>
}

@Dao
interface IncidentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<IncidentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: IncidentEntity)

    @Update
    suspend fun update(incident: IncidentEntity)

    @Delete
    suspend fun delete(incident: IncidentEntity)

    @Query("DELETE FROM incidents")
    suspend fun deleteAll()

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAll(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE boarderId = :boarderId ORDER BY timestamp DESC")
    fun getByBoarder(boarderId: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE resolved = 0 ORDER BY timestamp DESC")
    fun getUnresolved(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<IncidentEntity>>
}

@Dao
interface MaintenanceReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<MaintenanceReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: MaintenanceReportEntity)

    @Update
    suspend fun update(report: MaintenanceReportEntity)

    @Delete
    suspend fun delete(report: MaintenanceReportEntity)

    @Query("DELETE FROM maintenance_reports")
    suspend fun deleteAll()

    @Query("SELECT * FROM maintenance_reports ORDER BY submittedAt DESC")
    fun getAll(): Flow<List<MaintenanceReportEntity>>

    @Query("SELECT * FROM maintenance_reports ORDER BY submittedAt DESC")
    suspend fun getAllOnce(): List<MaintenanceReportEntity>

    @Query("SELECT * FROM maintenance_reports WHERE boarderId = :boarderId ORDER BY submittedAt DESC")
    fun getByBoarder(boarderId: String): Flow<List<MaintenanceReportEntity>>

    @Query("SELECT * FROM maintenance_reports WHERE status = :status ORDER BY submittedAt DESC")
    fun getByStatus(status: String): Flow<List<MaintenanceReportEntity>>

    @Query("SELECT * FROM maintenance_reports WHERE room = :room ORDER BY submittedAt DESC")
    fun getByRoom(room: String): Flow<List<MaintenanceReportEntity>>
}

@Dao
interface CurfewRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CurfewRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CurfewRecordEntity)

    @Update
    suspend fun update(record: CurfewRecordEntity)

    @Delete
    suspend fun delete(record: CurfewRecordEntity)

    @Query("DELETE FROM curfew_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM curfew_records WHERE boarderId = :boarderId ORDER BY date DESC")
    fun getByBoarder(boarderId: String): Flow<List<CurfewRecordEntity>>

    @Query("SELECT * FROM curfew_records WHERE date = :date ORDER BY boarderId")
    fun getByDate(date: String): Flow<List<CurfewRecordEntity>>

    @Query("SELECT * FROM curfew_records WHERE status = :status")
    fun getByStatus(status: String): Flow<List<CurfewRecordEntity>>

    @Query("SELECT * FROM curfew_records ORDER BY date DESC")
    fun getAll(): Flow<List<CurfewRecordEntity>>

    @Query("SELECT * FROM curfew_records ORDER BY date DESC")
    suspend fun getAllOnce(): List<CurfewRecordEntity>
}



@Dao
interface WorshipScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: WorshipScheduleEntity)

    @Update
    suspend fun update(schedule: WorshipScheduleEntity)

    @Query("SELECT * FROM worship_schedule WHERE id = 'default'")
    suspend fun get(): WorshipScheduleEntity?

    @Query("SELECT * FROM worship_schedule WHERE id = 'default'")
    fun observe(): Flow<WorshipScheduleEntity?>
}

@Dao
interface BillingSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: BillingSettingsEntity)

    @Update
    suspend fun update(settings: BillingSettingsEntity)

    @Query("SELECT * FROM billing_settings WHERE id = 'default'")
    suspend fun get(): BillingSettingsEntity?

    @Query("SELECT * FROM billing_settings WHERE id = 'default'")
    fun observe(): Flow<BillingSettingsEntity?>
}