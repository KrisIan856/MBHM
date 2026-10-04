package com.example.mbhm.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mbhm.data.converter.Converters
import com.example.mbhm.data.dao.*
import com.example.mbhm.data.entity.*

@Database(
    entities = [
        UserEntity::class,
        BoarderEntity::class,
        PaymentRecordEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class,
        AnnouncementEntity::class,
        VisitorEntryEntity::class,
        IncidentEntity::class,
        MaintenanceReportEntity::class,
        CurfewRecordEntity::class,
        WorshipScheduleEntity::class,
        BillingSettingsEntity::class,
        LeaveNoticeEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun boarderDao(): BoarderDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun attendanceSessionDao(): AttendanceSessionDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun visitorEntryDao(): VisitorEntryDao
    abstract fun incidentDao(): IncidentDao
    abstract fun maintenanceReportDao(): MaintenanceReportDao
    abstract fun curfewRecordDao(): CurfewRecordDao
    abstract fun worshipScheduleDao(): WorshipScheduleDao
    abstract fun billingSettingsDao(): BillingSettingsDao
    abstract fun leaveNoticeDao(): LeaveNoticeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mbhm_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}