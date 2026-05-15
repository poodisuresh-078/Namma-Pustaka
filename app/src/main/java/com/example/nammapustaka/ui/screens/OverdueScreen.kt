package com.example.nammapustaka.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.ui.theme.AppShape
import com.example.nammapustaka.ui.theme.OverdueRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueScreen(repo: LibraryRepository) {
    val rows by repo.observeOverdueRows().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var pickerTxn by remember { mutableStateOf<ActiveBorrowRow?>(null) }
    val pickerState = rememberDatePickerState()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.nav_overdue),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(rows, key = { it.txn.txnId }) { row ->
            val now = System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(now - row.txn.dueDate).toInt().coerceAtLeast(1)
            Card(
                shape = AppShape.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.bookTitle, style = MaterialTheme.typography.titleMedium, color = OverdueRed)
                    Text(row.studentName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Issued ${fmt.format(Date(row.txn.issueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Due ${fmt.format(Date(row.txn.dueDate))} · $days days overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdueRed,
                    )
                    TextButton(
                        onClick = { pickerTxn = row },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.extend_due))
                    }
                }
            }
        }
    }

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
