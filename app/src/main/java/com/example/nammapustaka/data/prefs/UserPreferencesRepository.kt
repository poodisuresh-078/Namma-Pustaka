package com.example.nammapustaka.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "namma_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val dataStore get() = context.dataStore

    val isTeacherMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TEACHER] ?: true
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_STUDENT_ID]
    }

    val loggedInUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN_USER_ID]
    }

    /** Emits the list of profiles saved for quick login, newest first. */
    val savedProfiles: Flow<List<SavedProfile>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_SAVED_PROFILES] ?: return@map emptyList()
        raw.split(";").mapNotNull { SavedProfile.decode(it) }
    }

    val loggedInRole: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN_ROLE]
    }

    val loggedInDisplayName: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LOGGED_IN_DISPLAY_NAME]
    }

    suspend fun setTeacherMode(value: Boolean) {
        dataStore.edit { it[KEY_TEACHER] = value }
    }

    suspend fun setCurrentStudentId(id: String?) {
        dataStore.edit { e ->
            if (id == null) e.remove(KEY_STUDENT_ID) else e[KEY_STUDENT_ID] = id
        }
    }

    suspend fun setLoggedIn(userId: String, role: String, displayName: String, studentId: String?) {
        dataStore.edit { e ->
            e[KEY_LOGGED_IN_USER_ID] = userId
            e[KEY_LOGGED_IN_ROLE] = role
            e[KEY_LOGGED_IN_DISPLAY_NAME] = displayName
            e[KEY_TEACHER] = role != "STUDENT"
            if (studentId != null) {
                e[KEY_STUDENT_ID] = studentId
            }
        }
    }

    suspend fun logout() {
        dataStore.edit { e ->
            e.remove(KEY_LOGGED_IN_USER_ID)
            e.remove(KEY_LOGGED_IN_ROLE)
            e.remove(KEY_LOGGED_IN_DISPLAY_NAME)
            e.remove(KEY_STUDENT_ID)
        }
    }

    /** Saves (or updates) a profile. The list is capped at 5 entries; oldest are evicted. */
    suspend fun saveProfile(profile: SavedProfile) {
        dataStore.edit { prefs ->
            val existing = (prefs[KEY_SAVED_PROFILES] ?: "")
                .split(";")
                .filter { it.isNotBlank() }
                .mapNotNull { SavedProfile.decode(it) }
                .filter { it.userId != profile.userId }   // remove any existing entry for this user
            val updated = (listOf(profile) + existing).take(5) // newest first, max 5
            prefs[KEY_SAVED_PROFILES] = updated.joinToString(";") { it.encode() }
        }
    }

    /** Removes a saved profile by its userId. */
    suspend fun removeProfile(userId: String) {
        dataStore.edit { prefs ->
            val updated = (prefs[KEY_SAVED_PROFILES] ?: "")
                .split(";")
                .filter { it.isNotBlank() }
                .mapNotNull { SavedProfile.decode(it) }
                .filter { it.userId != userId }
            if (updated.isEmpty()) prefs.remove(KEY_SAVED_PROFILES)
            else prefs[KEY_SAVED_PROFILES] = updated.joinToString(";") { it.encode() }
        }
    }

    companion object {
        private val KEY_TEACHER = booleanPreferencesKey("is_teacher")
        private val KEY_STUDENT_ID = stringPreferencesKey("current_student_id")
        private val KEY_LOGGED_IN_USER_ID = stringPreferencesKey("logged_in_user_id")
        private val KEY_LOGGED_IN_ROLE = stringPreferencesKey("logged_in_role")
        private val KEY_LOGGED_IN_DISPLAY_NAME = stringPreferencesKey("logged_in_display_name")
        private val KEY_SAVED_PROFILES = stringPreferencesKey("saved_profiles")
    }
}
