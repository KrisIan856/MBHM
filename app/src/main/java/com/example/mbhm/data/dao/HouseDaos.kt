package com.example.mbhm.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mbhm.data.entity.GuardianProfileEntity
import com.example.mbhm.data.entity.JobEntity
import com.example.mbhm.data.entity.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<RoomEntity>)

    @Delete
    suspend fun delete(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM rooms ORDER BY floor, name")
    fun getAll(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY floor, name")
    suspend fun getAllOnce(): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE name = :name")
    suspend fun getByName(name: String): RoomEntity?
}

@Dao
interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<JobEntity>)

    @Delete
    suspend fun delete(job: JobEntity)

    @Query("DELETE FROM jobs WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM jobs ORDER BY name")
    fun getAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs ORDER BY name")
    suspend fun getAllOnce(): List<JobEntity>
}

@Dao
interface GuardianProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: GuardianProfileEntity)

    @Query("SELECT * FROM guardian_profile WHERE id = 'default'")
    suspend fun get(): GuardianProfileEntity?

    @Query("SELECT * FROM guardian_profile WHERE id = 'default'")
    fun observe(): Flow<GuardianProfileEntity?>
}
