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
import kotlinx.coroutines.flow.first
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
import com.example.nammapustaka.data.local.UserRole
import com.example.nammapustaka.data.prefs.SavedProfile
import com.example.nammapustaka.ui.screens.AddBookScreen
import com.example.nammapustaka.ui.screens.BookDetailScreen
import com.example.nammapustaka.ui.screens.CatalogScreen
import com.example.nammapustaka.ui.screens.LeaderboardScreen
import com.example.nammapustaka.ui.screens.LoginScreen
import com.example.nammapustaka.ui.screens.OverdueScreen
import com.example.nammapustaka.ui.screens.RecordsScreen
import com.example.nammapustaka.ui.screens.RequestsScreen
import com.example.nammapustaka.ui.screens.ProfileDashboardBottomSheet
import com.example.nammapustaka.ui.screens.ScanScreen
import com.example.nammapustaka.ui.screens.StudentSignUpScreen
import com.example.nammapustaka.ui.screens.TeacherSignUpScreen
import com.example.nammapustaka.ui.screens.ForgotPasswordScreen
import androidx.compose.material.icons.filled.Inbox
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
    val loggedInUserId by prefs.loggedInUserId.collectAsStateWithLifecycle(initialValue = null)
    val loggedInRole by prefs.loggedInRole.collectAsStateWithLifecycle(initialValue = null)
    val loggedInDisplayName by prefs.loggedInDisplayName.collectAsStateWithLifecycle(initialValue = null)
    val savedProfiles by prefs.savedProfiles.collectAsStateWithLifecycle(initialValue = emptyList())

    // Auth loaded flag: null = not yet read, String? = value from DataStore
    var isAuthLoaded by remember { mutableStateOf(false) }
    var initialLoggedInUserId by remember { mutableStateOf<String?>(null) }

    var showProfileDashboard by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    // Holds the profile to offer saving after a successful login
    var pendingSaveProfile by remember { mutableStateOf<SavedProfile?>(null) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // Read the initial auth state once before showing any screen
    LaunchedEffect(Unit) {
        initialLoggedInUserId = prefs.loggedInUserId.first()
        isAuthLoaded = true
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val teacherTabs = remember {
        listOf(
            TabSpec("catalog", R.string.nav_catalog, Icons.Default.MenuBook),
            TabSpec("scan/qr", R.string.nav_scan, Icons.Default.QrCodeScanner),
            TabSpec("records", R.string.nav_records, Icons.Default.PriorityHigh),
            TabSpec("requests", R.string.nav_requests, Icons.Default.Inbox),
            TabSpec("leaderboard", R.string.nav_leaderboard, Icons.Default.EmojiEvents),
        )
    }
    val studentTabs = remember {
        listOf(
            TabSpec("catalog", R.string.nav_catalog, Icons.Default.MenuBook),
            TabSpec("requests", R.string.nav_requests, Icons.Default.Inbox),
            TabSpec("leaderboard", R.string.nav_leaderboard, Icons.Default.EmojiEvents),
        )
    }
    val tabs = if (isTeacher) teacherTabs else studentTabs

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = when {
        currentRoute == null -> true
        currentRoute == "login" -> false
        currentRoute == "student_signup" -> false
        currentRoute == "teacher_signup" -> false
        currentRoute.startsWith("detail") -> false
        currentRoute == "add_book" -> false
        currentRoute.startsWith("scan") -> false
        else -> true
    }

    // If the user logs out while on a protected screen, send them back to login
    LaunchedEffect(loggedInUserId) {
        if (isAuthLoaded && loggedInUserId == null && currentRoute != null &&
            currentRoute != "login" && currentRoute != "student_signup" && currentRoute != "teacher_signup"
        ) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(isTeacher, currentStudentId) {
        if (!isTeacher && currentStudentId == null && loggedInUserId != null) {
            showStudentDialog = true
        }
    }

    if (showProfileDashboard) {
        ProfileDashboardBottomSheet(
            isTeacher = isTeacher,
            studentId = currentStudentId,
            studentName = loggedInDisplayName,
            repo = repo,
            onDismiss = { showProfileDashboard = false },
            onSwitchRole = {
                scope.launch {
                    val nextTeacher = !isTeacher
                    prefs.setTeacherMode(nextTeacher)
                    if (!nextTeacher && currentStudentId == null) {
                        showStudentDialog = true
                    }
                }
            }
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

    // ── Save Profile dialog ───────────────────────────────────────
    pendingSaveProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingSaveProfile = null },
            title = { Text("Save profile?") },
            text = {
                Text(
                    "Save \"${profile.displayName}\" for quick sign-in next time? " +
                    "Your password is never stored.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { prefs.saveProfile(profile) }
                        pendingSaveProfile = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSaveProfile = null }) { Text("Skip") }
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
        // Wait for the first DataStore read before showing any screen
        if (!isAuthLoaded) return@Scaffold

        val startDestination = if (initialLoggedInUserId != null) "catalog" else "login"

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("login") {
                LoginScreen(
                    onLogin = { role, credential, password ->
                        loginError = null
                        scope.launch {
                            val user = repo.login(credential, password)
                            if (user != null) {
                                prefs.setLoggedIn(
                                    userId = user.userId,
                                    role = user.role,
                                    displayName = user.displayName,
                                    studentId = user.linkedStudentId,
                                )
                                com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                                    userId = user.userId,
                                    username = user.username,
                                    displayName = user.displayName,
                                    role = user.role,
                                    action = "LOGIN"
                                )
                                // Offer to save profile if not already saved
                                val alreadySaved = prefs.savedProfiles.first()
                                    .any { it.userId == user.userId }
                                if (!alreadySaved) {
                                    pendingSaveProfile = SavedProfile(
                                        userId = user.userId,
                                        username = user.username,
                                        displayName = user.displayName,
                                        role = user.role,
                                    )
                                }
                                navController.navigate("catalog") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                loginError = "Invalid username or password"
                            }
                        }
                    },
                    onStudentSignUp = {
                        navController.navigate("student_signup")
                    },
                    onTeacherSignUp = {
                        navController.navigate("teacher_signup")
                    },
                    onForgotPassword = {
                        navController.navigate("forgot_password")
                    },
                    savedProfiles = savedProfiles,
                    onSelectProfile = { profile ->
                        loginError = null
                        scope.launch {
                            val user = repo.getAppUserById(profile.userId)
                            if (user != null) {
                                prefs.setLoggedIn(
                                    userId = user.userId,
                                    role = user.role,
                                    displayName = user.displayName,
                                    studentId = user.linkedStudentId,
                                )
                                com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                                    userId = user.userId,
                                    username = user.username,
                                    displayName = user.displayName,
                                    role = user.role,
                                    action = "LOGIN_SAVED_PROFILE"
                                )
                                navController.navigate("catalog") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                loginError = "Profile no longer exists"
                            }
                        }
                    },
                    onRemoveProfile = { profile ->
                        scope.launch { prefs.removeProfile(profile.userId) }
                    },
                    errorMessage = loginError,
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(
                    repo = repo,
                    onPasswordResetSuccess = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("student_signup") {
                StudentSignUpScreen(
                    repo = repo,
                    onAccountCreated = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onSignIn = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("teacher_signup") {
                TeacherSignUpScreen(
                    repo = repo,
                    onAccountCreated = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onSignIn = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                )
            }
            composable("catalog") {
                val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(repo))
                CatalogScreen(
                    isTeacher = isTeacher,
                    onOpenAddBook = { navController.navigate("add_book") },
                    onOpenRoleSettings = { showProfileDashboard = true },
                    onLogout = {
                        scope.launch {
                            val uid = loggedInUserId
                            val name = loggedInDisplayName ?: ""
                            val role = loggedInRole ?: ""
                            if (uid != null) {
                                com.example.nammapustaka.data.remote.FirestoreService.logActivity(
                                    userId = uid,
                                    username = name,
                                    displayName = name,
                                    role = role,
                                    action = "LOGOUT"
                                )
                            }
                            prefs.logout()
                        }
                    },
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
                    navController = navController,
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
            composable("records") {
                RecordsScreen(repo = repo)
            }
            composable("leaderboard") {
                LeaderboardScreen(repo = repo, isTeacher = isTeacher)
            }
            composable("requests") {
                RequestsScreen(
                    repo = repo,
                    isTeacher = isTeacher,
                    studentId = currentStudentId,
                    studentName = loggedInDisplayName
                )
            }
        }
    }
}
