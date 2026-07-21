package com.securephoneapps.securebrowser.engine

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel

class WebViewManager(private val context: Context) {
    private val pool = mutableMapOf<String, WebView>()
    
    fun getOrCreateWebView(tabId: String, viewModel: BrowserStateViewModel): WebView {
        return pool.getOrPut(tabId) {
            createWebView(viewModel)
        }
    }

    fun updateSettings(viewModel: BrowserStateViewModel) {
        pool.values.forEach { webView ->
            webView.settings.apply {
                javaScriptEnabled = viewModel.javascriptEnabled.value
                userAgentString = viewModel.selectedUserAgent.value
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !viewModel.blockThirdPartyCookies.value)
        }
    }

    private fun createWebView(viewModel: BrowserStateViewModel): WebView {
        return WebView(context).apply {
            settings.apply {
                // Security Hardening
                javaScriptEnabled = viewModel.javascriptEnabled.value
                domStorageEnabled = true
                databaseEnabled = false
                setSupportMultipleWindows(true)
                allowFileAccess = false
                allowContentAccess = false
                
                // Privacy Hardening
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                
                // Performance
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Fingerprinting Resistance
                userAgentString = viewModel.selectedUserAgent.value
            }
            
            webViewClient = HardenedWebViewClient(
                shieldsEngine = ShieldsCoreEngine(),
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
            
            // Clear state for privacy
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !viewModel.blockThirdPartyCookies.value)
        }
    }

    fun destroyWebView(webView: WebView) {
        webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
    }
}
