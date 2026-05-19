package com.example.nammapustaka.data.remote

data class BookRequest(
    val requestId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val bookTitle: String = "",
    val author: String = "",
    val requestDate: Long = 0,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val notes: String = ""
)
