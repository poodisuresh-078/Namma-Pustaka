package com.example.nammapustaka.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.LeaderboardRow
import com.example.nammapustaka.ui.theme.AppShape

@Composable
fun LeaderboardScreen(repo: LibraryRepository, isTeacher: Boolean) {
    var monthsAgo by remember { mutableIntStateOf(0) }
    var monthMenuOpen by remember { mutableStateOf(false) }
    val rows by remember(monthsAgo) {
        if (monthsAgo == 0) repo.observeLeaderboardCurrentMonth() else repo.observeLeaderboardForPastMonth(monthsAgo)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val monthLabel = remember(monthsAgo) {
        com.example.nammapustaka.util.TimeUtil.monthRangeMillis(monthsAgo).label
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.nav_leaderboard),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (isTeacher) {
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { monthMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShape.field,
                ) {
                    Text(stringResource(R.string.archive_months) + ": " + monthLabel)
                }
                DropdownMenu(expanded = monthMenuOpen, onDismissRequest = { monthMenuOpen = false }) {
                    (0..3).forEach { idx ->
                        val label = com.example.nammapustaka.util.TimeUtil.monthRangeMillis(idx).label
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                monthsAgo = idx
                                monthMenuOpen = false
                            },
                        )
                    }
                }
            }
        } else {
            Text(monthLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
            itemsIndexed(rows, key = { _, r -> r.studentId }) { index, row ->
                LeaderRowCard(rank = index + 1, row = row, isTeacher = isTeacher)
            }
        }
    }
}

@Composable
private fun LeaderRowCard(rank: Int, row: LeaderboardRow, isTeacher: Boolean) {
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShape.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = AppShape.chip,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(width = 40.dp, height = 40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = medal ?: "$rank",
                            style = if (medal != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column {
                    Text(row.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (isTeacher) {
                        Text(row.classSection, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${row.pagesRead}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("pages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${row.booksRead} books", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
