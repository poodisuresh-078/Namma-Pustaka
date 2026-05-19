package com.example.nammapustaka.data.remote

import android.util.Log
import com.example.nammapustaka.data.local.StudentDao
import com.example.nammapustaka.data.local.StudentEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import java.util.UUID

object FirestoreService {
    private const val TAG = "FirestoreService"
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Logs real-time application events (login, logout, transactions) to the Firebase Firestore "activities" collection.
     */
    fun logActivity(
        userId: String,
        username: String,
        displayName: String,
        role: String,
        action: String,
        details: Map<String, Any> = emptyMap()
    ) {
        val activity = hashMapOf(
            "activityId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "username" to username,
            "displayName" to displayName,
            "role" to role,
            "action" to action,
            "timestamp" to System.currentTimeMillis(),
            "details" to details
        )
        db.collection("activities")
            .add(activity)
            .addOnSuccessListener {
                Log.d(TAG, "Activity logged successfully: $action")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error logging activity", e)
            }
    }

    /**
     * Uploads a student record to the Firebase Firestore "students" collection.
     */
    fun uploadStudent(student: StudentEntity) {
        val studentData = hashMapOf(
            "studentId" to student.studentId,
            "name" to student.name,
            "classSection" to student.classSection,
            "photoPath" to student.photoPath
        )
        db.collection("students")
            .document(student.studentId)
            .set(studentData)
            .addOnSuccessListener {
                Log.d(TAG, "Student uploaded to Firestore: ${student.name}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading student", e)
            }
    }

    /**
     * Listens for changes on the Firebase Firestore "students" collection in real-time,
     * syncing updates straight into the local SQLite Room database.
     */
    fun startRealtimeSync(studentDao: StudentDao, scope: CoroutineScope) {
        db.collection("students")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            val doc = dc.document
                            val studentId = doc.getString("studentId") ?: continue
                            val name = doc.getString("name") ?: continue
                            val classSection = doc.getString("classSection") ?: ""
                            val photoPath = doc.getString("photoPath")

                            val student = StudentEntity(
                                studentId = studentId,
                                name = name,
                                classSection = classSection,
                                photoPath = photoPath
                            )

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    studentDao.insert(student)
                                    Log.d(TAG, "Realtime sync: Synced student ${student.name}")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    // Keep locally if needed, or remove if required:
                                    // studentDao.delete(student)
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * Submits a book request from a student.
     */
    fun submitBookRequest(
        studentId: String,
        studentName: String,
        bookTitle: String,
        author: String,
        notes: String
    ) {
        val reqId = UUID.randomUUID().toString()
        val requestData = hashMapOf(
            "requestId" to reqId,
            "studentId" to studentId,
            "studentName" to studentName,
            "bookTitle" to bookTitle,
            "author" to author,
            "requestDate" to System.currentTimeMillis(),
            "status" to "PENDING",
            "notes" to notes
        )
        db.collection("book_requests")
            .document(reqId)
            .set(requestData)
            .addOnSuccessListener {
                Log.d(TAG, "Book request submitted: $bookTitle")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error submitting book request", e)
            }
    }

    /**
     * Updates the status of a book request (e.g. APPROVED, REJECTED).
     */
    fun updateBookRequestStatus(requestId: String, status: String) {
        db.collection("book_requests")
            .document(requestId)
            .update("status", status)
            .addOnSuccessListener {
                Log.d(TAG, "Updated request status for $requestId to $status")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating request status", e)
            }
    }

    /**
     * Observes all book requests in real-time.
     */
    fun observeBookRequests(): kotlinx.coroutines.flow.Flow<List<BookRequest>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = db.collection("book_requests")
            .orderBy("requestDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen to requests failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            BookRequest(
                                requestId = doc.getString("requestId") ?: "",
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                bookTitle = doc.getString("bookTitle") ?: "",
                                author = doc.getString("author") ?: "",
                                requestDate = doc.getLong("requestDate") ?: 0L,
                                status = doc.getString("status") ?: "PENDING",
                                notes = doc.getString("notes") ?: ""
                            )
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error parsing BookRequest doc", ex)
                            null
                        }
                    }
                    trySend(requests)
                }
            }
        awaitClose { listener.remove() }
    }
}
