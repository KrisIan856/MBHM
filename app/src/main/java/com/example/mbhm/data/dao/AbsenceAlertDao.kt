package com.example.mbhm.data.dao

import androidx.room.*
import com.example.mbhm.data.entity.AbsenceAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AbsenceAlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AbsenceAlertEntity>)

    @Delete
    suspend fun delete(alert: AbsenceAlertEntity)

    @Query("DELETE FROM absence_alerts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM absence_alerts")
    suspend fun deleteAll()

    @Query("SELECT * FROM absence_alerts ORDER BY sentAt DESC, id DESC")
    fun getAll(): Flow<List<AbsenceAlertEntity>>

    @Query("SELECT * FROM absence_alerts ORDER BY sentAt DESC, id DESC")
    suspend fun getAllOnce(): List<AbsenceAlertEntity>

    @Query("SELECT * FROM absence_alerts WHERE boarderId = :boarderId ORDER BY sentAt DESC, id DESC")
    suspend fun getByBoarder(boarderId: String): List<AbsenceAlertEntity>

    @Query("SELECT * FROM absence_alerts WHERE id = :id")
    suspend fun getById(id: String): AbsenceAlertEntity?
}