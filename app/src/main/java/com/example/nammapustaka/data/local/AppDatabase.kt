package com.example.nammapustaka.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class BookCatalogRow(
    @Embedded val book: BookEntity,
    val avgRating: Float?,
    val reviewCount: Int,
)

data class ActiveBorrowRow(
    @Embedded val txn: TransactionEntity,
    val studentName: String,
    val bookTitle: String,
)

data class LeaderboardRow(
    val studentId: String,
    val name: String,
    val classSection: String,
    val booksRead: Int,
    val pagesRead: Int,
)

@Dao
interface BookDao {
    @Query(
        """
        SELECT b.*,
          (SELECT AVG(rating) FROM reviews r WHERE r.bookId = b.bookId) AS avgRating,
          (SELECT COUNT(*) FROM reviews r WHERE r.bookId = b.bookId) AS reviewCount
        FROM books b
        WHERE (:category IS NULL OR b.category = :category)
        AND (
          :search = ''
          OR b.title LIKE '%' || :search || '%' COLLATE NOCASE
          OR b.author LIKE '%' || :search || '%' COLLATE NOCASE
        )
        ORDER BY b.title COLLATE NOCASE ASC
        """,
    )
    fun observeCatalog(search: String, category: String?): Flow<List<BookCatalogRow>>

    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    fun observeBook(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    suspend fun getBook(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE qrCodePayload = :payload OR bookId = :payload LIMIT 1")
    suspend fun getBookByQr(payload: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Unit

    @Update
    suspend fun update(book: BookEntity): Unit
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name COLLATE NOCASE ASC")
    suspend fun getStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE studentId = :id LIMIT 1")
    suspend fun getStudent(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>): Unit
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE bookId = :bookId AND status IN ('ISSUED', 'OVERDUE')
        LIMIT 1
        """,
    )
    suspend fun getActiveForBook(bookId: String): TransactionEntity?

    @Query(
        """
        SELECT t.*, s.name AS studentName, b.title AS bookTitle
        FROM transactions t
        INNER JOIN students s ON s.studentId = t.studentId
        INNER JOIN books b ON b.bookId = t.bookId
        WHERE t.status IN ('ISSUED', 'OVERDUE')
        ORDER BY t.dueDate ASC
        """,
    )
    fun observeActiveBorrows(): Flow<List<ActiveBorrowRow>>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE status IN ('ISSUED', 'OVERDUE') AND dueDate < :nowMillis
        """,
    )
    suspend fun countOverdue(nowMillis: Long): Int

    @Query("SELECT * FROM transactions WHERE txnId = :txnId LIMIT 1")
    suspend fun getById(txnId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(txn: TransactionEntity): Unit

    @Update
    suspend fun update(txn: TransactionEntity): Unit

    @Query(
        """
        UPDATE transactions SET status = 'OVERDUE'
        WHERE status = 'ISSUED' AND dueDate < :nowMillis
        """,
    )
    suspend fun markIssuedPastDueAsOverdue(nowMillis: Long): Unit

    @Query(
        """
        SELECT s.studentId, s.name, s.classSection,
          COUNT(*) AS booksRead,
          IFNULL(SUM(b.pageCount), 0) AS pagesRead
        FROM transactions t
        INNER JOIN books b ON b.bookId = t.bookId
        INNER JOIN students s ON s.studentId = t.studentId
        WHERE t.status = 'RETURNED'
          AND t.returnDate IS NOT NULL
          AND t.returnDate >= :monthStart
          AND t.returnDate < :monthEnd
        GROUP BY s.studentId
        ORDER BY pagesRead DESC, booksRead DESC
        """,
    )
    fun observeLeaderboard(monthStart: Long, monthEnd: Long): Flow<List<LeaderboardRow>>

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM transactions
          WHERE studentId = :studentId AND bookId = :bookId AND status = 'RETURNED'
        )
        """,
    )
    suspend fun hasReturned(studentId: String, bookId: String): Boolean
}

@Dao
interface ReviewDao {
    @Query(
        """
        SELECT * FROM reviews WHERE bookId = :bookId
        ORDER BY timestamp DESC
        """,
    )
    fun observeForBook(bookId: String): Flow<List<ReviewEntity>>

    @Query(
        """
        SELECT * FROM reviews WHERE bookId = :bookId AND studentId = :studentId LIMIT 1
        """,
    )
    suspend fun getByStudentBook(studentId: String, bookId: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: ReviewEntity): Unit

    @Update
    suspend fun update(review: ReviewEntity): Unit
}

@Database(
    entities = [BookEntity::class, StudentEntity::class, TransactionEntity::class, ReviewEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun studentDao(): StudentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reviewDao(): ReviewDao
}
