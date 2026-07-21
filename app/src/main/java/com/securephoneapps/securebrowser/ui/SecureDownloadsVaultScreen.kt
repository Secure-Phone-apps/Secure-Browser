package com.securephoneapps.securebrowser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDownloadsVaultScreen(
    viewModel: BrowserStateViewModel,
    onClose: () -> Unit
) {
    val downloadedFiles by viewModel.downloadedFilesList.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedFiles(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🛡️ SECURE VAULT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (downloadedFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No secure files found in sandbox",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(downloadedFiles) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${file.length() / 1024} KB • Encrypted Vault",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.exportFileToPublicStorage(context, file) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Export",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.purgeFile(context, file) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Purge",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
