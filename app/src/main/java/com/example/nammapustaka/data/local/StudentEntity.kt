package com.example.nammapustaka.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val studentId: String,
    val name: String,
    val classSection: String,
    val photoPath: String?,
)
