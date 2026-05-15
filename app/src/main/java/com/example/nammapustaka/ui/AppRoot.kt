package com.example.nammapustaka.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.nammapustaka.NammaPustakaApplication
import com.example.nammapustaka.R
import com.example.nammapustaka.ui.screens.AddBookScreen
import com.example.nammapustaka.ui.screens.BookDetailScreen
import com.example.nammapustaka.ui.screens.CatalogScreen
import com.example.nammapustaka.ui.screens.LeaderboardScreen
import com.example.nammapustaka.ui.screens.OverdueScreen
import com.example.nammapustaka.ui.screens.ScanScreen
import kotlinx.coroutines.launch

private data class TabSpec(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as NammaPustakaApplication
    val repo = app.libraryRepository
    val prefs = app.userPreferences
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val isTeacher by prefs.isTeacherMode.collectAsStateWithLifecycle(initialValue = true)
    val currentStudentId by prefs.currentStudentId.collectAsStateWithLifecycle(initialValue = null)

    var showRoleDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val teacherTabs = remember {
        listOf(
            TabSpec("catalog", R.string.nav_catalog, Icons.Default.MenuBook),
            TabSpec("scan/qr", R.string.nav_scan, Icons.Default.QrCodeScanner),
            TabSpec("overdue", R.string.nav_overdue, Icons.Default.PriorityHigh),
            TabSpec("leaderboard", R.string.nav_leaderboard, Icons.Default.EmojiEvents),
        )
    }
    val studentTabs = remember {
        listOf(
            TabSpec("catalog", R.string.nav_catalog, Icons.Default.MenuBook),
            TabSpec("leaderboard", R.string.nav_leaderboard, Icons.Default.EmojiEvents),
        )
    }
    val tabs = if (isTeacher) teacherTabs else studentTabs

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = when {
        currentRoute == null -> true
        currentRoute.startsWith("detail") -> false
        currentRoute == "add_book" -> false
        currentRoute.startsWith("scan") -> false
        else -> true
    }

    LaunchedEffect(isTeacher, currentStudentId) {
        if (!isTeacher && currentStudentId == null) {
            showStudentDialog = true
        }
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Who is using the device?") },
            text = { Text("Teachers manage books and borrowing. Students browse and review after returns.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            prefs.setTeacherMode(true)
                            showRoleDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.role_teacher)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            prefs.setTeacherMode(false)
                            showRoleDialog = false
                            showStudentDialog = true
                        }
                    },
                ) { Text(stringResource(R.string.role_student)) }
            },
        )
    }

    if (showStudentDialog && !isTeacher) {
        val students by repo.observeStudents().collectAsStateWithLifecycle(initialValue = emptyList())
        AlertDialog(
            onDismissRequest = { showStudentDialog = false },
            title = { Text(stringResource(R.string.pick_student_profile)) },
            text = {
                LazyColumn {
                    items(students, key = { it.studentId }) { s ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    prefs.setCurrentStudentId(s.studentId)
                                    showStudentDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${s.name} (${s.classSection})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudentDialog = false }) { Text("Close") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "catalog",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("catalog") {
                val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(repo))
                CatalogScreen(
                    isTeacher = isTeacher,
                    onOpenAddBook = { navController.navigate("add_book") },
                    onOpenRoleSettings = { showRoleDialog = true },
                    onBookClick = { id -> navController.navigate("detail/$id") },
                    viewModel = vm,
                )
            }
            composable("add_book") {
                AddBookScreen(
                    repo = repo,
                    navController = navController,
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate("detail/$id")
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "detail/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val bookId = entry.arguments?.getString("bookId").orEmpty()
                BookDetailScreen(
                    bookId = bookId,
                    repo = repo,
                    isTeacher = isTeacher,
                    currentStudentId = currentStudentId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "scan/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "qr" }),
            ) { entry ->
                val mode = entry.arguments?.getString("mode").orEmpty()
                ScanScreen(
                    mode = mode,
                    repo = repo,
                    navController = navController,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("overdue") {
                OverdueScreen(repo = repo)
            }
            composable("leaderboard") {
                LeaderboardScreen(repo = repo, isTeacher = isTeacher)
            }
        }
    }
}
