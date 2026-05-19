package com.example.nammapustaka.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.data.local.AllBorrowRow
import com.example.nammapustaka.data.local.AppDatabase
import com.example.nammapustaka.data.local.AppUserEntity
import com.example.nammapustaka.data.local.BookCatalogRow
import com.example.nammapustaka.data.local.BookEntity
import com.example.nammapustaka.data.local.LeaderboardRow
import com.example.nammapustaka.data.local.ReviewEntity
import com.example.nammapustaka.data.local.StudentEntity
import com.example.nammapustaka.data.local.TransactionEntity
import com.example.nammapustaka.data.local.TxnStatus
import com.example.nammapustaka.util.ImageCompressor
import com.example.nammapustaka.util.QrBitmapGenerator
import com.example.nammapustaka.util.TimeUtil
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibraryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "namma_pustaka.db",
    )
        .fallbackToDestructiveMigration()
        .build()

    private val books = db.bookDao()
    private val students = db.studentDao()
    private val txns = db.transactionDao()
    private val reviews = db.reviewDao()
    private val appUsers = db.appUserDao()

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val currentStudents = students.getStudents()
        val hasIseSeeded = currentStudents.any { it.name == "ABHIJEET GIREESAN" }
        if (!hasIseSeeded) {
            students.deleteAll()
            
            // Delete old demo students s1-s15 from Firestore
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                (1..15).forEach { i ->
                    firestore.collection("students").document("s$i").delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryRepository", "Error deleting old students from Firestore", e)
            }
            
            val iseA = listOf(
                "ABHIJEET GIREESAN", "ABHISHEK P", "ABHISHEK S", "ACSHA SHARON H", "AKASH H",
                "AKASH KUMAR M V", "AMOGHA G", "ANKITHA G S", "ANKITHA N", "ANVITHA L",
                "APOORVA K J", "B LOHITH", "B SHEETHAL", "BALAJI S YALABURGI", "BALAJI SAMPATH K R",
                "BASAVA PRASAD", "BHAVANA PRAKASH NAI", "BRUNDA R", "BUSUPALLI HARITHA", "CHAITANYA T",
                "CHANDANA M V", "CHARITHA S P", "DARSHAN P", "DARSHAN RAMESH T", "DHANUSH K",
                "DHRUTHI V ARADHYA", "DHRUVA RAJ N", "DILIP Y S", "DIVYA SHREE L", "GAGAN H G",
                "HARIVARMA E", "HARSHINI S", "HARSHITHA P", "HEERA DEVI BARAL", "HEMAKIRANMAI H R",
                "HEMANTH KUMAR M", "HITHASHREE R", "HYDER UMAR KHAN", "J BINDU", "JAHNAVI S K",
                "JANAVI K", "KATHYAINI E", "KEERTHANA BHAT", "KEERTHANA C", "KEERTHANA M",
                "KRUTHIKA S R", "LAKSHMI R", "LAVANYA N", "LIKHITHA M", "M INDUSHREE",
                "MADHURI M", "MAHIMA M", "MANJUNATHA J", "MONIKA S M", "N M MOHITHA",
                "NAGARJUNA K", "NAGESH B A", "NAMITH K P", "NAMRATHA H R", "NANDITHA T S",
                "NAVEEN D S", "NAYANA B K", "NEERAJ S", "NIHARIKA B A", "NITHIN M P", "NITHIN P S"
            )

            val iseB = listOf(
                "NIVEDITHA K", "PARINITHA M.R", "PAVAN C", "PAVAN M", "PAVANA K", "PRAJWAL V N",
                "PRAKRUTHI V A", "PRATEEK UDAY NAYAK", "PRATEEK Y BHATT", "PRIYA GANAPATI MUNA",
                "PRIYADARSHINI K N", "PRIYASHREE L", "PUSHPA PALLAVI D E", "RAGHUNANDAN K N",
                "RAJATH NAREN M K G", "RAKSHITHA S", "RAZA ABBAS", "RISHIKESH RAJU", "S J DIVYASRI",
                "SACHIN GOWDA G", "SAHANA S R", "SAHIL", "SANJANA H P", "SANKET", "SHARATH R",
                "SHREEHARI GURURAJ KU", "SHREEJITH P N", "SHREYA S N", "SHREYAS S", "SHRIRAM HEGDE",
                "SNEHA N", "SNEHA N L", "SRINIVASULU A", "SRUJAN SWAMY H S", "SUJAY H S",
                "SUMANTH KUMAR SHET", "SUPREETH A", "T SAI NIKITHA", "TANGIRALA RUTHU", "TEJAS S",
                "TEJASWINI L", "THEJASWINI S", "TUSHAR S", "UJWAL RAJ P", "UTKARSH MAHAJAN",
                "VAIBHAV B K", "VAIBHAV N ACHARYA", "VAISHNAVI", "VAISHNAVI SRINIVASA M", "VASANTH KUMAR A",
                "VASANTH KUMAR B", "VASANTH KUMAR K O", "VEERESH M", "VENKATESHWARA RAJU", "VENU S",
                "VIDYASHREE G", "VINAYAK RATEESH NAYA", "VIVEK D K", "VIVEKANANDA S", "YUKTHA H N"
            )

            val newStudents = mutableListOf<StudentEntity>()
            var idx = 1
            iseA.forEach { name ->
                newStudents.add(StudentEntity("std-ise-$idx", name, "ISE A", null))
                idx++
            }
            iseB.forEach { name ->
                newStudents.add(StudentEntity("std-ise-$idx", name, "ISE B", null))
                idx++
            }
            students.insertAll(newStudents)
            
            // Sync new students to Firestore
            try {
                newStudents.forEach { s ->
                    com.example.nammapustaka.data.remote.FirestoreService.uploadStudent(s)
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryRepository", "Error uploading students to Firestore", e)
            }
        }
        val now = System.currentTimeMillis()
        val demoBooks = listOf(
            BookEntity(
                bookId = "b-demo-1",
                title = "Malgudi Days",
                author = "R. K. Narayan",
                category = "Story",
                pageCount = 220,
                summaryKn = "ಮಲ್ಗುಡಿಯ ಸಣ್ಣ ಪಟ್ಟಣದ ಜೀವಂತ ಕಥೆಗಳು; ವಿದ್ಯಾರ್ಥಿಗಳ ಓದುವ ಆಸಕ್ತಿ ಹೆಚ್ಚಿಸುತ್ತದೆ.",
                coverImagePath = null,
                qrCodePayload = "b-demo-1",
                addedDate = now,
                publisher = "Indian Thought",
                isbn = null,
            ),
            BookEntity(
                bookId = "b-demo-2",
                title = "A Brief History of Time",
                author = "Stephen Hawking",
                category = "Science",
                pageCount = 256,
                summaryKn = "ಬ್ರಹ್ಮಾಂಡ, ಕಾಲ ಮತ್ತು ಗುರುತ್ವದ ಬಗ್ಗೆ ಸರಳವಾಗಿ ವಿವರಿಸುವ ವಿಜ್ಞಾನ ಪುಸ್ತಕ.",
                coverImagePath = null,
                qrCodePayload = "b-demo-2",
                addedDate = now,
                publisher = "Bantam",
                isbn = null,
            ),
            BookEntity(
                bookId = "b-demo-3",
                title = "Pax Indica",
                author = "Shashi Tharoor",
                category = "History",
                pageCount = 448,
                summaryKn = "ಭಾರತದ ವಿದೇಶಾಂಗ ನೀತಿ ಮತ್ತು ಚರಿತ್ರೆಯ ಹಿನ್ನೆಲೆ — ಓದುಗರ ಚಿಂತನೆಗೆ ಹೊಸ ಕಿಟಕಿ.",
                coverImagePath = null,
                qrCodePayload = "b-demo-3",
                addedDate = now,
                publisher = "Penguin",
                isbn = null,
            ),
        )
        demoBooks.forEach { books.insert(it) }
    }

    fun observeCatalog(search: String, category: String?): Flow<List<BookCatalogRow>> =
        books.observeCatalog(search.trim(), category)

    fun observeBook(bookId: String) = books.observeBook(bookId)

    fun observeStudents() = students.observeStudents()

    suspend fun getBookByQr(payload: String) = books.getBookByQr(payload.trim())

    suspend fun getActiveTxn(bookId: String) = txns.getActiveForBook(bookId)

    fun observeActiveBorrows(): Flow<List<ActiveBorrowRow>> = txns.observeActiveBorrows()

    fun observeReturnedBorrows(): Flow<List<AllBorrowRow>> = txns.observeReturnedBorrows()

    fun observeAllBorrows(): Flow<List<AllBorrowRow>> = txns.observeAllBorrows()

    fun observeBorrowsForStudent(studentId: String): Flow<List<AllBorrowRow>> = txns.observeBorrowsForStudent(studentId)

    fun observeActiveBorrowForBook(bookId: String): Flow<ActiveBorrowRow?> =
        txns.observeActiveBorrows().map { rows -> rows.firstOrNull { it.txn.bookId == bookId } }

    fun observeOverdueRows(): Flow<List<ActiveBorrowRow>> =
        txns.observeActiveBorrows().map { rows ->
            val now = System.currentTimeMillis()
            rows.filter { it.txn.dueDate < now }
        }

    fun observeLeaderboardCurrentMonth(): Flow<List<LeaderboardRow>> {
        val (start, end) = TimeUtil.currentMonthRangeMillis()
        return txns.observeLeaderboard(start, end)
    }

    fun observeLeaderboardForPastMonth(monthsAgo: Int): Flow<List<LeaderboardRow>> {
        val r = TimeUtil.monthRangeMillis(monthsAgo)
        return txns.observeLeaderboard(r.start, r.endExclusive)
    }

    suspend fun countOverdue(): Int =
        txns.countOverdue(System.currentTimeMillis())

    suspend fun syncOverdueStatuses() {
        txns.markIssuedPastDueAsOverdue(System.currentTimeMillis())
    }

    suspend fun issueBook(bookId: String, studentId: String) {
        val active = txns.getActiveForBook(bookId)
        require(active == null) { "Book already issued" }
        val now = System.currentTimeMillis()
        val due = TimeUtil.defaultDueMillis(now, 14)
        val txn = TransactionEntity(
            txnId = UUID.randomUUID().toString(),
            bookId = bookId,
            studentId = studentId,
            issueDate = now,
            dueDate = due,
            returnDate = null,
            status = TxnStatus.ISSUED,
        )
        txns.insert(txn)

        try {
            val book = books.getBook(bookId)
            val student = students.getStudent(studentId)
            com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                userId = studentId,
                username = student?.name ?: studentId,
                displayName = student?.name ?: studentId,
                role = "STUDENT",
                action = "BORROW_BOOK",
                details = mapOf(
                    "bookId" to bookId,
                    "bookTitle" to (book?.title ?: ""),
                    "studentId" to studentId,
                    "studentName" to (student?.name ?: "")
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepository", "Firestore log failed", e)
        }
    }

    suspend fun returnBookForActive(bookId: String) {
        val active = txns.getActiveForBook(bookId) ?: return
        val now = System.currentTimeMillis()
        txns.update(
            active.copy(
                returnDate = now,
                status = TxnStatus.RETURNED,
            ),
        )

        try {
            val book = books.getBook(bookId)
            val student = students.getStudent(active.studentId)
            com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                userId = active.studentId,
                username = student?.name ?: active.studentId,
                displayName = student?.name ?: active.studentId,
                role = "STUDENT",
                action = "RETURN_BOOK",
                details = mapOf(
                    "bookId" to bookId,
                    "bookTitle" to (book?.title ?: ""),
                    "studentId" to active.studentId,
                    "studentName" to (student?.name ?: "")
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepository", "Firestore log failed", e)
        }
    }

    suspend fun extendDueDate(txnId: String, newDueMillis: Long) {
        val t = txns.getById(txnId) ?: return
        if (t.status == TxnStatus.RETURNED) return
        txns.update(t.copy(dueDate = newDueMillis, status = TxnStatus.ISSUED))

        try {
            val book = books.getBook(t.bookId)
            val student = students.getStudent(t.studentId)
            com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                userId = t.studentId,
                username = student?.name ?: t.studentId,
                displayName = student?.name ?: t.studentId,
                role = "STUDENT",
                action = "EXTEND_DUE_DATE",
                details = mapOf(
                    "bookId" to t.bookId,
                    "bookTitle" to (book?.title ?: ""),
                    "studentId" to t.studentId,
                    "studentName" to (student?.name ?: ""),
                    "newDueDate" to newDueMillis
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepository", "Firestore log failed", e)
        }
    }

    suspend fun addManualBook(
        title: String,
        author: String,
        category: String,
        pageCount: Int,
        summaryKn: String,
        publisher: String,
        coverBitmap: Bitmap?,
    ): BookEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val coversDir = File(appContext.filesDir, "covers").apply { mkdirs() }
        val coverPath = if (coverBitmap != null) {
            val f = File(coversDir, "$id.jpg")
            ImageCompressor.saveJpegUnderMaxBytes(coverBitmap, f)
            f.absolutePath
        } else {
            null
        }
        val entity = BookEntity(
            bookId = id,
            title = title.trim(),
            author = author.trim(),
            category = category,
            pageCount = pageCount,
            summaryKn = summaryKn.trim(),
            coverImagePath = coverPath,
            qrCodePayload = id,
            addedDate = System.currentTimeMillis(),
            publisher = publisher.trim().ifEmpty { "—" },
            isbn = null,
        )
        books.insert(entity)
        entity
    }

    suspend fun saveIsbnLookupStub(isbn: String): String = isbn.trim()

    fun observeReviews(bookId: String) = reviews.observeForBook(bookId)

    suspend fun canReview(studentId: String, bookId: String): Boolean =
        txns.hasReturned(studentId, bookId)

    suspend fun upsertReview(studentId: String, bookId: String, rating: Int, text: String) {
        require(canReview(studentId, bookId)) { "Borrow and return required before review" }
        val existing = reviews.getByStudentBook(studentId, bookId)
        val now = System.currentTimeMillis()
        if (existing == null) {
            reviews.insert(
                ReviewEntity(
                    reviewId = UUID.randomUUID().toString(),
                    bookId = bookId,
                    studentId = studentId,
                    rating = rating.coerceIn(1, 5),
                    reviewText = text.take(120),
                    timestamp = now,
                ),
            )
        } else {
            reviews.update(
                existing.copy(
                    rating = rating.coerceIn(1, 5),
                    reviewText = text.take(120),
                    timestamp = now,
                ),
            )
        }
    }

    fun buildQrBitmapForBook(bookId: String) = QrBitmapGenerator.encode(bookId)

    suspend fun getStudent(id: String) = students.getStudent(id)

    // ── Authentication ──────────────────────────────────────────────

    suspend fun getAppUserById(userId: String): AppUserEntity? =
        withContext(Dispatchers.IO) { appUsers.getById(userId) }

    suspend fun login(username: String, password: String): AppUserEntity? =
        withContext(Dispatchers.IO) {
            val user = appUsers.getByUsername(username) ?: return@withContext null
            if (user.passwordHash == hashPassword(password)) user else null
        }

    suspend fun usernameExists(username: String): Boolean =
        withContext(Dispatchers.IO) { appUsers.usernameExists(username) }

    suspend fun searchStudentsByName(query: String): List<StudentEntity> =
        withContext(Dispatchers.IO) { students.searchStudents(query) }

    suspend fun registerStudent(
        displayName: String,
        classSection: String,
        username: String,
        password: String,
        linkedStudentId: String?,
        securityAnswer: String,
    ): Result<AppUserEntity> = withContext(Dispatchers.IO) {
        try {
            if (appUsers.usernameExists(username)) {
                return@withContext Result.failure(Exception("Username already taken"))
            }
            val finalStudentId = linkedStudentId ?: run {
                val newId = UUID.randomUUID().toString()
                val newStudent = StudentEntity(
                    studentId = newId,
                    name = displayName.trim(),
                    classSection = classSection.trim().ifEmpty { "—" },
                    photoPath = null,
                )
                students.insert(newStudent)
                com.example.nammapustaka.data.remote.FirestoreService.uploadStudent(newStudent)
                newId
            }
            val user = AppUserEntity(
                userId = UUID.randomUUID().toString(),
                username = username.trim(),
                passwordHash = hashPassword(password),
                role = "STUDENT",
                linkedStudentId = finalStudentId,
                displayName = displayName.trim(),
                securityAnswer = securityAnswer.trim().lowercase(),
            )
            appUsers.insert(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerStaff(
        username: String,
        password: String,
        role: String,
        signUpCode: String,
        securityAnswer: String,
    ): Result<AppUserEntity> = withContext(Dispatchers.IO) {
        try {
            val derivedRole = when (signUpCode.trim()) {
                TEACHER_SIGN_UP_CODE -> "TEACHER"
                LIBRARIAN_SIGN_UP_CODE -> "LIBRARIAN"
                else -> return@withContext Result.failure(Exception("Invalid sign up code"))
            }

            if (appUsers.usernameExists(username)) {
                return@withContext Result.failure(Exception("Username already taken"))
            }
            val user = AppUserEntity(
                userId = UUID.randomUUID().toString(),
                username = username.trim(),
                passwordHash = hashPassword(password),
                role = derivedRole,
                linkedStudentId = null,
                displayName = username.trim(),
                securityAnswer = securityAnswer.trim().lowercase(),
            )
            appUsers.insert(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsersByDisplayName(query: String): List<AppUserEntity> =
        withContext(Dispatchers.IO) { appUsers.searchUsersByDisplayName(query) }

    suspend fun resetPassword(username: String, newPasswordStr: String): Boolean =
        withContext(Dispatchers.IO) {
            val user = appUsers.getByUsername(username) ?: return@withContext false
            appUsers.update(user.copy(passwordHash = hashPassword(newPasswordStr)))
            true
        }

    suspend fun syncAllLocalStudentsToFirestore() = withContext(Dispatchers.IO) {
        try {
            val localStudents = students.getStudents()
            localStudents.forEach { student ->
                com.example.nammapustaka.data.remote.FirestoreService.uploadStudent(student)
            }
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepository", "Error syncing local students to Firestore", e)
        }
    }

    fun startFirestoreStudentSync(scope: kotlinx.coroutines.CoroutineScope) {
        com.example.nammapustaka.data.remote.FirestoreService.startRealtimeSync(students, scope)
    }

    fun submitBookRequest(studentId: String, studentName: String, bookTitle: String, author: String, notes: String) {
        com.example.nammapustaka.data.remote.FirestoreService.submitBookRequest(studentId, studentName, bookTitle, author, notes)
    }

    fun updateBookRequestStatus(requestId: String, status: String) {
        com.example.nammapustaka.data.remote.FirestoreService.updateBookRequestStatus(requestId, status)
    }

    fun observeBookRequests(): kotlinx.coroutines.flow.Flow<List<com.example.nammapustaka.data.remote.BookRequest>> {
        return com.example.nammapustaka.data.remote.FirestoreService.observeBookRequests()
    }

    suspend fun updateBook(
        bookId: String,
        title: String,
        author: String,
        publisher: String,
        pageCount: Int,
        summaryKn: String,
        category: String,
        coverBitmap: android.graphics.Bitmap? = null,
        isbn: String? = null
    ) = withContext(Dispatchers.IO) {
        val existing = books.getBook(bookId) ?: return@withContext
        val coversDir = File(appContext.filesDir, "covers").apply { mkdirs() }
        val coverPath = if (coverBitmap != null) {
            existing.coverImagePath?.let { File(it).delete() }
            val f = File(coversDir, "$bookId.jpg")
            ImageCompressor.saveJpegUnderMaxBytes(coverBitmap, f)
            f.absolutePath
        } else {
            existing.coverImagePath
        }
        val updated = existing.copy(
            title = title.trim(),
            author = author.trim(),
            publisher = publisher.trim(),
            pageCount = pageCount,
            summaryKn = summaryKn.trim(),
            category = category.trim(),
            coverImagePath = coverPath,
            isbn = isbn?.trim() ?: existing.isbn
        )
        books.update(updated)
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        books.deleteBookById(bookId)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TEACHER_SIGN_UP_CODE = "NPL-TEACH-2026"
        private const val LIBRARIAN_SIGN_UP_CODE = "NPL-LIB-2026"
    }
}
