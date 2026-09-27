package com.example.mbhm.data.repository

import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.AttendanceRecordEntity
import com.example.mbhm.data.entity.AttendanceSessionEntity
import com.example.mbhm.model.AttendanceRecord
import com.example.mbhm.model.AttendanceSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttendanceRepository(private val db: AppDatabase) {
    // Sessions
    fun getAllSessions(): Flow<List<AttendanceSession>> =
        db.attendanceSessionDao().getAll().map { it.map { it.toDomain() } }

    suspend fun getSessionById(id: String): AttendanceSession? =
        db.attendanceSessionDao().getById(id)?.toDomain()

    suspend fun getSessionByDateAndType(date: String, type: String): AttendanceSession? =
        db.attendanceSessionDao().getByDateAndType(date, type)?.toDomain()

    fun getSessionsByDateRange(startDate: String, endDate: String): Flow<List<AttendanceSession>> =
        db.attendanceSessionDao().getByDateRange(startDate, endDate).map { it.map { it.toDomain() } }

    suspend fun insertSession(session: AttendanceSession) =
        db.attendanceSessionDao().insert(AttendanceSessionEntity.fromDomain(session))

    suspend fun insertAllSessions(sessions: List<AttendanceSession>) =
        db.attendanceSessionDao().insertAll(sessions.map { AttendanceSessionEntity.fromDomain(it) })

    // Records
    fun getRecordsBySession(sessionId: String): Flow<List<AttendanceRecord>> =
        db.attendanceRecordDao().getBySession(sessionId).map { it.map { it.toDomain() } }

    fun getRecordsByBoarder(boarderId: String): Flow<List<AttendanceRecord>> =
        db.attendanceRecordDao().getByBoarder(boarderId).map { it.map { it.toDomain() } }

    suspend fun getRecord(boarderId: String, sessionId: String): AttendanceRecord? =
        db.attendanceRecordDao().getByBoarderAndSession(boarderId, sessionId)?.toDomain()

    suspend fun insertRecord(record: AttendanceRecord) =
        db.attendanceRecordDao().insert(AttendanceRecordEntity.fromDomain(record))

    suspend fun insertAllRecords(records: List<AttendanceRecord>) =
        db.attendanceRecordDao().insertAll(records.map { AttendanceRecordEntity.fromDomain(it) })

    suspend fun updateRecord(record: AttendanceRecord) =
        db.attendanceRecordDao().update(AttendanceRecordEntity.fromDomain(record))
}