package com.securephoneapps.securebrowser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
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
import android.widget.Toast
import android.webkit.DownloadListener
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore

import android.content.Context
import android.print.PrintManager

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private var activeWebView: WebView? = null

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Unlock")
            .setSubtitle("Authenticate to access Secure Browser")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for secure systems without biometrics enabled/set up
            onSuccess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // WEB ENGINE COLD-START WARMUP
        // Instructure a detached background WebView instance to boot immediately.
        // This forces Chromium to cache runtime rendering classes, cutting latency.
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val warmupWebView = WebView(applicationContext)
                warmupWebView.loadUrl("about:blank")
                delay(1000)
                warmupWebView.destroy()
            } catch (e: Exception) {}
        }

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
                val themeColorHex by viewModel.activePageThemeColor.collectAsState()

                // DYNAMIC THEME BAR COLOR MATCHING
                LaunchedEffect(themeColorHex) {
                    if (currentScreen == BrowserStateViewModel.Screen.Browser) {
                        themeColorHex?.let { hex ->
                            try {
                                val colorInt = android.graphics.Color.parseColor(hex)
                                window.statusBarColor = colorInt
                                window.navigationBarColor = colorInt
                            } catch (e: Exception) {}
                        }
                    } else {
                        // Reset to default on other screens
                        window.statusBarColor = android.graphics.Color.WHITE
                        window.navigationBarColor = android.graphics.Color.WHITE
                    }
                }

                val biometricEnabled by viewModel.isBiometricLockEnabled.collectAsState()
                var isAuthenticated by remember { mutableStateOf(!biometricEnabled) }

                if (!isAuthenticated) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Secure Browser Locked",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Biometric authentication required to unlock",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    showBiometricPrompt {
                                        isAuthenticated = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Unlock App", color = Color.White)
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        showBiometricPrompt {
                            isAuthenticated = true
                        }
                    }
                } else {
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
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (activeWebView != null && activeWebView!!.canGoBack()) {
            activeWebView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    fun exportCurrentPageToPdf(webView: WebView, jobName: String) {
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val webView = activeWebView
        if (webView != null) {
            webView.evaluateJavascript(
                "(function() { " +
                "  var videos = document.getElementsByTagName('video'); " +
                "  for (var i = 0; i < videos.length; i++) { " +
                "    if (!videos[i].paused && !videos[i].ended) { return true; } " +
                "  } " +
                "  return false; " +
                "})();"
            ) { result ->
                if (result == "true") {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            val params = android.app.PictureInPictureParams.Builder().build()
                            enterPictureInPictureMode(params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}

fun configureEngineParameters(settings: WebSettings, viewModel: BrowserStateViewModel? = null) {
    settings.apply {
        val currentUA = viewModel?.selectedUserAgent?.value ?: com.securephoneapps.securebrowser.engine.ShieldsCoreEngine().getRandomizedUserAgent()
        userAgentString = currentUA
        
        // FORCED WEB LAYOUT DARK MODE (Vivaldi Style)
        // Checks if global override is enabled or if device is in dark mode
        val forcedDark = viewModel?.forcedDarkModeEnabled?.value ?: true
        if (forcedDark) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }
        
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        databaseEnabled = true
        domStorageEnabled = true
        if (currentUA.contains("Mobile") || currentUA.contains("Android")) {
            // FIXED MOBILE MAPPING: Forces Google to read the screen width natively as a mobile frame
            useWideViewPort = false
            loadWithOverviewMode = false
            textZoom = 100 // Prevent scale distortions
        } else {
            // DESKTOP MODE FALLBACK: Forces full width desktop site layouts
            useWideViewPort = true
            loadWithOverviewMode = true
        }
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

        // Disable WebRTC via hidden/internal settings reflection or feature flags
        try {
            val methods = settings.javaClass.methods
            for (method in methods) {
                if (method.name.lowercase().contains("webrtc") || method.name.lowercase().contains("peerconnection")) {
                    if (method.parameterTypes.size == 1 && method.parameterTypes[0] == Boolean::class.java) {
                        method.isAccessible = true
                        method.invoke(settings, false)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback for standard SDK targets
        }
    }
}

class SecureContainerDownloadListener(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    private val onPdfTriggered: (String) -> Unit
) : DownloadListener {

    private val KEY_ALIAS = "secure_browser_download_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url == null) return
        
        // INTERCEPT PDF TO LOAD SECURE INLINE VIEWER (pdf.js)
        if (url.endsWith(".pdf", ignoreCase = true) || mimetype?.contains("pdf", ignoreCase = true) == true || contentDisposition?.contains(".pdf", ignoreCase = true) == true) {
            scope.launch(Dispatchers.Main) {
                try {
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val pdfJsUrl = "file:///android_asset/pdfjs/web/viewer.html?file=$encodedUrl"
                    onPdfTriggered(pdfJsUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }
        
        Toast.makeText(context, "Secure Download Started...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val uri = URL(url)
                    val connection = uri.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val fileName = getFileName(url, contentDisposition, mimetype)
                        val downloadsDir = File(context.filesDir, "secure_downloads")
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }
                        val outputFile = File(downloadsDir, fileName)
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }

                        val secretKey = getOrCreateSecretKey()
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                        val iv = cipher.iv // 12-byte IV for GCM

                        java.io.FileOutputStream(outputFile).use { fos ->
                            // Write IV first so we can retrieve it for decryption
                            fos.write(iv)
                            
                            val cipherOutputStream = CipherOutputStream(fos, cipher)
                            cipherOutputStream.use { cos ->
                                connection.inputStream.use { input ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead = input.read(buffer)
                                    while (bytesRead != -1) {
                                        cos.write(buffer, 0, bytesRead)
                                        bytesRead = input.read(buffer)
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Downloaded securely to isolated storage: $fileName", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Secure Download failed: Server returned ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Secure Download error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        var filename = ""
        if (contentDisposition != null) {
            val index = contentDisposition.indexOf("filename=")
            if (index != -1) {
                filename = contentDisposition.substring(index + 9).trim()
                if (filename.startsWith("\"") && filename.endsWith("\"")) {
                    filename = filename.substring(1, filename.length - 1)
                }
            }
        }
        if (filename.isEmpty()) {
            val lastSlash = url.lastIndexOf('/')
            filename = if (lastSlash != -1) {
                url.substring(lastSlash + 1)
            } else {
                "download_file"
            }
            val questionMark = filename.indexOf('?')
            if (questionMark != -1) {
                filename = filename.substring(0, questionMark)
            }
        }
        return filename.ifEmpty { "downloaded_asset" }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWorkspaceScreen(
    viewModel: BrowserStateViewModel,
    onWebViewBound: (WebView) -> Unit
) {
    val context = LocalContext.current
    val tabs by viewModel.tabsState.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.tabId == activeTabId }

    val jsEnabled by viewModel.javascriptEnabled.collectAsState()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
    val selectedUa by viewModel.selectedUserAgent.collectAsState()
    val httpsOnly by viewModel.httpsOnlyMode.collectAsState()
    val liveBlockedDomains by viewModel.liveBlockedDomains.collectAsState()

    var userTypedInput by remember { mutableStateOf("") }
    var activeLoadingUrl by remember { mutableStateOf("") }
    var lastLoadedUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sslSecured by remember { mutableStateOf(true) }
    var showBlockedBottomSheet by remember { mutableStateOf(false) }

    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showLinkContextMenu by remember { mutableStateOf(false) }
    var longPressedUrl by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val shieldsEngine = remember { ShieldsCoreEngine() }
    val coroutineScope = rememberCoroutineScope()

    var triggerUrlLoad by remember { mutableStateOf("") }

    val sharedWebView = remember {
        WebView(context).apply {
            onWebViewBound(this)
            webViewInstance = this

            // Encrypted, isolated download interceptor pipeline
            setDownloadListener(SecureContainerDownloadListener(context, coroutineScope) { pdfUrl ->
                triggerUrlLoad = pdfUrl
            })

            // Apply strict Sandbox parameters
            configureEngineParameters(settings, viewModel)
            settings.javaScriptEnabled = jsEnabled
            settings.userAgentString = if (selectedUa.contains("Mozilla/5.0 (Linux; Android 10; K)") || selectedUa.isBlank()) {
                shieldsEngine.getRandomizedUserAgent()
            } else {
                selectedUa
            }

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

            setOnLongClickListener { v ->
                val result = (v as WebView).hitTestResult
                if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || 
                    result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    val url = result.extra
                    if (!url.isNullOrBlank()) {
                        longPressedUrl = url
                        showLinkContextMenu = true
                    }
                    true
                } else {
                    false
                }
            }

            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    view?.evaluateJavascript("(function() { return document.querySelector('meta[name=\"theme-color\"]')?.content; })();") { color ->
                        if (color != null && color != "null") {
                            viewModel.activePageThemeColor.value = color.replace("\"", "")
                        }
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: android.graphics.Bitmap?) {
                    super.onReceivedIcon(view, icon)
                }

                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                    val isShutterEnabled = viewModel.isHardwareShutterActive.value
                    if (isShutterEnabled && request != null) {
                        val resources = request.resources
                        val hasCameraOrMic = resources.any { res ->
                            res == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                            res == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE
                        }
                        if (hasCameraOrMic) {
                            request.deny()
                            return
                        }
                    }
                    super.onPermissionRequest(request)
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    viewModel.pageLoadingProgress.value = newProgress
                }
            }
        }
    }

    // MEMORY LEAK DETACHMENT
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.detachAndCleanupWebView(webViewInstance)
            webViewInstance = null
        }
    }

    LaunchedEffect(triggerUrlLoad) {
        if (triggerUrlLoad.isNotBlank()) {
            sharedWebView.loadUrl(triggerUrlLoad)
        }
    }

    LaunchedEffect(jsEnabled) {
        sharedWebView.settings.javaScriptEnabled = jsEnabled
    }

    LaunchedEffect(selectedUa) {
        sharedWebView.settings.userAgentString = if (selectedUa.contains("Mozilla/5.0 (Linux; Android 10; K)") || selectedUa.isBlank()) {
            shieldsEngine.getRandomizedUserAgent()
        } else {
            selectedUa
        }
    }

    LaunchedEffect(blockThirdPartyCookies) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(sharedWebView, !blockThirdPartyCookies)
    }

    // Synchronize Input Field and load state strictly when active tab or URL switches
    LaunchedEffect(activeTab?.tabId, activeTab?.currentUrl) {
        if (activeTab != null) {
            val current = activeTab.currentUrl
            
            val cleanInput = userTypedInput.trim().trimEnd('/')
            val cleanCurrent = current.trim().trimEnd('/')
            if (cleanInput.isBlank() || (
                !cleanCurrent.equals(cleanInput, ignoreCase = true) && 
                !cleanCurrent.contains(cleanInput, ignoreCase = true) && 
                !cleanInput.contains(cleanCurrent, ignoreCase = true)
            )) {
                userTypedInput = if (current == "about:blank") "" else current
            }
            activeLoadingUrl = current

            val oldTabId = sharedWebView.tag as? String
            if (oldTabId != activeTab.tabId) {
                if (oldTabId != null) {
                    viewModel.saveTabState(oldTabId, sharedWebView)
                }
                sharedWebView.tag = activeTab.tabId
                val state = activeTab.serializedEngineState
                if (state != null) {
                    val bundle = viewModel.bytesToBundle(state)
                    if (bundle != null) {
                        sharedWebView.restoreState(bundle)
                    } else {
                        triggerUrlLoad = current
                    }
                } else {
                    triggerUrlLoad = current
                }
            } else {
                fun urlsMatch(url1: String?, url2: String?): Boolean {
                    if (url1 == url2) return true
                    if (url1 == null || url2 == null) return false
                    val u1 = url1.trim().trimEnd('/')
                    val u2 = url2.trim().trimEnd('/')
                    return u1.equals(u2, ignoreCase = true)
                }
                if (!urlsMatch(sharedWebView.url, current) && current.isNotEmpty()) {
                    triggerUrlLoad = current
                }
            }
        }
    }

    LaunchedEffect(activeLoadingUrl) {
        sslSecured = activeLoadingUrl.startsWith("https://", ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
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

                if (liveBlockedDomains.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                            .clickable { showBlockedBottomSheet = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("blocked_trackers_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Blocked Trackers",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${liveBlockedDomains.size}",
                                color = Color(0xFF2563EB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Editable Input Area
                OutlinedTextField(
                    value = userTypedInput,
                    onValueChange = { userTypedInput = it },
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
                            if (userTypedInput.isNotBlank()) {
                                var target = viewModel.executeUrlResolution(userTypedInput)
                                target = shieldsEngine.stripTrackingParameters(target)
                                if (httpsOnly && target.startsWith("http://", ignoreCase = true)) {
                                    target = target.replaceFirst("http://", "https://", ignoreCase = true)
                                }
                                viewModel.clearLiveTelemetry()
                                activeLoadingUrl = target
                                viewModel.updateActiveTabUrl(target, target)
                                triggerUrlLoad = target
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

            // Ensure the linear Jetpack Compose progress indicator bar only recomposes when progress is between 1 and 99, and completely sets visibility to gone when it reaches 100 to save rendering cycles.
            val progress by viewModel.pageLoadingProgress.collectAsState()
            if (progress in 1..99) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color(0xFF2563EB),
                    trackColor = Color(0xFFE2E8F0)
                )
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
                        if (canGoBack) {
                            webViewInstance?.goBack()
                        }
                    },
                    enabled = canGoBack,
                    modifier = Modifier.testTag("nav_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = if (canGoBack) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (canGoForward) {
                            webViewInstance?.goForward()
                        }
                    },
                    enabled = canGoForward,
                    modifier = Modifier.testTag("nav_forward")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Forward",
                        tint = if (canGoForward) Color(0xFF475569) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
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

                // SESSION PANIC TERMINATION ACTION
                IconButton(
                    onClick = {
                        viewModel.executeHardPanicPurge()
                        // Completely drop the OS application process matrix instantly
                        android.os.Process.killProcess(android.os.Process.myPid())
                        System.exit(0)
                    },
                    modifier = Modifier.testTag("nav_panic")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Panic Purge", tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showLinkContextMenu && longPressedUrl != null) {
                AlertDialog(
                    onDismissRequest = {
                        showLinkContextMenu = false
                        longPressedUrl = null
                    },
                    title = {
                        Text(
                            text = "Link Options",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = longPressedUrl!!,
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Option 1: Open in New Tab
                            Button(
                                onClick = {
                                    viewModel.createNewTab(url = longPressedUrl!!)
                                    showLinkContextMenu = false
                                    longPressedUrl = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("Open in New Tab", color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Option 2: Open in New Tab Group
                            Button(
                                onClick = {
                                    viewModel.createNewTabInGroup(url = longPressedUrl!!)
                                    showLinkContextMenu = false
                                    longPressedUrl = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text("Open in New Tab Group", color = Color.White)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showLinkContextMenu = false
                                longPressedUrl = null
                            }
                        ) {
                            Text("Cancel", color = Color(0xFF64748B))
                        }
                    },
                    containerColor = Color.White
                )
            }

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
                    factory = {
                        webViewInstance = sharedWebView
                        sharedWebView.webViewClient = HardenedWebViewClient(
                            shieldsEngine = shieldsEngine,
                            onTrackerBlocked = { url -> viewModel.incrementTelemetry(trackers = 1, url = url) },
                            onCanvasFaked = { viewModel.incrementTelemetry(canvas = 1) },
                            onFingerprintMocked = { type -> viewModel.incrementTelemetry(fingerprint = 1) },
                            onPageStartedCallback = { loadedUrl ->
                                isLoading = true
                                canGoBack = sharedWebView.canGoBack()
                                canGoForward = sharedWebView.canGoForward()
                                viewModel.clearLiveTelemetry()
                                viewModel.activePageThemeColor.value = null
                                
                                lastLoadedUrl = loadedUrl
                                activeLoadingUrl = loadedUrl
                            },
                            onPageFinishedCallback = { url, title ->
                                isLoading = false
                                canGoBack = sharedWebView.canGoBack()
                                canGoForward = sharedWebView.canGoForward()
                                
                                // Handle internal navigation state syncing
                                viewModel.updateActiveTabUrl(url, title)
                            },
                            isAudioShieldActive = { viewModel.isAudioShieldActive.value }
                        )
                        sharedWebView
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { /* Leave this entirely EMPTY to prevent recomposition reloads */ }
                )
            }

            // --- EXPANDABLE LIVE BLOCKER DIAGNOSTICS BOTTOM SHEET ---
            if (showBlockedBottomSheet) {
                // Semi-transparent scrim background overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showBlockedBottomSheet = false }
                )

                // Sliding card container anchored to the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(bottom = 24.dp)
                        .testTag("blocked_trackers_bottom_sheet")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header handle bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFCBD5E1))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Header details row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Live Shield Diagnostics",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            IconButton(
                                onClick = { showBlockedBottomSheet = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Panel",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "The secure browser core has dynamically intercepted and neutralized the following tracking and telemetry payloads on this domain:",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (liveBlockedDomains.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No trackers detected on active webpage",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            ) {
                                items(liveBlockedDomains) { domain ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(Color(0xFFF8FAFC))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Blocked Tracker",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = domain,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
