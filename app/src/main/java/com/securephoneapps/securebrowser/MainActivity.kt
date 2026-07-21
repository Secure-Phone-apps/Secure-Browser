package com.securephoneapps.securebrowser

import android.os.Bundle
import android.os.Build
import android.app.role.RoleManager
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
import android.webkit.WebView
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel
import com.securephoneapps.securebrowser.ui.*
import androidx.compose.foundation.background

class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: BrowserStateViewModel = viewModel()
            val isAuthenticated by viewModel.isAuthenticated.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            val sharedWebView = remember { 
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }
                    
                    val downloadManager = com.securephoneapps.securebrowser.data.SecureDownloadManager(context)
                    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner ?: (context as? android.content.ContextWrapper)?.baseContext as? androidx.lifecycle.LifecycleOwner
                    setDownloadListener(
                        downloadManager.SecureContainerDownloadListener(
                            scope = lifecycleOwner?.lifecycleScope ?: kotlinx.coroutines.GlobalScope,
                            viewModel = viewModel,
                            onPdfTriggered = { pdfUrl -> viewModel.updateActiveTabUrl(pdfUrl, "PDF Viewer") }
                        )
                    )

                    webViewClient = com.securephoneapps.securebrowser.engine.HardenedWebViewClient(
                        shieldsEngine = com.securephoneapps.securebrowser.engine.ShieldsCoreEngine(),
                        onTrackerBlocked = { viewModel.incrementTelemetry(trackers = 1, url = it) },
                        onCanvasFaked = { viewModel.incrementTelemetry(canvas = 1) },
                        onFingerprintMocked = { viewModel.incrementTelemetry(fingerprint = 1) },
                        onPageStartedCallback = { url -> viewModel.updateActiveTabUrl(url, "Loading...") },
                        onPageFinishedCallback = { url, title -> 
                            viewModel.updateActiveTabUrl(url, title)
                            viewModel.recordHistory(url, title)
                        },
                        viewModel = viewModel
                    )
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                            if (viewModel.isHardwareShutterActive.value) {
                                request.deny()
                            } else {
                                request.grant(request.resources)
                            }
                        }
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            viewModel.pageLoadingProgress.value = newProgress
                        }
                    }
                }
            }

            // Sync Settings to WebView
            val jsEnabled by viewModel.javascriptEnabled.collectAsState()
            val userAgent by viewModel.selectedUserAgent.collectAsState()
            val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
            val forcedDarkMode by viewModel.forcedDarkModeEnabled.collectAsState()
            
            LaunchedEffect(jsEnabled) {
                sharedWebView.settings.javaScriptEnabled = jsEnabled
            }
            
            LaunchedEffect(userAgent) {
                sharedWebView.settings.userAgentString = userAgent
            }

            LaunchedEffect(blockThirdPartyCookies) {
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(sharedWebView, !blockThirdPartyCookies)
            }

            LaunchedEffect(forcedDarkMode) {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                    androidx.webkit.WebSettingsCompat.setForceDark(sharedWebView.settings, 
                        if (forcedDarkMode) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF)
                }
            }

            val triggerUrlLoad by viewModel.activeTabUrl.collectAsState()
            
            LaunchedEffect(triggerUrlLoad) {
                if (triggerUrlLoad.isNotBlank() && triggerUrlLoad != "about:blank") {
                    if (sharedWebView.url != triggerUrlLoad) {
                        sharedWebView.loadUrl(triggerUrlLoad)
                    }
                }
            }

            val appTheme by viewModel.appTheme.collectAsState()
            val themeColor by viewModel.themeColor.collectAsState()
            
            MaterialTheme(colorScheme = com.securephoneapps.securebrowser.ui.theme.AppThemeProvider.getColorScheme(appTheme, themeColor)) {
                if (!isAuthenticated) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Secure Browser Locked", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = { showBiometricPrompt { viewModel.isAuthenticated.value = true } },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Unlock with Biometrics")
                            }
                        }
                    }
                } else {
                    when (currentScreen) {
                        BrowserStateViewModel.Screen.Browser -> {
                            MainBrowserScreen(viewModel, sharedWebView)
                        }
                        BrowserStateViewModel.Screen.Settings -> GranularControlSettingsScreen(viewModel)
                        BrowserStateViewModel.Screen.Downloads -> SecureDownloadsVaultScreen(viewModel)
                        BrowserStateViewModel.Screen.TabManager -> AdvancedTabManagerScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Unlock")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try { biometricPrompt.authenticate(promptInfo) } catch (e: Exception) { onSuccess() }
    }
}
