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

        // Pre-create WebView Code Cache directories to eliminate E/chromium: opendir missing directory warnings
        try {
            val jsDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val wasmDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!jsDir.exists()) jsDir.mkdirs()
            if (!wasmDir.exists()) wasmDir.mkdirs()
            
            // Clean up decrypted temporary directory on launch for privacy protection
            val decryptedDir = java.io.File(cacheDir, "decrypted")
            if (decryptedDir.exists()) {
                decryptedDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val webViewManager = com.securephoneapps.securebrowser.engine.WebViewManager(this)

        setContent {
            val viewModel: BrowserStateViewModel = viewModel()
            val isAuthenticated by viewModel.isAuthenticated.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val activeTabId by viewModel.activeTabId.collectAsState()
            
            val webView = remember(activeTabId) { 
                activeTabId?.let { webViewManager.getOrCreateWebView(it, viewModel) }
            }
            
            // Re-authentication logic on start
            LaunchedEffect(Unit) {
                if (viewModel.isBiometricLockEnabled.value && !viewModel.isAuthenticated.value) {
                    showBiometricPrompt { viewModel.isAuthenticated.value = true }
                }
            }

            // Sync Settings to Pooled WebViews
            val jsEnabled by viewModel.javascriptEnabled.collectAsState()
            val userAgent by viewModel.selectedUserAgent.collectAsState()
            val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
            
            LaunchedEffect(jsEnabled, userAgent, blockThirdPartyCookies) {
                webViewManager.updateSettings(viewModel)
            }

            LaunchedEffect(webView) {
                if (webView != null) {
                    val currentTabUrl = viewModel.activeTabUrl.value
                    if (currentTabUrl.isNotBlank() && currentTabUrl != "about:blank") {
                        if (webView.url == null || webView.url == "about:blank" || webView.url != currentTabUrl) {
                            webView.loadUrl(currentTabUrl)
                        }
                    }
                }
            }

            LaunchedEffect(webView) {
                if (webView != null) {
                    viewModel.loadUrlEvent.collect { url ->
                        if (url.isNotBlank() && url != "about:blank") {
                            webView.loadUrl(url)
                        }
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
                        com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Browser -> {
                            webView?.let {
                                MainBrowserScreen(viewModel, it)
                            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Settings -> GranularControlSettingsScreen(viewModel)
                        com.securephoneapps.securebrowser.manager.NavigationManager.Screen.Downloads -> SecureDownloadsVaultScreen(viewModel)
                        com.securephoneapps.securebrowser.manager.NavigationManager.Screen.AdvancedTabs -> AdvancedTabManagerScreen(viewModel)
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
