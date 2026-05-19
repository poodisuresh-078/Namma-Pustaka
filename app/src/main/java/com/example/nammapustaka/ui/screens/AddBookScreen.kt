package com.example.nammapustaka.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nammapustaka.R
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.BookCategory
import com.example.nammapustaka.ui.theme.AppShape
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    repo: LibraryRepository,
    navController: NavController,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BookCategory.STORY.storageValue) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var isbn by remember { mutableStateOf("") }
    var cover by remember { mutableStateOf<Bitmap?>(null) }

    val entry = remember { navController.getBackStackEntry("add_book") }
    LaunchedEffect(entry) {
        snapshotFlow { entry.savedStateHandle.get<String>("isbn_result") }
            .distinctUntilChanged()
            .filterNotNull()
            .collect { v ->
                isbn = v
                entry.savedStateHandle.remove<String>("isbn_result")
            }
    }

    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) cover = bmp
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                if (bmp != null) {
                    cover = bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_book), style = MaterialTheme.typography.titleLarge) },
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
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text(stringResource(R.string.publisher)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedTextField(
                value = pages,
                onValueChange = { pages = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.pages) + " *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text(stringResource(R.string.summary_kn) + " *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text("ISBN (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = AppShape.field,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            OutlinedButton(
                onClick = { navController.navigate("scan/isbn") },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.field,
            ) {
                Text("Scan ISBN barcode")
            }
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { categoryMenuOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShape.field,
                ) {
                    Text("Category: ${BookCategory.fromStorage(category).labelEn}")
                }
                DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                    BookCategory.entries.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.labelEn) },
                            onClick = {
                                category = c.storageValue
                                categoryMenuOpen = false
                            },
                        )
                    }
                }
            }
            cover?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Book Cover Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    shape = AppShape.field,
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = AppShape.field,
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
            Button(
                onClick = {
                    val p = pages.toIntOrNull() ?: 0
                    if (title.isBlank() || author.isBlank() || p <= 0 || summary.isBlank()) return@Button
                    scope.launch {
                        val saved = repo.addManualBook(
                            title = title,
                            author = author,
                            category = category,
                            pageCount = p,
                            summaryKn = summary,
                            publisher = publisher,
                            coverBitmap = cover,
                        )
                        onSaved(saved.bookId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShape.field,
            ) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
