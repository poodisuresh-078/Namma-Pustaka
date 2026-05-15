package com.example.nammapustaka.data.local

enum class UserRole {
    STUDENT,
    TEACHER,
    LIBRARIAN,
    ;

    fun isStaff(): Boolean = this == TEACHER || this == LIBRARIAN
}
