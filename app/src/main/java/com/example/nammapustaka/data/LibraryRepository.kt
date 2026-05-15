package com.example.nammapustaka.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.data.local.AppDatabase
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

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (students.getStudents().isNotEmpty()) return@withContext
        val demoStudents = listOf(
            StudentEntity("s1", "Ananya", "7-A", null),
            StudentEntity("s2", "Ravi", "7-B", null),
            StudentEntity("s3", "Kavya", "8-A", null),
            StudentEntity("s4", "Manjunath", "8-B", null),
            StudentEntity("s5", "Divya", "9-A", null),
        )
        students.insertAll(demoStudents)
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
    }

    suspend fun extendDueDate(txnId: String, newDueMillis: Long) {
        val t = txns.getById(txnId) ?: return
        if (t.status == TxnStatus.RETURNED) return
        txns.update(t.copy(dueDate = newDueMillis, status = TxnStatus.ISSUED))
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
}
