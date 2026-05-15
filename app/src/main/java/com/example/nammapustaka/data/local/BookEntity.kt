package com.example.nammapustaka.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val author: String,
    val category: String,
    val pageCount: Int,
    val summaryKn: String,
    val coverImagePath: String?,
    val qrCodePayload: String,
    val addedDate: Long,
    val publisher: String,
    val isbn: String?,
)
