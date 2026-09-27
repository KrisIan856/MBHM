package com.example.mbhm.data.repository

import android.content.Context
import com.example.mbhm.data.SampleData
import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.*
import com.example.mbhm.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppRepository private constructor(context: Context) {
    private val db = AppDatabase.getInstance(context)
    val boarders = BoarderRepository(db)
    val payments = PaymentRepository(db)
    val attendance = AttendanceRepository(db)

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                AppRepository(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return db.userDao().getByUsername(username.trim())
    }

    suspend fun authenticateUser(username: String, password: String): UserEntity? {
        val user = db.userDao().getByUsername(username.trim()) ?: return null
        if (user.passwordHash == password) {
            return user
        }
        return null
    }

    suspend fun registerUser(username: String, password: String, role: Role): UserEntity? {
        val trimmed = username.trim()
        if (db.userDao().getByUsername(trimmed) != null) {
            return null // Username already exists
        }
        val newUser = UserEntity(
            username = trimmed,
            passwordHash = password,
            role = role,
            boarderId = if (role == Role.BOARDER) "4" else null
        )
        db.userDao().insert(newUser)
        return newUser
    }

    fun initializeSampleData() = CoroutineScope(Dispatchers.IO).launch {
        // Ensure default users exist in Room DB
        if (db.userDao().getByUsername("guardian") == null) {
            val defaultUsers = listOf(
                UserEntity("guardian", "password", Role.GUARDIAN, null),
                UserEntity("boarder", "password", Role.BOARDER, "4"),
                UserEntity("maria", "password", Role.BOARDER, "1"),
                UserEntity("carlo", "password", Role.BOARDER, "2"),
                UserEntity("diego", "password", Role.BOARDER, "4")
            )
            db.userDao().insertAll(defaultUsers)
        }

        if (db.boarderDao().getById("1") != null) return@launch

        // Seed boarders and records from SampleData into Room Database
        db.boarderDao().insertAll(SampleData.boarders.map { BoarderEntity.fromDomain(it) })
        db.paymentRecordDao().insertAll(SampleData.payments.map { PaymentRecordEntity.fromDomain(it) })
        db.attendanceSessionDao().insertAll(SampleData.sessions.map { AttendanceSessionEntity.fromDomain(it) })
        db.attendanceRecordDao().insertAll(SampleData.attendanceRecords.map { AttendanceRecordEntity.fromDomain(it) })
        db.announcementDao().insertAll(SampleData.announcements.map { AnnouncementEntity.fromDomain(it) })
        db.visitorEntryDao().insertAll(SampleData.visitorEntries.map { VisitorEntryEntity.fromDomain(it) })
        db.incidentDao().insertAll(SampleData.incidents.map { IncidentEntity.fromDomain(it) })
        db.maintenanceReportDao().insertAll(SampleData.maintenanceReports.map { MaintenanceReportEntity.fromDomain(it) })
        db.curfewRecordDao().insertAll(SampleData.curfewRecords.map { CurfewRecordEntity.fromDomain(it) })
        db.worshipScheduleDao().insert(WorshipScheduleEntity.fromDomain(SampleData.worshipSchedule))
        db.billingSettingsDao().insert(BillingSettingsEntity.fromDomain(SampleData.defaultBilling))
    }
}
