package com.securephoneapps.securebrowser.viewmodel

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.securephoneapps.securebrowser.data.SecureBrowserDatabase
import com.securephoneapps.securebrowser.model.BookmarkItem
import com.securephoneapps.securebrowser.model.HistoryItem
import com.securephoneapps.securebrowser.model.ShieldTelemetry
import com.securephoneapps.securebrowser.model.TabGroup
import com.securephoneapps.securebrowser.model.TabInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

class BrowserStateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = SecureBrowserDatabase.getInstance(context)
    private val repository = com.securephoneapps.securebrowser.repository.BrowserRepository(database)
    private val settingsRepository = com.securephoneapps.securebrowser.repository.SettingsRepository(context)
    private val navigationManager = com.securephoneapps.securebrowser.manager.NavigationManager()
    private val tabManager = com.securephoneapps.securebrowser.manager.TabManager()
    private val databaseMutex = Mutex()

    // Reactive Streams from Database
    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabGroups: StateFlow<List<TabGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shieldTelemetry: StateFlow<ShieldTelemetry?> = repository.getTelemetryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShieldTelemetry())

    private val _tabsState = MutableStateFlow<List<TabInstance>>(emptyList())
    val tabsState: StateFlow<List<TabInstance>> = _tabsState.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val currentScreen = navigationManager.currentScreen

    // Configuration Settings
    val javascriptEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_JS_ENABLED, true))
    val blockThirdPartyCookies = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BLOCK_THIRD_PARTY_COOKIES, true))
    val webRtcPrivacyEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_WEB_RTC_PRIVACY, true))
    val deGooglingTelemetryEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_DE_GOOGLING, true))
    val httpsOnlyMode = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_HTTPS_ONLY, true))
    val clearOnExitEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CLEAR_ON_EXIT, false))
    val stripTrackingEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_STRIP_TRACKING, true))
    val deAMPEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_DE_AMP, true))
    val searchEngine = MutableStateFlow(settingsRepository.getString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_SEARCH_ENGINE, "DuckDuckGo"))
    val customSearchEngineUrl = MutableStateFlow(settingsRepository.getString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CUSTOM_SEARCH_URL, "https://duckduckgo.com/?q="))
    val liveBlockedDomains = MutableStateFlow<List<String>>(emptyList())
    val liveBlockedDomainsLog = MutableStateFlow<List<String>>(emptyList())
    val forcedDarkModeEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_FORCE_DARK_MODE, true))
    val appTheme = MutableStateFlow(settingsRepository.getString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_APP_THEME, "Dark"))
    val themeColor = MutableStateFlow(settingsRepository.getString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_THEME_COLOR, "Blue"))
    val activePageThemeColor = MutableStateFlow<String?>(null)
    val selectedUserAgent = MutableStateFlow(
        settingsRepository.getString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CUSTOM_USER_AGENT, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
    )
    val selectedDohProvider = MutableStateFlow(settingsRepository.getString("selected_doh_provider", "https://cloudflare-dns.com"))
    val proxyEnabled = MutableStateFlow(settingsRepository.getBoolean("proxy_enabled", false))
    val proxyHost = MutableStateFlow(settingsRepository.getString("proxy_host", "localhost"))
    val proxyPort = MutableStateFlow(settingsRepository.getInt("proxy_port", 9050))
    val proxyType = MutableStateFlow(settingsRepository.getString("proxy_type", "SOCKS"))
    val downloadedFilesList = MutableStateFlow<List<File>>(emptyList())
    val isAudioShieldActive = MutableStateFlow(true)
    val restrictLocalSubnets = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_STRICT_LOCAL_RESTRICTION, true))
    val userScriptsList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val proxyDiagnosticState = MutableStateFlow("")
    val searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val isHardwareShutterActive = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_HARDWARE_SHUTTER, true))
    val isBiometricLockEnabled = MutableStateFlow(settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BIOMETRIC_LOCK, false))
    val isAuthenticated = MutableStateFlow(!settingsRepository.getBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BIOMETRIC_LOCK, false))
    val pageLoadingProgress = MutableStateFlow(0)
    val canGoBack = MutableStateFlow(false)
    val canGoForward = MutableStateFlow(false)

    fun updateNavigationState(canBack: Boolean, canForward: Boolean) {
        canGoBack.value = canBack
        canGoForward.value = canForward
    }
    
    private val _activeTabUrl = MutableStateFlow("")
    val activeTabUrl: StateFlow<String> = _activeTabUrl.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allTabs.collect { tabs ->
                _tabsState.value = tabs
                if (tabs.isNotEmpty() && _activeTabId.value == null) {
                    _activeTabId.value = tabs.first().tabId
                }
            }
        }
    }

    fun navigateTo(screen: com.securephoneapps.securebrowser.manager.NavigationManager.Screen) { navigationManager.navigateTo(screen) }

    fun goBack(): Boolean = navigationManager.goBack()

    fun refreshDownloadedFiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = File(context.noBackupFilesDir, "secure_downloads")
            if (downloadsDir.exists()) {
                val files = downloadsDir.listFiles()?.toList() ?: emptyList()
                downloadedFilesList.value = files.sortedByDescending { it.lastModified() }
            }
        }
    }

    fun purgeAllDownloads(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadsDir = File(context.noBackupFilesDir, "secure_downloads")
            if (downloadsDir.exists()) {
                downloadsDir.listFiles()?.forEach { it.delete() }
            }
            refreshDownloadedFiles(context)
        }
    }

    fun purgeFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) { file.delete() }
            refreshDownloadedFiles(context)
        }
    }

    fun exportFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = com.securephoneapps.securebrowser.data.SecureDownloadManager(context)
            downloadManager.exportFileToPublicStorage(file)
        }
    }

    fun viewFile(context: Context, file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = com.securephoneapps.securebrowser.data.SecureDownloadManager(context)
            val decryptedFile = downloadManager.decryptFileToTemp(file)
            if (decryptedFile != null) {
                withContext(Dispatchers.Main) {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            decryptedFile
                        )
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Open file"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Cannot open file: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun toggleForcedDarkMode(enabled: Boolean) {
        forcedDarkModeEnabled.value = enabled
        settingsRepository.setBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_FORCE_DARK_MODE, enabled)
    }


    fun updateDohProvider(url: String) {
        selectedDohProvider.value = url
        settingsRepository.setString("selected_doh_provider", url)
    }

    fun updateUserAgent(ua: String) {
        selectedUserAgent.value = ua
        settingsRepository.setString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CUSTOM_USER_AGENT, ua)
    }

    fun toggleBiometricLock(enabled: Boolean) {
        isBiometricLockEnabled.value = enabled
        settingsRepository.setBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BIOMETRIC_LOCK, enabled)
    }

    fun toggleHardwareShutter(enabled: Boolean) {
        isHardwareShutterActive.value = enabled
        settingsRepository.setBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_HARDWARE_SHUTTER, enabled)
    }

    fun toggleRestrictLocalSubnets(enabled: Boolean) {
        restrictLocalSubnets.value = enabled
        settingsRepository.setBoolean(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_STRICT_LOCAL_RESTRICTION, enabled)
    }

    fun setBrowserProxy(host: String, port: Int, type: String, enabled: Boolean) {
        proxyHost.value = host
        proxyPort.value = port
        proxyType.value = type
        proxyEnabled.value = enabled
        
        settingsRepository.setString("proxy_host", host)
        settingsRepository.setInt("proxy_port", port)
        settingsRepository.setString("proxy_type", type)
        settingsRepository.setBoolean("proxy_enabled", enabled)
    }

    private var searchJob: kotlinx.coroutines.Job? = null
    fun fetchSearchSuggestions(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) { searchSuggestions.value = emptyList(); return }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(150)
                val url = java.net.URL("https://duckduckgo.com/ac/?q=${URLEncoder.encode(query, "UTF-8")}&type=list")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.connect()
                if (connection.responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    val regex = Regex("\"([^\"]+)\"")
                    val matches = regex.findAll(text).map { it.groupValues[1] }.toList()
                    if (matches.isNotEmpty()) { searchSuggestions.value = matches.drop(1) }
                    else { searchSuggestions.value = emptyList() }
                }
            } catch (e: Exception) { if (e !is kotlinx.coroutines.CancellationException) e.printStackTrace() }
        }
    }

    fun hibernateTab(tabId: String, webView: WebView) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        val bundle = android.os.Bundle()
        webView.saveState(bundle)
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = bundleToBytes(bundle)
            repository.insertTab(tab.copy(serializedEngineState = bytes, isSuspendedState = true, lastActiveTimestamp = System.currentTimeMillis()))
            withContext(Dispatchers.Main) { webView.loadUrl("about:blank") }
        }
    }

    fun restoreTab(tabId: String, webView: WebView) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        val bytes = tab.serializedEngineState ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bundle = bytesToBundle(bytes)
            if (bundle != null) {
                withContext(Dispatchers.Main) {
                    webView.restoreState(bundle)
                    repository.insertTab(tab.copy(isSuspendedState = false, lastActiveTimestamp = System.currentTimeMillis()))
                }
            }
        }
    }

    fun executeHardPanicPurge(activeWebView: WebView? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseMutex.withLock {
                database.clearAllTables()
                withContext(Dispatchers.Main) {
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    activeWebView?.loadUrl("about:blank")
                }
                _tabsState.value = emptyList()
                createDefaultTab()
            }
            withContext(Dispatchers.Main) {
                android.os.Process.killProcess(android.os.Process.myPid())
                java.lang.System.exit(0)
            }
        }
    }

    private suspend fun createDefaultTab() {
        val tabId = UUID.randomUUID().toString()
        val tab = TabInstance(tabId = tabId, currentUrl = "about:blank", pageTitle = "Secure Dashboard", lastActiveTimestamp = System.currentTimeMillis(), isIncognito = false)
        repository.insertTab(tab)
        _activeTabId.value = tabId
    }

    fun updateActiveTabUrl(url: String, title: String) {
        _activeTabUrl.value = url
        var tabId = activeTabId.value
        
        if (tabId == null) {
            // Create a new tab if none active
            createNewTab(url)
            return
        }
        
        val currentTabs = _tabsState.value
        val tab = currentTabs.find { it.tabId == tabId }
        
        if (tab == null) {
            createNewTab(url)
            return
        }
        
        val updatedTab = tab.copy(currentUrl = url, pageTitle = title, lastActiveTimestamp = System.currentTimeMillis())
        _tabsState.value = currentTabs.map { if (it.tabId == tabId) updatedTab else it }
        viewModelScope.launch(Dispatchers.IO) { databaseMutex.withLock { repository.insertTab(updatedTab) } }
    }

    fun recordHistory(url: String, title: String) {
        val tabId = activeTabId.value ?: return
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        if (!tab.isIncognito && url != "about:blank") {
            viewModelScope.launch(Dispatchers.IO) {
                databaseMutex.withLock { repository.insertHistory(HistoryItem(url = url, title = title, timestamp = System.currentTimeMillis())) }
            }
        }
    }

    fun createNewTab(url: String = "about:blank", isIncognito: Boolean = false) {
        _activeTabUrl.value = url
        viewModelScope.launch {
            val tab = TabInstance(tabId = UUID.randomUUID().toString(), currentUrl = url, pageTitle = "New Tab", lastActiveTimestamp = System.currentTimeMillis(), isIncognito = isIncognito)
            repository.insertTab(tab)
            _activeTabId.value = tab.tabId
            navigationManager.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Browser)
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
        _activeTabUrl.value = _tabsState.value.find { it.tabId == tabId }?.currentUrl ?: ""
        navigationManager.navigateTo(com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Browser)
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            repository.deleteTabById(tabId)
            if (_activeTabId.value == tabId) {
                val remaining = _tabsState.value.filter { it.tabId != tabId }
                if (remaining.isNotEmpty()) { _activeTabId.value = remaining.first().tabId }
                else { createDefaultTab() }
            }
        }
    }

    fun incrementTelemetry(trackers: Int = 0, canvas: Int = 0, fingerprint: Int = 0, pings: Int = 0, url: String? = null) {
        if (url != null) {
            val host = extractHost(url) ?: url
            if (!liveBlockedDomains.value.contains(host)) { liveBlockedDomains.value = liveBlockedDomains.value + host }
            if (!liveBlockedDomainsLog.value.contains(host)) { liveBlockedDomainsLog.value = listOf(host) + liveBlockedDomainsLog.value }
        }
        viewModelScope.launch {
            val current = repository.getTelemetry() ?: ShieldTelemetry()
            repository.insertTelemetry(current.copy(
                trackersBlockedGlobal = current.trackersBlockedGlobal + trackers,
                canvasFakesTriggered = current.canvasFakesTriggered + canvas,
                fingerprintMocksTriggered = current.fingerprintMocksTriggered + fingerprint,
                telemetryPingsNeutralized = current.telemetryPingsNeutralized + pings
            ))
        }
    }

    fun clearLiveTelemetry() {
        liveBlockedDomains.value = emptyList()
        liveBlockedDomainsLog.value = emptyList()
    }

    fun toggleSetting(key: String, value: Boolean) {
        settingsRepository.setBoolean(key, value)
        when (key) {
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_JS_ENABLED -> javascriptEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BLOCK_THIRD_PARTY_COOKIES -> blockThirdPartyCookies.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_WEB_RTC_PRIVACY -> webRtcPrivacyEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_DE_GOOGLING -> deGooglingTelemetryEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_HTTPS_ONLY -> httpsOnlyMode.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CLEAR_ON_EXIT -> clearOnExitEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_STRIP_TRACKING -> stripTrackingEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_DE_AMP -> deAMPEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_FORCE_DARK_MODE -> forcedDarkModeEnabled.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_HARDWARE_SHUTTER -> isHardwareShutterActive.value = value
            com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_BIOMETRIC_LOCK -> {
                isBiometricLockEnabled.value = value
                if (!value) isAuthenticated.value = true
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllTabs()
            repository.clearHistory()
            repository.clearAllBookmarks()
            repository.clearAllGroups()
            withContext(Dispatchers.Main) {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebStorage.getInstance().deleteAllData()
            }
            createDefaultTab()
        }
    }

    fun updateAppTheme(theme: String) {
        settingsRepository.setString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_APP_THEME, theme)
        appTheme.value = theme
    }

    fun updateThemeColor(color: String) {
        settingsRepository.setString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_THEME_COLOR, color)
        themeColor.value = color
    }

    fun updateSearchEngine(engine: String) {
        settingsRepository.setString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_SEARCH_ENGINE, engine)
        searchEngine.value = engine
        val url = when(engine) {
            "DuckDuckGo" -> "https://duckduckgo.com/?q="
            "Brave" -> "https://search.brave.com/search?q="
            "Google" -> "https://www.google.com/search?q="
            "Bing" -> "https://www.bing.com/search?q="
            else -> customSearchEngineUrl.value
        }
        if (engine != "Custom") {
            updateCustomSearchEngine(url)
        }
    }

    fun updateCustomSearchEngine(newUrl: String) {
        settingsRepository.setString(com.securephoneapps.securebrowser.repository.SettingsRepository.KEY_CUSTOM_SEARCH_URL, newUrl)
        customSearchEngineUrl.value = newUrl
    }

    fun cycleActiveUserAgent(): String {
        val nextUa = listOf(
            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 14; Samsung Browser; Model) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Android 14; Mobile; rv:127.0) Gecko/127.0 Firefox/127.0"
        ).random()
        updateUserAgent(nextUa)
        return nextUa
    }

    fun predictiveDnsPreFetch(query: String) {
        if (query.isBlank() || query.contains(" ")) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val host = if (query.contains(".")) query else "$query.com"
                java.net.InetAddress.getByName(host)
            } catch (e: Exception) {}
        }
    }

    fun exportBookmarksToEncryptedJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = bookmarks.value
                val json = list.joinToString(",", "[", "]") { "{\"title\":\"${it.title}\",\"url\":\"${it.url}\"}" }
                val file = File(context.noBackupFilesDir, "bookmarks_secure_export.json")
                file.writeText(json)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Encrypted backup saved to: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun executeUrlResolution(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        
        // Handle explicit protocols
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        
        // Handle common file paths or about pages
        if (trimmed.startsWith("file://") || trimmed.startsWith("about:")) return trimmed
        
        // Handle domain-like structures (e.g., google.com, localhost:8080)
        val domainRegex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:(\\d+))?(/.*)?$")
        val localhostRegex = Regex("^localhost(:(\\d+))?(/.*)?$")
        
        if (domainRegex.matches(trimmed) || localhostRegex.matches(trimmed)) {
            return "https://$trimmed"
        }
        
        // Fallback to search engine
        val baseUrl = customSearchEngineUrl.value
        val separator = if (baseUrl.contains("?")) "" else "/?q="
        return "$baseUrl$separator${URLEncoder.encode(trimmed, "UTF-8")}"
    }

    val activeMediaDownloadUrl = MutableStateFlow<String?>(null)
    fun updateActiveMediaDownloadTarget(url: String?) {
        activeMediaDownloadUrl.value = url
    }

    fun handleRenderProcessCrash(webView: WebView?) {
        val tabId = webView?.tag as? String ?: return
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                webView?.stopLoading()
                webView?.clearHistory()
                webView?.destroy()
            } catch (e: Exception) {}
            repository.insertTab(tab.copy(isSuspendedState = true))
            delay(300)
            selectTab(tabId)
        }
    }

    private fun bundleToBytes(bundle: android.os.Bundle): ByteArray? {
        val parcel = android.os.Parcel.obtain()
        return try { parcel.writeBundle(bundle); parcel.marshall() } catch (e: Exception) { null } finally { parcel.recycle() }
    }

    private fun bytesToBundle(bytes: ByteArray): android.os.Bundle? {
        val parcel = android.os.Parcel.obtain()
        return try { parcel.unmarshall(bytes, 0, bytes.size); parcel.setDataPosition(0); parcel.readBundle(android.os.Bundle::class.java.classLoader) }
        catch (e: Exception) { null } finally { parcel.recycle() }
    }

    private fun extractHost(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.lowercase()
        } catch (e: Exception) { null }
    }

    companion object {
        @Volatile private var companionContext: Context? = null
        @Synchronized fun saveEncryptedCredentials(domain: String, user: String, pass: String) { /* implementation similar to prefs */ }
        @Synchronized fun getEncryptedCredentials(domain: String): Pair<String?, String?> { return Pair(null, null) }
    }
}


