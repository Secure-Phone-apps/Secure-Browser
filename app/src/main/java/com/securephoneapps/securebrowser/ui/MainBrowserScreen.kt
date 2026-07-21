package com.securephoneapps.securebrowser.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securephoneapps.securebrowser.model.TabInstance
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBrowserScreen(viewModel: BrowserStateViewModel, webView: WebView) {
    val activeTabId by viewModel.activeTabId.collectAsState()
    val tabs by viewModel.tabsState.collectAsState()
    val activeTab = tabs.find { it.tabId == activeTabId }

    var urlInput by remember(activeTab?.currentUrl) { mutableStateOf(activeTab?.currentUrl ?: "") }
    val progress by viewModel.pageLoadingProgress.collectAsState()

    Scaffold(
        topBar = {
            if (activeTab != null && activeTab.currentUrl != "about:blank") {
                Column {
                    Surface(
                        modifier = Modifier.statusBarsPadding(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AddressBar(
                            viewModel = viewModel,
                            userTypedInput = urlInput,
                            onValueChange = { urlInput = it },
                            onGo = { input ->
                                if (input.isNotBlank()) {
                                    val resolvedUrl = viewModel.executeUrlResolution(input)
                                    viewModel.updateActiveTabUrl(resolvedUrl, "Loading...")
                                }
                            }
                        )
                    }
                    if (progress > 0 && progress < 100) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomToolbar(viewModel, webView)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (activeTab?.currentUrl == "about:blank" || activeTab == null) {
                HomeView(
                    viewModel = viewModel,
                    onSearch = { query ->
                        val resolvedUrl = viewModel.executeUrlResolution(query)
                        viewModel.updateActiveTabUrl(resolvedUrl, "Loading...")
                    }
                )
            } else {
                BrowserWorkspace(viewModel, activeTab, webView)
            }
        }
    }
}

@Composable
fun HomeView(viewModel: BrowserStateViewModel, onSearch: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo or Icon
        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Secure Browser",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Centered Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("Search privately...") },
            colors = OutlinedTextFieldDefaults.colors(),
            trailingIcon = {
                IconButton(onClick = { onSearch(searchQuery) }) {
                    Icon(Icons.Default.Search, null)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
            singleLine = true
        )
        
        Spacer(Modifier.height(48.dp))
        
        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeActionItem(Icons.Default.History, "History") { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Browser) }
            HomeActionItem(Icons.Default.Bookmark, "Bookmarks") { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Browser) }
            HomeActionItem(Icons.Default.Download, "Downloads") { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Downloads) }
            HomeActionItem(Icons.Default.Settings, "Settings") { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Settings) }
        }

        Spacer(Modifier.height(32.dp))

        // Quick Access Bookmarks
        Text(
            "Quick Access",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        QuickAccessGrid(viewModel)
    }
}

@Composable
fun QuickAccessGrid(viewModel: BrowserStateViewModel) {
    val quickAccess = listOf(
        "DuckDuckGo" to "https://duckduckgo.com",
        "Wikipedia" to "https://wikipedia.org",
        "Reddit" to "https://reddit.com",
        "GitHub" to "https://github.com"
    )
    
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        quickAccess.forEach { (name, url) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { viewModel.updateActiveTabUrl(url, name) }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.take(1), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Text(name, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun HomeActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun BrowserWorkspace(viewModel: BrowserStateViewModel, tab: TabInstance, webView: WebView) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
        update = { }
    )
}

@Composable
fun BottomToolbar(viewModel: BrowserStateViewModel, webView: WebView) {
    val canBack by viewModel.canGoBack.collectAsState()
    val canForward by viewModel.canGoForward.collectAsState()

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { if (webView.canGoBack()) webView.goBack() }) { 
                Icon(Icons.Default.ArrowBack, "Back", tint = if (canBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) 
            }
            IconButton(onClick = { if (webView.canGoForward()) webView.goForward() }) { 
                Icon(Icons.Default.ArrowForward, "Forward", tint = if (canForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) 
            }
            IconButton(onClick = { 
                viewModel.updateActiveTabUrl("about:blank", "Home")
            }) { 
                Icon(Icons.Default.Home, "Home", tint = MaterialTheme.colorScheme.primary) 
            }
            IconButton(onClick = { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.AdvancedTabs) }) { 
                Icon(Icons.Default.Layers, "Tabs") 
            }
            IconButton(onClick = { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Settings) }) { 
                Icon(Icons.Default.Settings, "Settings") 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBar(
    viewModel: BrowserStateViewModel,
    userTypedInput: String,
    onValueChange: (String) -> Unit,
    onGo: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        OutlinedTextField(
            value = userTypedInput,
            onValueChange = {
                onValueChange(it)
                viewModel.fetchSearchSuggestions(it)
                isDropdownExpanded = it.isNotBlank()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("Search or enter URL") },
            colors = OutlinedTextFieldDefaults.colors(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onGo(userTypedInput)
                keyboardController?.hide()
                focusManager.clearFocus()
            }),
            trailingIcon = {
                IconButton(
                    onClick = { 
                        onGo(userTypedInput)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        )

        DropdownMenu(
            expanded = isDropdownExpanded && suggestions.isNotEmpty(),
            onDismissRequest = { isDropdownExpanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        isDropdownExpanded = false
                        val resolvedUrl = viewModel.executeUrlResolution(suggestion)
                        viewModel.updateActiveTabUrl(resolvedUrl, "Loading...")
                        keyboardController?.hide()
                    }
                )
            }
        }
    }
}
