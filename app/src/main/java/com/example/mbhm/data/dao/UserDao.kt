package com.example.mbhm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mbhm.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username)")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<UserEntity>>

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
