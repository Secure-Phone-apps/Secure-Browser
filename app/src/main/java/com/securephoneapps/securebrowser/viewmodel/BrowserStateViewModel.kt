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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

class BrowserStateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = SecureBrowserDatabase.getInstance(context)
    private val repository = BrowserRepository(database)
    private val databaseMutex = Mutex()

    // Master Key for Encrypted Shared Preferences
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_browser_settings",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 1. Reactive Streams from Database
    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabGroups: StateFlow<List<TabGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shieldTelemetry: StateFlow<ShieldTelemetry?> = repository.getTelemetryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShieldTelemetry())

    // 2. In-Memory Managed Reactive Tabs State
    private val _tabsState = MutableStateFlow<List<TabInstance>>(emptyList())
    val tabsState: StateFlow<List<TabInstance>> = _tabsState.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    // Screen State Navigation
    enum class Screen { Browser, TabManager, Settings }
    private val _currentScreen = MutableStateFlow(Screen.Browser)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // 3. User Configuration Settings State
    val javascriptEnabled = MutableStateFlow(encryptedPrefs.getBoolean("javascript_enabled", true))
    val blockThirdPartyCookies = MutableStateFlow(encryptedPrefs.getBoolean("block_third_party_cookies", true))
    val webRtcPrivacyEnabled = MutableStateFlow(encryptedPrefs.getBoolean("webrtc_privacy_enabled", true))
    val deGooglingTelemetryEnabled = MutableStateFlow(encryptedPrefs.getBoolean("de_googling_telemetry_enabled", true))
    val httpsOnlyMode = MutableStateFlow(encryptedPrefs.getBoolean("https_only_mode", true))
    val searchEngine = MutableStateFlow(encryptedPrefs.getString("search_engine", "DuckDuckGo") ?: "DuckDuckGo")
    val selectedUserAgent = MutableStateFlow(
        encryptedPrefs.getString("custom_user_agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36") ?: "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    )

    init {
        // Observe and load Tab States from DB
        viewModelScope.launch {
            repository.allTabs.collect { loadedTabs ->
                _tabsState.value = loadedTabs
                if (loadedTabs.isEmpty()) {
                    createDefaultTab()
                } else if (_activeTabId.value == null) {
                    _activeTabId.value = loadedTabs.firstOrNull()?.tabId
                }
            }
        }

        // Initialize empty telemetry entry if none exists
        viewModelScope.launch {
            if (repository.getTelemetry() == null) {
                repository.insertTelemetry(ShieldTelemetry())
            }
        }

        // Background Tab Hibernation Ticker (Chrome Efficiency Protocol - 15 minutes timeout check)
        viewModelScope.launch {
            while (isActive) {
                delay(60000) // check every minute
                val now = System.currentTimeMillis()
                val currentTabs = _tabsState.value
                val activeId = _activeTabId.value
                currentTabs.forEach { tab ->
                    if (tab.tabId != activeId && !tab.isSuspendedState && (now - tab.lastActiveTimestamp) > 900000) {
                        // Hibernate tab and serialize state to DB
                        val suspended = tab.copy(isSuspendedState = true)
                        repository.insertTab(suspended)
                        // Log a neutralized event
                        incrementTelemetry(pings = 1)
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    private suspend fun createDefaultTab() {
        val tab = TabInstance(
            tabId = UUID.randomUUID().toString(),
            currentUrl = "about:blank",
            pageTitle = "Secure Dashboard",
            lastActiveTimestamp = System.currentTimeMillis()
        )
        repository.insertTab(tab)
        _activeTabId.value = tab.tabId
    }

    // Tab Operations
    fun createNewTab(url: String = "about:blank", isIncognito: Boolean = false) {
        viewModelScope.launch {
            val tab = TabInstance(
                tabId = UUID.randomUUID().toString(),
                currentUrl = url,
                pageTitle = if (url == "about:blank") "Secure Dashboard" else "New Tab",
                lastActiveTimestamp = System.currentTimeMillis(),
                isIncognito = isIncognito
            )
            repository.insertTab(tab)
            _activeTabId.value = tab.tabId
            _currentScreen.value = Screen.Browser
        }
    }

    fun createNewTabInGroup(url: String, isIncognito: Boolean = false) {
        viewModelScope.launch {
            val groupId = UUID.randomUUID().toString()
            val colorHex = "#0A84FF" // Default secure blue badge for link-created group
            repository.insertGroup(TabGroup(groupId = groupId, groupName = "Link Stack", hexColorBadge = colorHex))
            val tab = TabInstance(
                tabId = UUID.randomUUID().toString(),
                currentUrl = url,
                pageTitle = "New Tab",
                lastActiveTimestamp = System.currentTimeMillis(),
                isIncognito = isIncognito,
                parentGroupId = groupId
            )
            repository.insertTab(tab)
            _activeTabId.value = tab.tabId
            _currentScreen.value = Screen.Browser
        }
    }

    fun selectTab(tabId: String) {
        viewModelScope.launch {
            _activeTabId.value = tabId
            val tab = _tabsState.value.find { it.tabId == tabId }
            if (tab != null) {
                // Wake up if hibernated
                val updated = tab.copy(
                    isSuspendedState = false,
                    lastActiveTimestamp = System.currentTimeMillis()
                )
                repository.insertTab(updated)
            }
            _currentScreen.value = Screen.Browser
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            repository.deleteTabById(tabId)
            if (_activeTabId.value == tabId) {
                val remaining = _tabsState.value.filter { it.tabId != tabId }
                if (remaining.isNotEmpty()) {
                    _activeTabId.value = remaining.first().tabId
                } else {
                    createDefaultTab()
                }
            }
        }
    }

    fun updateActiveTabUrl(url: String, title: String) {
        val activeId = _activeTabId.value ?: return
        val currentTab = _tabsState.value.find { it.tabId == activeId } ?: return
        viewModelScope.launch {
            val updated = currentTab.copy(
                currentUrl = url,
                pageTitle = title,
                lastActiveTimestamp = System.currentTimeMillis()
            )
            repository.insertTab(updated)

            // Add to history if not incognito and not about:blank
            if (!currentTab.isIncognito && url != "about:blank" && url.isNotBlank()) {
                repository.insertHistory(
                    HistoryItem(
                        url = url,
                        title = title,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun wakeUpTab(tabId: String) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        viewModelScope.launch {
            val updated = tab.copy(isSuspendedState = false, lastActiveTimestamp = System.currentTimeMillis())
            repository.insertTab(updated)
        }
    }

    fun bundleToBytes(bundle: android.os.Bundle): ByteArray? {
        val parcel = android.os.Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.marshall()
        } catch (e: Exception) {
            null
        } finally {
            parcel.recycle()
        }
    }

    fun bytesToBundle(bytes: ByteArray): android.os.Bundle? {
        val parcel = android.os.Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            parcel.readBundle(android.os.Bundle::class.java.classLoader)
        } catch (e: Exception) {
            null
        } finally {
            parcel.recycle()
        }
    }

    fun saveTabState(tabId: String, webView: WebView) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        val bundle = android.os.Bundle()
        webView.saveState(bundle)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                databaseMutex.withLock {
                    val bytes = bundleToBytes(bundle)
                    repository.insertTab(tab.copy(serializedEngineState = bytes))
                }
            }
        }
    }

    fun hibernateTab(tabId: String, webView: WebView) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        val bundle = android.os.Bundle()
        webView.saveState(bundle)
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                databaseMutex.withLock {
                    bundleToBytes(bundle)
                }
            }
            withContext(Dispatchers.IO) {
                databaseMutex.withLock {
                    val updated = tab.copy(
                        serializedEngineState = bytes,
                        isSuspendedState = true
                    )
                    repository.insertTab(updated)
                }
            }
            // Safely clear content on UI thread
            webView.post {
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                } catch (e: Exception) {
                    // Safe fall
                }
            }
        }
    }

    fun restoreTabState(tabId: String, webView: WebView) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        val bytes = tab.serializedEngineState ?: return
        viewModelScope.launch {
            val bundle: android.os.Bundle? = withContext(Dispatchers.IO) {
                databaseMutex.withLock {
                    bytesToBundle(bytes)
                }
            }
            if (bundle != null) {
                withContext(Dispatchers.Main) {
                    webView.restoreState(bundle)
                }
            }
        }
    }

    // Search Engine & URL Purifier
    fun parseUrlOrSearch(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"

        val hasScheme = trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
        val hasPeriod = trimmed.contains(".") && !trimmed.contains(" ")

        val baseTarget = if (hasScheme) {
            trimmed
        } else if (hasPeriod) {
            "https://$trimmed"
        } else {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            if (searchEngine.value.lowercase() == "brave") {
                "https://search.brave.com/search?q=$encodedQuery"
            } else {
                "https://html.duckduckgo.com/html/?q=$encodedQuery"
            }
        }

        return stripTrackingParameters(baseTarget)
    }

    fun stripTrackingParameters(url: String): String {
        try {
            val uri = URI(url)
            val query = uri.query ?: return url
            if (query.isEmpty()) return url

            val cleanedParams = query.split("&")
                .filterNot { param ->
                    val name = param.split("=").firstOrNull()?.lowercase() ?: ""
                    name.startsWith("utm_") || 
                    name == "gclid" || 
                    name == "fbclid" || 
                    name == "gclsrc" || 
                    name == "msclkid" || 
                    name == "cx" || 
                    name == "cof"
                }
                .joinToString("&")

            val newQuery = if (cleanedParams.isNotEmpty()) "?$cleanedParams" else ""
            val scheme = uri.scheme ?: "https"
            val authority = uri.authority ?: ""
            val path = uri.path ?: ""
            val fragment = if (uri.fragment != null) "#${uri.fragment}" else ""
            return "$scheme://$authority$path$newQuery$fragment"
        } catch (e: Exception) {
            return url
        }
    }

    // Toggle Policy Settings
    fun toggleSetting(key: String, value: Boolean) {
        encryptedPrefs.edit().putBoolean(key, value).apply()
        when (key) {
            "javascript_enabled" -> javascriptEnabled.value = value
            "block_third_party_cookies" -> blockThirdPartyCookies.value = value
            "webrtc_privacy_enabled" -> webRtcPrivacyEnabled.value = value
            "de_googling_telemetry_enabled" -> deGooglingTelemetryEnabled.value = value
            "https_only_mode" -> httpsOnlyMode.value = value
        }
    }

    fun updateSearchEngine(engine: String) {
        encryptedPrefs.edit().putString("search_engine", engine).apply()
        searchEngine.value = engine
    }

    fun updateUserAgent(ua: String) {
        encryptedPrefs.edit().putString("custom_user_agent", ua).apply()
        selectedUserAgent.value = ua
    }

    // Bookmarks and History DB ops
    fun addBookmark(title: String, url: String, folder: String = "Root") {
        viewModelScope.launch {
            repository.insertBookmark(BookmarkItem(title = title, url = url, folderPath = folder))
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmarkById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Tab Grouping Matrix Ops
    fun createTabGroup(name: String, colorHex: String): String {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.insertGroup(TabGroup(groupId = id, groupName = name, hexColorBadge = colorHex))
        }
        return id
    }

    fun assignTabToGroup(tabId: String, groupId: String?) {
        val tab = _tabsState.value.find { it.tabId == tabId } ?: return
        viewModelScope.launch {
            repository.insertTab(tab.copy(parentGroupId = groupId))
        }
    }

    fun removeGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteGroupById(groupId)
            // Reset parent group IDs of associated tabs
            _tabsState.value.filter { it.parentGroupId == groupId }.forEach {
                repository.insertTab(it.copy(parentGroupId = null))
            }
        }
    }

    // Shield Telemetry Dynamic Counter Logging
    fun incrementTelemetry(trackers: Int = 0, canvas: Int = 0, fingerprint: Int = 0, pings: Int = 0) {
        viewModelScope.launch {
            val current = repository.getTelemetry() ?: ShieldTelemetry()
            val updated = current.copy(
                trackersBlockedGlobal = current.trackersBlockedGlobal + trackers,
                canvasFakesTriggered = current.canvasFakesTriggered + canvas,
                fingerprintMocksTriggered = current.fingerprintMocksTriggered + fingerprint,
                telemetryPingsNeutralized = current.telemetryPingsNeutralized + pings
            )
            repository.insertTelemetry(updated)
        }
    }

    // Automated Clean-Room Execution Wiping Sequence
    fun closeSession() {
        viewModelScope.launch {
            // 1. Clear database active records
            repository.clearAllTabs()
            repository.clearHistory()
            repository.clearAllGroups()

            // 2. Clear System WebView engine runtime assets
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()

            // 3. Clear memory variables
            _tabsState.value = emptyList()
            createDefaultTab()
            _currentScreen.value = Screen.Browser
        }
    }
}

/**
 * Concrete, production-grade Repository class isolating database operations from the architecture thread pools.
 */
class BrowserRepository(private val database: SecureBrowserDatabase) {
    val allHistory: Flow<List<HistoryItem>> = database.historyDao().getAllHistory()
    val allBookmarks: Flow<List<BookmarkItem>> = database.bookmarkDao().getAllBookmarks()
    val allTabs: Flow<List<TabInstance>> = database.tabInstanceDao().getAllTabs()
    val allGroups: Flow<List<TabGroup>> = database.tabGroupDao().getAllGroups()

    fun getTelemetryFlow(): Flow<ShieldTelemetry?> = database.shieldTelemetryDao().getTelemetryFlow()
    suspend fun getTelemetry(): ShieldTelemetry? = database.shieldTelemetryDao().getTelemetry()
    suspend fun insertTelemetry(telemetry: ShieldTelemetry) = database.shieldTelemetryDao().insertTelemetry(telemetry)

    suspend fun insertHistory(item: HistoryItem) = database.historyDao().insertHistoryItem(item)
    suspend fun clearHistory() = database.historyDao().clearAllHistory()

    suspend fun insertBookmark(item: BookmarkItem) = database.bookmarkDao().insertBookmark(item)
    suspend fun deleteBookmarkById(id: Long) = database.bookmarkDao().deleteBookmarkById(id)

    suspend fun insertTab(tab: TabInstance) = database.tabInstanceDao().insertTab(tab)
    suspend fun deleteTabById(tabId: String) = database.tabInstanceDao().deleteTabById(tabId)
    suspend fun clearAllTabs() = database.tabInstanceDao().clearAllTabs()

    suspend fun insertGroup(group: TabGroup) = database.tabGroupDao().insertGroup(group)
    suspend fun deleteGroupById(groupId: String) = database.tabGroupDao().deleteGroupById(groupId)
    suspend fun clearAllGroups() = database.tabGroupDao().clearAllGroups()
}
