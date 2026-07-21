package com.securephoneapps.securebrowser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDownloadsVaultScreen(viewModel: BrowserStateViewModel) {
    val context = LocalContext.current
    val files by viewModel.downloadedFilesList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedFiles(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(BrowserStateViewModel.Screen.Browser) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDownloadedFiles(context) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { viewModel.purgeAllDownloads(context) }) {
                        Icon(Icons.Default.DeleteSweep, "Purge All", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No isolated files found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(files) { file ->
                    DownloadItem(
                        context = context,
                        file = file,
                        onOpen = { viewModel.viewFile(context, file) },
                        onExport = { viewModel.exportFile(context, file) },
                        onDelete = { viewModel.purgeFile(context, file) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItem(context: android.content.Context, file: File, onOpen: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilePresent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpen) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.IosShare, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }
    }
}
