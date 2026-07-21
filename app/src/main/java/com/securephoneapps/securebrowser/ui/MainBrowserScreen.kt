package com.securephoneapps.securebrowser.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var sheetPane by remember { mutableStateOf("MENU") }

    val addressBarPosition by viewModel.addressBarPosition.collectAsState()
    val isReaderModeActive by viewModel.isReaderModeActive.collectAsState()
    val readerTitle by viewModel.readerTitle.collectAsState()
    val readerContent by viewModel.readerContent.collectAsState()

    Scaffold(
        topBar = {
            if (!isReaderModeActive && activeTab != null && activeTab.currentUrl != "about:blank" && addressBarPosition == "Top") {
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
                                    viewModel.loadUrl(input)
                                }
                            },
                            onShieldClick = {
                                sheetPane = "SHIELDS"
                                showMoreOptionsSheet = true
                            },
                            onReaderClick = {
                                viewModel.enterReaderMode(webView)
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
            if (!isReaderModeActive) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    if (activeTab != null && activeTab.currentUrl != "about:blank" && addressBarPosition == "Bottom") {
                        AddressBar(
                            viewModel = viewModel,
                            userTypedInput = urlInput,
                            onValueChange = { urlInput = it },
                            onGo = { input ->
                                if (input.isNotBlank()) {
                                    viewModel.loadUrl(input)
                                }
                            },
                            onShieldClick = {
                                sheetPane = "SHIELDS"
                                showMoreOptionsSheet = true
                            },
                            onReaderClick = {
                                viewModel.enterReaderMode(webView)
                            }
                        )
                        if (progress > 0 && progress < 100) {
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                    BottomToolbar(viewModel, webView, onShowMenu = {
                        sheetPane = "MENU"
                        showMoreOptionsSheet = true
                    })
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isReaderModeActive) {
                ReaderModeView(
                    title = readerTitle,
                    content = readerContent,
                    onExit = { viewModel.exitReaderMode() }
                )
            } else if (activeTab?.currentUrl == "about:blank" || activeTab == null) {
                HomeView(
                    viewModel = viewModel,
                    onSearch = { query ->
                        if (query.isNotBlank()) {
                            viewModel.loadUrl(query)
                        }
                    },
                    onHistoryClick = {
                        sheetPane = "HISTORY"
                        showMoreOptionsSheet = true
                    },
                    onBookmarksClick = {
                        sheetPane = "BOOKMARKS"
                        showMoreOptionsSheet = true
                    }
                )
            } else {
                BrowserWorkspace(viewModel, activeTab, webView)
            }

            if (showMoreOptionsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreOptionsSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        when (sheetPane) {
                            "MENU" -> {
                                Text(
                                    text = "Page & Privacy Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                activeTab?.let { tab ->
                                    if (tab.currentUrl != "about:blank") {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(tab.pageTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text(tab.currentUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OptionGridItem(
                                            icon = Icons.Default.Shield,
                                            title = "Tracker Shield",
                                            subtitle = "Configure blocks",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            sheetPane = "SHIELDS"
                                        }

                                        if (activeTab != null && activeTab.currentUrl != "about:blank") {
                                            val bookmarksList by viewModel.bookmarks.collectAsState()
                                            val isBookmarked = bookmarksList.any { it.url == activeTab.currentUrl }
                                            OptionGridItem(
                                                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                title = if (isBookmarked) "Bookmarked" else "Save Bookmark",
                                                subtitle = if (isBookmarked) "Remove page" else "Add to saved",
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isBookmarked) {
                                                    val bItem = bookmarksList.find { it.url == activeTab.currentUrl }
                                                    if (bItem != null) viewModel.deleteBookmarkById(bItem.id)
                                                } else {
                                                    viewModel.addBookmark(activeTab.currentUrl, activeTab.pageTitle)
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OptionGridItem(
                                            icon = Icons.Default.Bookmark,
                                            title = "Bookmarks",
                                            subtitle = "Saved pages",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            sheetPane = "BOOKMARKS"
                                        }

                                        OptionGridItem(
                                            icon = Icons.Default.History,
                                            title = "History",
                                            subtitle = "Browsed URLs",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            sheetPane = "HISTORY"
                                        }
                                    }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val forcedDark by viewModel.forcedDarkModeEnabled.collectAsState()
                                        OptionGridItem(
                                            icon = Icons.Default.DarkMode,
                                            title = "Dark Mode",
                                            subtitle = if (forcedDark) "Forced On" else "Forced Off",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            viewModel.toggleForcedDarkMode(!forcedDark)
                                        }

                                        OptionGridItem(
                                            icon = Icons.Default.PrivacyTip,
                                            title = "Private Tab",
                                            subtitle = "Incognito space",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            viewModel.createNewTab("about:blank", isIncognito = true)
                                            showMoreOptionsSheet = false
                                        }
                                    }

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OptionGridItem(
                                            icon = Icons.Default.Download,
                                            title = "Downloads",
                                            subtitle = "Vault files",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Downloads)
                                            showMoreOptionsSheet = false
                                        }

                                        OptionGridItem(
                                            icon = Icons.Default.Settings,
                                            title = "Settings",
                                            subtitle = "Configure app",
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Settings)
                                            showMoreOptionsSheet = false
                                        }
                                    }

                                    if (activeTab != null && activeTab.currentUrl != "about:blank") {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OptionGridItem(
                                                icon = Icons.Default.PictureAsPdf,
                                                title = "Save as PDF",
                                                subtitle = "Save page offline",
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                showMoreOptionsSheet = false
                                                viewModel.printActivePage(webView)
                                            }

                                            OptionGridItem(
                                                icon = Icons.Default.MenuBook,
                                                title = if (isReaderModeActive) "Disable Reader" else "Reader Mode",
                                                subtitle = if (isReaderModeActive) "Standard View" else "Distraction-Free",
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                showMoreOptionsSheet = false
                                                if (isReaderModeActive) {
                                                    viewModel.exitReaderMode()
                                                } else {
                                                    viewModel.enterReaderMode(webView)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            "SHIELDS" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    IconButton(onClick = { sheetPane = "MENU" }) {
                                        Icon(Icons.Default.ArrowBack, "Back")
                                    }
                                    Text(
                                        text = "Tracker Shield",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Shield Protection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Block ads and tracking scripts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                        Switch(
                                            checked = adBlockEnabled,
                                            onCheckedChange = { viewModel.toggleSetting(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_AD_BLOCK_ENABLED, it) }
                                        )
                                    }
                                }

                                Text("Blocked on Current Page", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                
                                val blockedLogs by viewModel.liveBlockedDomainsLog.collectAsState()
                                if (blockedLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.CheckCircle, "No trackers", tint = Color(0xFF34C759), modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Your connection is perfectly clean.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                                        items(blockedLogs) { domain ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(domain, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }

                                val telemetry by viewModel.shieldTelemetry.collectAsState()
                                telemetry?.let { t ->
                                    Spacer(Modifier.height(16.dp))
                                    Text("Global Stats", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            TelemetryStatRow("Total Ads & Trackers Intercepted", t.trackersBlockedGlobal)
                                            TelemetryStatRow("Anti-Fingerprint Defenses Triggered", t.fingerprintMocksTriggered + t.canvasFakesTriggered)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                            "BOOKMARKS" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { sheetPane = "MENU" }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                        Text(
                                            text = "Bookmarks",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                var query by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("Search Bookmarks") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = { if (query.isNotEmpty()) { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) } } },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                val bookmarksList by viewModel.bookmarks.collectAsState()
                                val filtered = bookmarksList.filter { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
                                
                                if (filtered.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                        Text("No bookmarks found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                        items(filtered) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.loadUrl(item.url)
                                                        showMoreOptionsSheet = false
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text(item.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                                                }
                                                IconButton(onClick = { viewModel.deleteBookmarkById(item.id) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                            "HISTORY" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { sheetPane = "MENU" }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                        Text(
                                            text = "History",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    val historyList by viewModel.history.collectAsState()
                                    if (historyList.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.clearHistory() }
                                        ) {
                                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                var query by remember { mutableStateOf("") }
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = { Text("Search History") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = { if (query.isNotEmpty()) { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) } } },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                val historyList by viewModel.history.collectAsState()
                                val filtered = historyList.filter { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
                                
                                if (filtered.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                        Text("No history items found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                        items(filtered) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.loadUrl(item.url)
                                                        showMoreOptionsSheet = false
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                    Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text(item.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1)
                                                }
                                                IconButton(onClick = { viewModel.deleteHistoryItem(item) }) {
                                                    Icon(Icons.Default.Close, "Delete")
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeView(
    viewModel: BrowserStateViewModel,
    onSearch: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onBookmarksClick: () -> Unit
) {
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
            HomeActionItem(Icons.Default.History, "History") { onHistoryClick() }
            HomeActionItem(Icons.Default.Bookmark, "Bookmarks") { onBookmarksClick() }
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
                modifier = Modifier.clickable { viewModel.loadUrl(url) }
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
    val addressBarPosition by viewModel.addressBarPosition.collectAsState()
    val modifier = if (addressBarPosition == "Bottom") {
        Modifier.fillMaxSize().statusBarsPadding()
    } else {
        Modifier.fillMaxSize()
    }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { }
    )
}

@Composable
fun BottomToolbar(viewModel: BrowserStateViewModel, webView: WebView, onShowMenu: () -> Unit) {
    val canBack by viewModel.canGoBack.collectAsState()
    val canForward by viewModel.canGoForward.collectAsState()
    var showFireConfirmDialog by remember { mutableStateOf(false) }

    if (showFireConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFireConfirmDialog = false },
            icon = { Icon(Icons.Default.Whatshot, "Nuclear Purge", tint = Color(0xFFFF453A)) },
            title = { Text("Nuclear Purge") },
            text = { Text("Are you sure you want to instantly destroy all tabs, cookies, web storage, and browsing history?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFireConfirmDialog = false
                        viewModel.triggerNuclearFire(webView)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                ) {
                    Text("Burn Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFireConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            IconButton(onClick = { showFireConfirmDialog = true }) { 
                Icon(Icons.Default.Whatshot, "Nuclear Fire", tint = Color(0xFFFF453A)) 
            }
            IconButton(onClick = { viewModel.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.AdvancedTabs) }) { 
                Icon(Icons.Default.Layers, "Tabs") 
            }
            IconButton(onClick = { onShowMenu() }) { 
                Icon(Icons.Default.MoreVert, "More Options") 
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
    onGo: (String) -> Unit,
    onShieldClick: () -> Unit,
    onReaderClick: (() -> Unit)? = null
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
            leadingIcon = {
                val blockedDomains by viewModel.liveBlockedDomains.collectAsState()
                val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
                IconButton(onClick = { onShieldClick() }) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Protection",
                        tint = if (adBlockEnabled) {
                            if (blockedDomains.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onReaderClick != null) {
                        IconButton(onClick = onReaderClick) {
                            Icon(Icons.Default.MenuBook, "Reader Mode", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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
                        viewModel.loadUrl(suggestion)
                        keyboardController?.hide()
                    }
                )
            }
        }
    }
}

@Composable
fun OptionGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun TelemetryStatRow(label: String, value: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ReaderModeView(
    title: String,
    content: List<String>,
    onExit: () -> Unit
) {
    var fontSizeMultiplier by remember { mutableStateOf(1.0f) }
    var readerTheme by remember { mutableStateOf("Cream") } // "Cream", "Sepia", "Dark"

    val backgroundColor = when (readerTheme) {
        "Cream" -> Color(0xFFFDF6E3)
        "Sepia" -> Color(0xFFF4ECD8)
        "Dark" -> Color(0xFF1E1E1E)
        else -> Color(0xFFFFFFFF)
    }

    val textColor = when (readerTheme) {
        "Dark" -> Color(0xFFE0E0E0)
        else -> Color(0xFF2C2C2C)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, "Exit Reader", tint = textColor)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { if (fontSizeMultiplier > 0.8f) fontSizeMultiplier -= 0.1f }) {
                    Icon(Icons.Default.TextFormat, "Decrease Font", tint = textColor, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { if (fontSizeMultiplier < 2.0f) fontSizeMultiplier += 0.1f }) {
                    Icon(Icons.Default.TextFormat, "Increase Font", tint = textColor, modifier = Modifier.size(24.dp))
                }
                
                listOf("Cream", "Sepia", "Dark").forEach { theme ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = when (theme) {
                                    "Cream" -> Color(0xFFFDF6E3)
                                    "Sepia" -> Color(0xFFF4ECD8)
                                    else -> Color(0xFF1E1E1E)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { readerTheme = theme }
                            .padding(2.dp)
                    ) {
                        if (readerTheme == theme) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = textColor.copy(alpha = 0.1f))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * fontSizeMultiplier).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (MaterialTheme.typography.headlineMedium.lineHeight.value * fontSizeMultiplier).sp
                    ),
                    color = textColor
                )
            }

            items(content) { paragraph ->
                if (paragraph.startsWith("## ")) {
                    Text(
                        text = paragraph.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * fontSizeMultiplier).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (MaterialTheme.typography.titleLarge.lineHeight.value * fontSizeMultiplier).sp
                        ),
                        color = textColor
                    )
                } else {
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * fontSizeMultiplier).sp,
                            lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value * 1.5 * fontSizeMultiplier).sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}
