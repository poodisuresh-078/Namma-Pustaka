package com.example.nammapustaka.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.ActiveBorrowRow
import com.example.nammapustaka.data.local.BookEntity
import com.example.nammapustaka.ui.theme.AppShape
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import androidx.concurrent.futures.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    mode: String,
    repo: LibraryRepository,
    navController: NavController,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasCamPermission = ok
    }
    var rationale by remember { mutableStateOf(false) }

    var scannedBook by remember { mutableStateOf<BookEntity?>(null) }
    var activeRow by remember { mutableStateOf<ActiveBorrowRow?>(null) }
    var lastPayload by remember { mutableStateOf<String?>(null) }

    val isIsbnMode = mode == "isbn"
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(hasCamPermission, isIsbnMode) {
        if (!hasCamPermission) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue ?: return@addOnSuccessListener
                    if (raw == lastPayload) return@addOnSuccessListener
                    lastPayload = raw
                    scope.launch {
                        if (isIsbnMode) {
                            navController.previousBackStackEntry?.savedStateHandle?.set("isbn_result", raw)
                            navController.popBackStack()
                        } else {
                            val book = repo.getBookByQr(raw) ?: return@launch
                            scannedBook = book
                            val txn = repo.getActiveTxn(book.bookId)
                            activeRow = if (txn != null) {
                                val st = repo.getStudent(txn.studentId)
                                ActiveBorrowRow(txn, st?.name ?: "?", book.title)
                            } else {
                                null
                            }
                        }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
        )
        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
        }
    }

    if (rationale) {
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text(stringResource(R.string.camera_permission_title)) },
            text = { Text(stringResource(R.string.camera_permission_body)) },
            confirmButton = {
                Button(onClick = {
                    rationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text(stringResource(R.string.grant_permission)) }
            },
            dismissButton = {
                Button(onClick = { rationale = false }) { Text("Not now") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isIsbnMode) "Scan ISBN" else "Scan book QR", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!hasCamPermission) {
                Button(
                    onClick = {
                        val act = context as? Activity
                        val shouldShow = act != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(act, Manifest.permission.CAMERA)
                        if (shouldShow) rationale = true else permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            } else {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
        }
    }

    val book = scannedBook
    if (book != null && !isIsbnMode) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                scannedBook = null
                activeRow = null
                lastPayload = null
            },
            sheetState = sheetState,
        ) {
            BorrowSheetContent(
                book = book,
                active = activeRow,
                repo = repo,
                onClose = {
                    scannedBook = null
                    activeRow = null
                    lastPayload = null
                },
            )
        }
    }
}

@Composable
private fun BorrowSheetContent(
    book: BookEntity,
    active: ActiveBorrowRow?,
    repo: LibraryRepository,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val students by repo.observeStudents().collectAsStateWithLifecycle(initialValue = emptyList())
    var selected by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleLarge)
        if (active != null) {
            Text("${stringResource(R.string.borrower)}: ${active.studentName}", style = MaterialTheme.typography.bodyLarge)
            Text("${stringResource(R.string.due_date)}: ${fmt.format(Date(active.txn.dueDate))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        repo.returnBookForActive(book.bookId)
                        onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.field,
            ) { Text(stringResource(R.string.mark_returned)) }
        } else {
            Text(stringResource(R.string.issue_to_student), style = MaterialTheme.typography.titleSmall)
            students.forEach { s ->
                FilterChip(
                    selected = selected == s.studentId,
                    onClick = { selected = s.studentId },
                    label = { Text("${s.name} (${s.classSection})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShape.chip,
                )
            }
            Button(
                onClick = {
                    val sid = selected ?: return@Button
                    scope.launch {
                        runCatching { repo.issueBook(book.bookId, sid) }.onSuccess { onClose() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.field,
            ) { Text(stringResource(R.string.reserve)) }
        }
    }
}
