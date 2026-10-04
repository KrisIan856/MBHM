package com.example.mbhm.data.dao

import androidx.room.*
import com.example.mbhm.data.entity.LeaveNoticeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveNoticeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notice: LeaveNoticeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notices: List<LeaveNoticeEntity>)

    @Update
    suspend fun update(notice: LeaveNoticeEntity)

    @Delete
    suspend fun delete(notice: LeaveNoticeEntity)

    @Query("DELETE FROM leave_notices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM leave_notices")
    suspend fun deleteAll()

    @Query("SELECT * FROM leave_notices ORDER BY submittedAt DESC")
    fun getAll(): Flow<List<LeaveNoticeEntity>>

    @Query("SELECT * FROM leave_notices WHERE boarderId = :boarderId ORDER BY submittedAt DESC")
    fun getByBoarder(boarderId: String): Flow<List<LeaveNoticeEntity>>

    @Query("SELECT * FROM leave_notices WHERE id = :id")
    suspend fun getById(id: String): LeaveNoticeEntity?

    @Query("SELECT * FROM leave_notices WHERE status = :status ORDER BY submittedAt DESC")
    fun getByStatus(status: String): Flow<List<LeaveNoticeEntity>>
}
