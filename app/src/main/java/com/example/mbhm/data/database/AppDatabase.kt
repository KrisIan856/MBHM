package com.example.mbhm.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        LeaveNoticeEntity::class,
        AbsenceAlertEntity::class,
        RoomEntity::class,
        JobEntity::class,
        GuardianProfileEntity::class
    ],
    version = 8,
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
    abstract fun absenceAlertDao(): AbsenceAlertDao
    abstract fun roomDao(): RoomDao
    abstract fun jobDao(): JobDao
    abstract fun guardianProfileDao(): GuardianProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN displayName TEXT")
                db.execSQL("ALTER TABLE leave_notices ADD COLUMN verifiedBy TEXT")
                db.execSQL("ALTER TABLE leave_notices ADD COLUMN verifiedAt TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leave_notices ADD COLUMN departedAt TEXT")
                db.execSQL("ALTER TABLE leave_notices ADD COLUMN arrivedAt TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS absence_alerts (
                        id TEXT NOT NULL PRIMARY KEY,
                        boarderId TEXT NOT NULL,
                        absenceCountAtSend INTEGER NOT NULL,
                        thresholdAtSend INTEGER NOT NULL,
                        sentOn TEXT NOT NULL,
                        sentAt TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_absence_alerts_boarder ON absence_alerts(boarderId)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rooms (
                        name TEXT NOT NULL PRIMARY KEY,
                        floor TEXT NOT NULL,
                        maxCapacity INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS jobs (
                        name TEXT NOT NULL PRIMARY KEY
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guardian_profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        houseName TEXT NOT NULL,
                        houseAddress TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS boarders_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        room TEXT NOT NULL,
                        floor TEXT NOT NULL,
                        initials TEXT NOT NULL,
                        avatarColor TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        joinDate TEXT NOT NULL,
                        monthlyRate INTEGER NOT NULL,
                        dueDay INTEGER NOT NULL,
                        gracePeriodDays INTEGER NOT NULL,
                        paymentStatus TEXT NOT NULL,
                        latePenaltyType TEXT NOT NULL,
                        latePenaltyAmount INTEGER NOT NULL,
                        emergencyContactName TEXT NOT NULL,
                        emergencyContactPhone TEXT NOT NULL,
                        emergencyContactSecondaryPhone TEXT,
                        emergencyContactAddress TEXT,
                        guardianContactName TEXT NOT NULL,
                        guardianContactPhone TEXT NOT NULL,
                        guardianContactAltPhone TEXT,
                        guardianContactAddress TEXT,
                        job TEXT NOT NULL,
                        idNumber TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO boarders_new (
                        id, name, room, floor, initials, avatarColor, phone, email, joinDate,
                        monthlyRate, dueDay, gracePeriodDays, paymentStatus, latePenaltyType,
                        latePenaltyAmount, emergencyContactName, emergencyContactPhone,
                        emergencyContactSecondaryPhone, emergencyContactAddress,
                        guardianContactName, guardianContactPhone, guardianContactAltPhone,
                        guardianContactAddress, job, idNumber
                    )
                    SELECT
                        b.id, b.name, b.room, b.floor, b.initials, b.avatarColor, b.phone,
                        b.email, b.joinDate, b.monthlyRate, b.dueDay, b.gracePeriodDays,
                        b.paymentStatus, b.latePenaltyType, b.latePenaltyAmount,
                        b.emergencyContactName, b.emergencyContactPhone,
                        b.emergencyContactSecondaryPhone, b.emergencyContactAddress,
                        b.guardianContactName, b.guardianContactPhone,
                        b.guardianContactAltPhone, b.guardianContactAddress, b.job,
                        COALESCE(u.username, '')
                    FROM boarders b
                    LEFT JOIN users u ON u.boarderId = b.id
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE boarders")
                db.execSQL("ALTER TABLE boarders_new RENAME TO boarders")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mbhm_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}