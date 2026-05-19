package com.example.nammapustaka

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.prefs.UserPreferencesRepository
import com.example.nammapustaka.work.OverdueNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NammaPustakaApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val libraryRepository by lazy { LibraryRepository(this) }
    val userPreferences by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        OverdueNotificationScheduler.schedule(this)
        applicationScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.seedIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("NammaPustakaApplication", "Error seeding database", e)
            }
            try {
                libraryRepository.syncAllLocalStudentsToFirestore()
            } catch (e: Exception) {
                android.util.Log.e("NammaPustakaApplication", "Error syncing students", e)
            }
            try {
                libraryRepository.startFirestoreStudentSync(applicationScope)
            } catch (e: Exception) {
                android.util.Log.e("NammaPustakaApplication", "Error starting student sync", e)
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID_OVERDUE,
            getString(R.string.channel_overdue_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.channel_overdue_description)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_OVERDUE = "overdue_books"
    }
}
