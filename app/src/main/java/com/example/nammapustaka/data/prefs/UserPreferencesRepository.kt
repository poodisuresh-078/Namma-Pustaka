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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "namma_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val dataStore get() = context.dataStore

    val isTeacherMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TEACHER] ?: true
    }

    val currentStudentId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_STUDENT_ID]
    }

    suspend fun setTeacherMode(value: Boolean) {
        dataStore.edit { it[KEY_TEACHER] = value }
    }

    suspend fun setCurrentStudentId(id: String?) {
        dataStore.edit { e ->
            if (id == null) e.remove(KEY_STUDENT_ID) else e[KEY_STUDENT_ID] = id
        }
    }

    companion object {
        private val KEY_TEACHER = booleanPreferencesKey("is_teacher")
        private val KEY_STUDENT_ID = stringPreferencesKey("current_student_id")
    }
}
