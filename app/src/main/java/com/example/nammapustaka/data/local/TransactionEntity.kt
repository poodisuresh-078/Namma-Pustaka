package com.example.nammapustaka.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
        Index("bookId"),
        Index("studentId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val txnId: String,
    val bookId: String,
    val studentId: String,
    val issueDate: Long,
    val dueDate: Long,
    val returnDate: Long?,
    val status: String,
)
