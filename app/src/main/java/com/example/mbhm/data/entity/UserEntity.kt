package com.example.mbhm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mbhm.model.Role

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val role: Role,
    val boarderId: String? = null
)
