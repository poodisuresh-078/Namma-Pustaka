package com.example.nammapustaka.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.data.local.ReviewEntity
import com.example.nammapustaka.data.local.StudentEntity
import com.example.nammapustaka.ui.theme.AppShape
import com.example.nammapustaka.ui.theme.OverdueRed
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    repo: LibraryRepository,
    isTeacher: Boolean,
    currentStudentId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val book by repo.observeBook(bookId).collectAsStateWithLifecycle(initialValue = null)
    val active by repo.observeActiveBorrowForBook(bookId).collectAsStateWithLifecycle(initialValue = null)
    val reviews by repo.observeReviews(bookId).collectAsStateWithLifecycle(initialValue = emptyList())
    val students by repo.observeStudents().collectAsStateWithLifecycle(initialValue = emptyList())

    var showIssueSheet by remember { mutableStateOf(false) }
    var showReviewSheet by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }

    var canReview by remember { mutableStateOf(false) }
    LaunchedEffect(currentStudentId, bookId, active) {
        val sid = currentStudentId
        canReview = sid != null && repo.canReview(sid, bookId)
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    if (book == null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val b = book!!

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        b.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isTeacher) {
                        IconButton(
                            onClick = {
                                val bmp = repo.buildQrBitmapForBook(b.bookId)
                                val f = File(context.cacheDir, "qr_${b.bookId}.png")
                                FileOutputStream(f).use { os -> bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os) }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    f,
                                )
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(send, context.getString(R.string.share_qr)))
                            },
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_qr))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    shape = AppShape.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (b.coverImagePath != null) {
                                AsyncImage(
                                    model = File(b.coverImagePath),
                                    contentDescription = stringResource(R.string.content_desc_cover),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    b.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                )
                            }
                        }
                        Column(Modifier.padding(16.dp)) {
                            Text(b.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(b.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(
                                        stringResource(R.string.publisher),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(b.publisher, style = MaterialTheme.typography.bodyMedium)
                                }
                                Column {
                                    Text(
                                        stringResource(R.string.pages),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(b.pageCount.toString(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            AvailabilityBadge(active, dateFmt)
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.summary_kn),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = AppShape.field,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        b.summaryKn,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.reviews),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(reviews, key = { it.reviewId }) { r ->
                ReviewRowCard(r)
            }
            item {
                Spacer(Modifier.height(8.dp))
                if (!isTeacher && currentStudentId != null && canReview) {
                    Button(
                        onClick = { showReviewSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShape.field,
                    ) {
                        Text(stringResource(R.string.add_review))
                    }
                }
                if (isTeacher) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (active == null) {
                            Button(
                                onClick = { showIssueSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShape.field,
                            ) {
                                Text(stringResource(R.string.reserve))
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { repo.returnBookForActive(bookId) }
                                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "Error") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShape.field,
                            ) {
                                Text(stringResource(R.string.mark_returned))
                            }
                        }
                    }
                } else if (!isTeacher && active == null) {
                    Text(
                        "Ask your teacher to borrow this book.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showIssueSheet && isTeacher) {
        IssueBottomSheet(
            students = students,
            selectedStudentId = selectedStudentId,
            onSelectStudent = { selectedStudentId = it },
            onDismiss = { showIssueSheet = false },
            onConfirm = {
                val sid = selectedStudentId
                if (sid == null) {
                    scope.launch { snackbarHostState.showSnackbar("Pick a student") }
                } else {
                    scope.launch {
                        runCatching { repo.issueBook(bookId, sid) }
                            .onSuccess {
                                showIssueSheet = false
                                snackbarHostState.showSnackbar("Issued")
                            }
                            .onFailure { snackbarHostState.showSnackbar(it.message ?: "Issue failed") }
                    }
                }
            },
        )
    }

    if (showReviewSheet && currentStudentId != null) {
        ReviewBottomSheet(
            initialText = reviews.find { it.studentId == currentStudentId }?.reviewText.orEmpty(),
            initialRating = reviews.find { it.studentId == currentStudentId }?.rating ?: 5,
            onDismiss = { showReviewSheet = false },
            onSubmit = { rating, text ->
                scope.launch {
                    runCatching {
                        repo.upsertReview(currentStudentId, bookId, rating, text)
                    }.onSuccess {
                        showReviewSheet = false
                        snackbarHostState.showSnackbar("Review saved")
                    }.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: "Review failed")
                    }
                }
            },
        )
    }
}

@Composable
private fun AvailabilityBadge(active: ActiveBorrowRow?, dateFmt: SimpleDateFormat) {
    val now = System.currentTimeMillis()
    if (active == null) {
        Surface(
            shape = AppShape.chip,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                stringResource(R.string.available),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        return
    }
    val overdue = active.txn.dueDate < now
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            shape = AppShape.chip,
            color = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                stringResource(R.string.borrowed),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            stringResource(R.string.borrower) + ": ${active.studentName}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.due_date) + ": ${dateFmt.format(Date(active.txn.dueDate))}",
            style = MaterialTheme.typography.bodySmall,
            color = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewRowCard(r: ReviewEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = AppShape.field,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(r.rating.coerceIn(0, 5)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(r.reviewText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueBottomSheet(
    students: List<StudentEntity>,
    selectedStudentId: String?,
    onSelectStudent: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.issue_to_student), style = MaterialTheme.typography.titleLarge)
            students.forEach { s ->
                FilterChip(
                    selected = selectedStudentId == s.studentId,
                    onClick = { onSelectStudent(s.studentId) },
                    label = { Text("${s.name} (${s.classSection})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShape.chip,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = AppShape.field) {
                    Text(stringResource(R.string.reserve))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewBottomSheet(
    initialText: String,
    initialRating: Int,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(initialRating) }
    var text by remember { mutableStateOf(initialText) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.add_review), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { rating = star },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = star.toString(),
                            tint = if (star <= rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 120) text = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = AppShape.field,
                supportingText = { Text("${text.length}/120", style = MaterialTheme.typography.labelSmall) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSubmit(rating, text) }, modifier = Modifier.weight(1f), shape = AppShape.field) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
