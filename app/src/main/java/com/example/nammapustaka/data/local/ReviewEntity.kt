package com.example.nammapustaka.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["bookId"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId", "studentId"], unique = true),
        Index("bookId"),
        Index("studentId"),
    ],
)
data class ReviewEntity(
    @PrimaryKey val reviewId: String,
    val bookId: String,
    val studentId: String,
    val rating: Int,
    val reviewText: String,
    val timestamp: Long,
)
