package com.example.nammapustaka.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_users",
    indices = [Index(value = ["username"], unique = true)],
)
data class AppUserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val passwordHash: String,
    val role: String,
    val linkedStudentId: String?,
    val displayName: String,
    val securityAnswer: String? = null,
)
