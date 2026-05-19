package com.example.nammapustaka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.data.local.AllBorrowRow
import com.example.nammapustaka.ui.theme.AppShape
import com.example.nammapustaka.ui.theme.OverdueRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class RecordsTab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(repo: LibraryRepository) {
    val tabs = remember {
        listOf(
            RecordsTab("Borrowed", Icons.Default.Book),
            RecordsTab("Returned", Icons.Default.AssignmentReturn),
            RecordsTab("Overdue", Icons.Default.Warning),
            RecordsTab("Transactions", Icons.Default.History),
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    val activeBorrows by repo.observeActiveBorrows().collectAsStateWithLifecycle(initialValue = emptyList())
    val returnedBorrows by repo.observeReturnedBorrows().collectAsStateWithLifecycle(initialValue = emptyList())
    val overdueRows by repo.observeOverdueRows().collectAsStateWithLifecycle(initialValue = emptyList())
    val allBorrows by repo.observeAllBorrows().collectAsStateWithLifecycle(initialValue = emptyList())

    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    var pickerTxn by remember { mutableStateOf<ActiveBorrowRow?>(null) }
    val pickerState = rememberDatePickerState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_records),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Book borrowing activity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Tab Row ──────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Tab(
                    selected = selected,
                    onClick = { selectedTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                        // Badge count
                        val count = when (index) {
                            0 -> activeBorrows.size
                            1 -> returnedBorrows.size
                            2 -> overdueRows.size
                            3 -> allBorrows.size
                            else -> 0
                        }
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Tab Content ──────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> BorrowedTab(rows = activeBorrows, fmt = fmt)
                1 -> ReturnedTab(rows = returnedBorrows, fmt = fmt)
                2 -> OverdueTab(
                    rows = overdueRows,
                    fmt = fmt,
                    onExtend = { row -> pickerTxn = row },
                )
                3 -> TransactionsTab(rows = allBorrows, fmt = fmt)
            }
        }
    }

    // ── Date Picker for extend due ───────────────────────────────────────
    val pick = pickerTxn
    if (pick != null) {
        DatePickerDialog(
            onDismissRequest = { pickerTxn = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: return@TextButton
                        scope.launch {
                            repo.extendDueDate(pick.txn.txnId, millis)
                            pickerTxn = null
                        }
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerTxn = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ── Borrowed Tab (currently issued — ISSUED + OVERDUE) ───────────────────────
@Composable
private fun BorrowedTab(rows: List<ActiveBorrowRow>, fmt: SimpleDateFormat) {
    if (rows.isEmpty()) {
        EmptyState(message = "No books currently borrowed")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.txn.txnId }) { row ->
            val isOverdue = row.txn.status == "OVERDUE"
            RecordCard(
                title = row.bookTitle,
                subtitle = row.studentName,
                line1 = "Issued: ${fmt.format(Date(row.txn.issueDate))}",
                line2 = "Due: ${fmt.format(Date(row.txn.dueDate))}",
                statusLabel = if (isOverdue) "Overdue" else "Issued",
                statusColor = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Returned Tab ─────────────────────────────────────────────────────────────
@Composable
private fun ReturnedTab(rows: List<AllBorrowRow>, fmt: SimpleDateFormat) {
    if (rows.isEmpty()) {
        EmptyState(message = "No books returned yet")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.txn.txnId }) { row ->
            RecordCard(
                title = row.bookTitle,
                subtitle = row.studentName,
                line1 = "Issued: ${fmt.format(Date(row.txn.issueDate))}",
                line2 = row.txn.returnDate?.let { "Returned: ${fmt.format(Date(it))}" } ?: "",
                statusLabel = "Returned",
                statusColor = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

// ── Overdue Tab ───────────────────────────────────────────────────────────────
@Composable
private fun OverdueTab(
    rows: List<ActiveBorrowRow>,
    fmt: SimpleDateFormat,
    onExtend: (ActiveBorrowRow) -> Unit,
) {
    if (rows.isEmpty()) {
        EmptyState(message = "No overdue books 🎉")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.txn.txnId }) { row ->
            val now = System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(now - row.txn.dueDate).toInt().coerceAtLeast(1)
            Card(
                shape = AppShape.card,
                colors = CardDefaults.cardColors(
                    containerColor = OverdueRed.copy(alpha = 0.06f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        row.bookTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdueRed,
                    )
                    Text(
                        row.studentName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Issued: ${fmt.format(Date(row.txn.issueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Due: ${fmt.format(Date(row.txn.dueDate))} · $days day(s) overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdueRed,
                    )
                    TextButton(
                        onClick = { onExtend(row) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.extend_due))
                    }
                }
            }
        }
    }
}

// ── Transactions Tab (all) ────────────────────────────────────────────────────
@Composable
private fun TransactionsTab(rows: List<AllBorrowRow>, fmt: SimpleDateFormat) {
    if (rows.isEmpty()) {
        EmptyState(message = "No transactions yet")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.txn.txnId }) { row ->
            val statusColor = when (row.txn.status) {
                "RETURNED" -> MaterialTheme.colorScheme.tertiary
                "OVERDUE" -> OverdueRed
                else -> MaterialTheme.colorScheme.primary
            }
            val statusLabel = row.txn.status.lowercase()
                .replaceFirstChar { it.uppercase() }
            RecordCard(
                title = row.bookTitle,
                subtitle = row.studentName,
                line1 = "Issued: ${fmt.format(Date(row.txn.issueDate))}",
                line2 = row.txn.returnDate?.let { "Returned: ${fmt.format(Date(it))}" }
                    ?: "Due: ${fmt.format(Date(row.txn.dueDate))}",
                statusLabel = statusLabel,
                statusColor = statusColor,
            )
        }
    }
}

// ── Shared Card ───────────────────────────────────────────────────────────────
@Composable
private fun RecordCard(
    title: String,
    subtitle: String,
    line1: String,
    line2: String,
    statusLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
) {
    Card(
        shape = AppShape.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (line1.isNotEmpty()) {
                Text(
                    line1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line2.isNotEmpty()) {
                Text(
                    line2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
