package com.example.nammapustaka.data.prefs

/**
 * A lightweight profile snapshot saved on the device for quick login access.
 * No passwords are stored — tapping a profile only auto-fills the username.
 *
 * Serialised as a single pipe-delimited record inside a DataStore string:
 *   userId|username|displayName|role
 * Multiple records are separated by a semicolon.
 */
data class SavedProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val role: String, // "STUDENT" | "TEACHER" | "LIBRARIAN"
) {
    /** Initials shown inside the avatar circle (up to 2 chars). */
    val initials: String
        get() = displayName
            .trim()
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
            .ifEmpty { username.take(1).uppercase() }

    fun encode(): String = "$userId|$username|$displayName|$role"

    companion object {
        fun decode(raw: String): SavedProfile? {
            val parts = raw.split("|")
            if (parts.size < 4) return null
            return SavedProfile(
                userId = parts[0],
                username = parts[1],
                displayName = parts[2],
                role = parts[3],
            )
        }
    }
}
