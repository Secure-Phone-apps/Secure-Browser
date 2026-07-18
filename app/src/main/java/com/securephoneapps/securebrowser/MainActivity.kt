package com.securephoneapps.securebrowser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.securephoneapps.securebrowser.engine.FingerprintShieldBridge
import com.securephoneapps.securebrowser.engine.HardenedWebViewClient
import com.securephoneapps.securebrowser.engine.ShieldsCoreEngine
import com.securephoneapps.securebrowser.ui.AdvancedTabManagerScreen
import com.securephoneapps.securebrowser.ui.GranularControlSettingsScreen
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

class MainActivity : ComponentActivity() {

    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val customLightPalette = lightColorScheme(
                primary = Color(0xFF2563EB), // Blue 600
                secondary = Color(0xFF059669), // Emerald 600
                background = Color(0xFFF8FAFC), // Slate 50 background
                surface = Color(0xFFFFFFFF), // White container background
                onPrimary = Color.White,
                onBackground = Color(0xFF0F172A), // Slate 900 text
                onSurface = Color(0xFF0F172A), // Slate 900 text
                outline = Color(0xFFE2E8F0) // Slate 200 border
            )

            MaterialTheme(colorScheme = customLightPalette) {
                val viewModel: BrowserStateViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentScreen) {
                        BrowserStateViewModel.Screen.Browser -> {
                            BrowserWorkspaceScreen(
                                viewModel = viewModel,
                                onWebViewBound = { activeWebView = it }
                            )
                        }
                        BrowserStateViewModel.Screen.TabManager -> {
                            AdvancedTabManagerScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(BrowserStateViewModel.Screen.Browser) }
                            )
                        }
                        BrowserStateViewModel.Screen.Settings -> {
                            GranularControlSettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(BrowserStateViewModel.Screen.Browser) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (activeWebView != null && activeWebView!!.canGoBack()) {
            activeWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

fun configureEngineParameters(settings: WebSettings) {
    settings.apply {
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        databaseEnabled = false
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        savePassword = false
        saveFormData = false
        mediaPlaybackRequiresUserGesture = true
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        
        // Geolocation block
        setGeolocationEnabled(false)

        // Disable WebGL via reflection
        try {
            val setWebGLEnabledMethod = settings.javaClass.getMethod("setWebGLEnabled", Boolean::class.java)
            setWebGLEnabledMethod.invoke(settings, false)
        } catch (e: Exception) {
            // WebGL reflection not supported or unnecessary on this target SDK
        }

        // Enable Safe Browsing via reflection
        try {
            val setSafeBrowsingEnabledMethod = settings.javaClass.getMethod("setSafeBrowsingEnabled", Boolean::class.java)
            setSafeBrowsingEnabledMethod.invoke(settings, true)
        } catch (e: Exception) {
            // Safe Browsing reflection not supported
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWorkspaceScreen(
    viewModel: BrowserStateViewModel,
    onWebViewBound: (WebView) -> Unit
) {
    val tabs by viewModel.tabsState.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.tabId == activeTabId }

    val jsEnabled by viewModel.javascriptEnabled.collectAsState()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
    val selectedUa by viewModel.selectedUserAgent.collectAsState()
    val httpsOnly by viewModel.httpsOnlyMode.collectAsState()

    var inputUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sslSecured by remember { mutableStateOf(true) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val shieldsEngine = remember { ShieldsCoreEngine() }

    // Synchronize Input Field when active tab switches or navigates
    LaunchedEffect(activeTab?.currentUrl) {
        if (activeTab != null) {
            inputUrl = if (activeTab.currentUrl == "about:blank") "" else activeTab.currentUrl
            sslSecured = activeTab.currentUrl.startsWith("https://", ignoreCase = true)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // -- HIGH-PERFORMANCE TOP ADDRESS BAR --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SSL Lock indicator
                val lockIcon = if (sslSecured) Icons.Default.Lock else Icons.Default.LockOpen
                val lockColor = if (sslSecured) Color(0xFF059669) else Color(0xFFFF453A) // Emerald Green vs Red
                Icon(
                    imageVector = lockIcon,
                    contentDescription = "Security State",
                    tint = lockColor,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("ssl_indicator")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Editable Input Area
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text("Search or type URL securely...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (inputUrl.isNotBlank()) {
                                var target = viewModel.parseUrlOrSearch(inputUrl)
                                if (httpsOnly && target.startsWith("http://", ignoreCase = true)) {
                                    target = target.replaceFirst("http://", "https://", ignoreCase = true)
                                }
                                viewModel.updateActiveTabUrl(target, target)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("address_bar_input")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Refresh Toggle / Progress Indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF2563EB),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            activeTab?.let {
                                viewModel.updateActiveTabUrl(it.currentUrl, it.pageTitle)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF475569))
                    }
                }
            }
        },
        bottomBar = {
            // -- CORE BOTTOM NAVIGATION PANEL --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.navigateTo(BrowserStateViewModel.Screen.Browser)
                    },
                    modifier = Modifier.testTag("nav_back")
                ) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = {
                        // Forward Navigation
                    }
                ) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Forward", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }

                // Central high-contrast Blue Home action
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2563EB))
                        .clickable {
                            viewModel.updateActiveTabUrl("about:blank", "Secure Dashboard")
                        }
                        .testTag("nav_home"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(22.dp))
                }

                IconButton(
                    onClick = { viewModel.navigateTo(BrowserStateViewModel.Screen.TabManager) },
                    modifier = Modifier.testTag("nav_tabs")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Tab Manager", tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                }

                IconButton(
                    onClick = { viewModel.navigateTo(BrowserStateViewModel.Screen.Settings) },
                    modifier = Modifier.testTag("nav_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab == null || activeTab.currentUrl == "about:blank") {
                // -- BRAVE-STYLE PRIVACY DASHBOARD --
                BravePrivacyDashboard(viewModel = viewModel)
            } else if (activeTab.isSuspendedState) {
                // Sleep Overlay for Hibernated state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tab Hibernated for Efficiency",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To preserve system memory and speed up your active session, this background engine task has been serialized and suspended. Tap below to wake up.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.wakeUpTab(activeTab.tabId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158))
                        ) {
                            Text("Restore Engine Task", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // -- ISOLATED SYSTEM WEBVIEW CONTAINER --
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            onWebViewBound(this)

                            // Apply strict Sandbox parameters
                            configureEngineParameters(settings)
                            settings.javaScriptEnabled = jsEnabled
                            settings.userAgentString = selectedUa

                            // Block third-party cookies if toggled
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, !blockThirdPartyCookies)

                            // Register Bridge for telemetry feedback
                            addJavascriptInterface(
                                FingerprintShieldBridge(
                                    onCanvasFaked = { viewModel.incrementTelemetry(canvas = 1) },
                                    onFingerprintMocked = { type -> viewModel.incrementTelemetry(fingerprint = 1) }
                                ),
                                "FingerprintShield"
                            )

                            webViewClient = HardenedWebViewClient(
                                shieldsEngine = shieldsEngine,
                                onTrackerBlocked = { url -> viewModel.incrementTelemetry(trackers = 1) },
                                onCanvasFaked = { viewModel.incrementTelemetry(canvas = 1) },
                                onFingerprintMocked = { type -> viewModel.incrementTelemetry(fingerprint = 1) }
                            )
                        }
                    },
                    update = { webView ->
                        // Dynamically adjust JavaScript execution policy
                        webView.settings.javaScriptEnabled = jsEnabled
                        webView.settings.userAgentString = selectedUa
                        
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptThirdPartyCookies(webView, !blockThirdPartyCookies)

                        if (activeTab != null) {
                            val oldTabId = webView.tag as? String
                            if (oldTabId != activeTab.tabId) {
                                // 1. Save the previous tab's state before switching
                                if (oldTabId != null) {
                                    viewModel.saveTabState(oldTabId, webView)
                                }
                                
                                // 2. Update tag to current active tab ID
                                webView.tag = activeTab.tabId
                                
                                // 3. Restore serialized state if present, otherwise load url
                                val state = activeTab.serializedEngineState
                                if (state != null) {
                                    val bundle = viewModel.bytesToBundle(state)
                                    if (bundle != null) {
                                        webView.restoreState(bundle)
                                    } else {
                                        webView.loadUrl(activeTab.currentUrl)
                                    }
                                } else {
                                    webView.loadUrl(activeTab.currentUrl)
                                }
                            } else {
                                // If the active tab ID is the same but the URL has changed in the viewModel, load it
                                if (webView.url != activeTab.currentUrl && activeTab.currentUrl.isNotEmpty()) {
                                    webView.loadUrl(activeTab.currentUrl)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun BravePrivacyDashboard(viewModel: BrowserStateViewModel) {
    val telemetry by viewModel.shieldTelemetry.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    val quickLaunchTiles = listOf(
        "DuckDuckGo" to "https://duckduckgo.com",
        "Brave Search" to "https://search.brave.com",
        "Proton Mail" to "https://proton.me",
        "Signal Sec" to "https://signal.org",
        "Wikipedia" to "https://wikipedia.org",
        "Tor Project" to "https://torproject.org"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)) // Custom deep dark midnight background
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Brand Shield
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF10B981), // Emerald 500
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "SECURE BROWSER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "HARDENED SANDBOX SHELL ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981), // Emerald 500
                    letterSpacing = 1.sp
                )
            }
        }

        // Blocking Statistics: Dark Theme Grid Style
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Trackers
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Slate 800
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (telemetry?.trackersBlockedGlobal ?: 0L).toString(),
                            color = Color(0xFF10B981), // Emerald Green
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Trackers Dropped",
                            color = Color(0xFF94A3B8), // Slate 400
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Card 2: Canvas Fakes
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (telemetry?.canvasFakesTriggered ?: 0L).toString(),
                            color = Color(0xFF3B82F6), // Blue 500
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Canvas Fakes",
                            color = Color(0xFF94A3B8), // Slate 400
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Card 3: Fingerprints Distorted
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (telemetry?.fingerprintMocksTriggered ?: 0L).toString(),
                            color = Color(0xFF818CF8), // Light Indigo
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fingerprints",
                            color = Color(0xFF94A3B8), // Slate 400
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Quick Launch Grid
        item {
            Column {
                Text(
                    text = "SECURE QUICK LAUNCH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8), // Slate 400
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(118.dp)
                ) {
                    items(quickLaunchTiles) { (title, url) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                                .clickable { viewModel.updateActiveTabUrl(url, title) }
                                .height(52.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Private Bookmarks
        if (bookmarks.isNotEmpty()) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SECURE BOOKMARKS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8), // Slate 400
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    bookmarks.take(4).forEach { bmk ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                                .clickable { viewModel.updateActiveTabUrl(bmk.url, bmk.title) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bmk.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(bmk.url, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { viewModel.deleteBookmark(bmk.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Bookmark", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Sessions History
        if (history.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECENT SESSION LOGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF94A3B8), // Slate 400
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Clear History",
                            color = Color(0xFFFF453A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    history.take(4).forEach { hist ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(width = 1.dp, color = Color(0xFF334155), shape = RoundedCornerShape(12.dp))
                                .clickable { viewModel.updateActiveTabUrl(hist.url, hist.title) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(hist.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(hist.url, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
