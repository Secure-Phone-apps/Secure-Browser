package com.securephoneapps.securebrowser.engine

import android.graphics.Bitmap
import android.net.http.SslError
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class FingerprintShieldBridge(
    private val onCanvasFaked: () -> Unit,
    private val onFingerprintMocked: (String) -> Unit
) {
    @JavascriptInterface
    fun onCanvasFakeTriggered() {
        onCanvasFaked()
    }

    @JavascriptInterface
    fun onFingerprintMockTriggered(type: String) {
        onFingerprintMocked(type)
    }
}

class HardenedWebViewClient(
    private val shieldsEngine: ShieldsCoreEngine,
    private val onTrackerBlocked: (url: String) -> Unit,
    private val onCanvasFaked: () -> Unit,
    private val onFingerprintMocked: (type: String) -> Unit,
    private val onPageStartedCallback: (url: String) -> Unit = {},
    private val onPageFinishedCallback: (url: String, title: String) -> Unit = { _, _ -> },
    private val isAudioShieldActive: () -> Boolean = { true },
    private val viewModel: com.securephoneapps.securebrowser.viewmodel.BrowserStateViewModel? = null
) : WebViewClient() {

    private val registeredWebViews = mutableSetOf<Int>()
    private var lastHost: String? = null
    private var baseUserAgent: String? = null

    private fun sweepPreviousOrigin(url: String?) {
        url?.let {
            try {
                // Cross-Domain Cleaning Lock: Compare hosts precisely to prevent redundant storage sweeps on same-site redirects
                val currentHost = java.net.URL(it).host
                if (currentHost != null) {
                    val previousHost = lastHost
                    if (previousHost != null && previousHost != currentHost) {
                        // CROSS-TAB ISOLATION PROTOCOL
                        // Trigger a targeted data purge to clear cross-origin local storage variables cleanly.
                        val storage = android.webkit.WebStorage.getInstance()
                        storage.deleteOrigin(previousHost)
                        storage.deleteOrigin("https://$previousHost")
                        storage.deleteOrigin("http://$previousHost")
                    }
                    lastHost = currentHost
                }
            } catch (e: Exception) {
                // Non-critical exception handling for malformed internal URLs
            }
        }
    }

    private fun cleanTrackingParameters(url: String): String {
        try {
            val uri = android.net.Uri.parse(url)
            if (uri.query == null) return url
            val parameterNames = uri.queryParameterNames
            val trackingParams = setOf("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "gclid", "ref", "affiliate", "tracking_id")
            val filteredNames = parameterNames.filter { param ->
                !trackingParams.contains(param.lowercase()) && !param.lowercase().startsWith("utm_")
            }
            if (filteredNames.size == parameterNames.size) return url
            
            val builder = uri.buildUpon().clearQuery()
            for (name in filteredNames) {
                val values = uri.getQueryParameters(name)
                for (value in values) {
                    builder.appendQueryParameter(name, value)
                }
            }
            return builder.build().toString()
        } catch (e: Exception) {
            return url
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        // 1. PDF path validation block
        if (url.contains("viewer.html", ignoreCase = true) || url.contains("pdfjs", ignoreCase = true)) {
            if (!url.startsWith("file:///android_asset/pdfjs/")) {
                return WebResourceResponse("text/plain", "UTF-8", "Blocked: PDF viewer must load strictly from local app resources.".byteInputStream())
            }
        }
        
        // 2. PHISHING & MALWARE FIREWALL INTERCEPTION
        val host = request.url.host ?: ""
        if (isMalicious(host)) {
            return generateSecurityWarningResponse()
        }

        if (shieldsEngine.shouldBlock(url)) {
            onTrackerBlocked(url)
            return shieldsEngine.generateBlankResponse()
        }

        // 3. Privacy Proxy (GET Only to avoid breaking POST forms)
        if (request.method == "GET" && (url.startsWith("http://") || url.startsWith("https://"))) {
            try {
                val requestHeaders = request.requestHeaders?.toMutableMap() ?: mutableMapOf()
                
                // Stripping specific privacy-leaking headers
                val refererKeys = requestHeaders.keys.filter { it.equals("Referer", ignoreCase = true) }
                for (key in refererKeys) {
                    requestHeaders.remove(key)
                }
                requestHeaders["Referer"] = "no-referrer"

                // De-Googling header stripping
                if (viewModel?.deGooglingTelemetryEnabled?.value == true) {
                    val googleSpecificHeaders = listOf(
                        "X-Client-Data", "X-Google-GFE-Backend-Request-Cost",
                        "X-Goog-Encode-Response-If-Executable", "X-Goog-Visitor-Id",
                        "X-Youtube-Client-Name", "X-Youtube-Client-Version"
                    )
                    googleSpecificHeaders.forEach { headerName ->
                        val keys = requestHeaders.keys.filter { it.equals(headerName, ignoreCase = true) }
                        keys.forEach { requestHeaders.remove(it) }
                    }
                }

                // Synchronize User-Agent Client Hints to prevent fingerprint mismatches
                val userAgent = view?.settings?.userAgentString ?: ""
                if (userAgent.isNotEmpty()) {
                    val isMobile = if (userAgent.contains("Mobile", ignoreCase = true) || userAgent.contains("Android", ignoreCase = true) || userAgent.contains("iPhone", ignoreCase = true)) "?1" else "?0"
                    val platform = when {
                        userAgent.contains("Android", ignoreCase = true) -> "\"Android\""
                        userAgent.contains("iPhone", ignoreCase = true) || userAgent.contains("iPad", ignoreCase = true) -> "\"iOS\""
                        userAgent.contains("Windows", ignoreCase = true) -> "\"Windows\""
                        userAgent.contains("Macintosh", ignoreCase = true) -> "\"macOS\""
                        userAgent.contains("Linux", ignoreCase = true) -> "\"Linux\""
                        else -> "\"Android\""
                    }
                    val chromeVersionRegex = "Chrome/([0-9.]+)".toRegex()
                    val chromeMatch = chromeVersionRegex.find(userAgent)
                    val chromeVersion = chromeMatch?.groupValues?.get(1)?.split(".")?.firstOrNull() ?: "120"
                    val secChUa = "\"Not A(Brand\";v=\"99\", \"Chromium\";v=\"$chromeVersion\", \"Google Chrome\";v=\"$chromeVersion\""
                    
                    val keysToRemove = requestHeaders.keys.filter { 
                        it.equals("Sec-CH-UA", ignoreCase = true) || 
                        it.equals("Sec-CH-UA-Mobile", ignoreCase = true) || 
                        it.equals("Sec-CH-UA-Platform", ignoreCase = true) 
                    }
                    for (key in keysToRemove) {
                        requestHeaders.remove(key)
                    }
                    requestHeaders["Sec-CH-UA"] = secChUa
                    requestHeaders["Sec-CH-UA-Mobile"] = isMobile
                    requestHeaders["Sec-CH-UA-Platform"] = platform
                }

                val connectionUrl = java.net.URL(url)
                val connection = connectionUrl.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = request.method
                
                // Cookie Synchronization: ensure the privacy proxy maintains session state
                val cookieManager = android.webkit.CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)
                if (cookies != null) {
                    connection.addRequestProperty("Cookie", cookies)
                }
                
                val obfuscatedHeaders = shieldsEngine.obfuscateHeaderSequence(requestHeaders)
                for ((key, value) in obfuscatedHeaders) {
                    connection.addRequestProperty(key, value)
                }
                
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                connection.connect()
                
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage
                val responseHeaders = mutableMapOf<String, String>()
                for (i in 0..100) {
                    val headerKey = connection.getHeaderFieldKey(i)
                    val headerValue = connection.getHeaderField(i)
                    if (headerKey == null && headerValue == null) break
                    if (headerKey != null && headerValue != null) {
                        responseHeaders[headerKey] = headerValue
                    }
                }
                
                val contentTypeHeader = connection.contentType ?: "text/html"
                val contentType = contentTypeHeader.substringBefore(";").trim()
                
                // Stream Media Sniffer: evaluates media mime-types & streaming extensions
                if (contentType.contains("video/", ignoreCase = true) || 
                    contentType.contains("audio/mpeg", ignoreCase = true) || 
                    contentType.contains("application/x-mpegURL", ignoreCase = true) || 
                    contentType.contains("application/vnd.apple.mpegurl", ignoreCase = true) ||
                    url.contains(".mp4", ignoreCase = true) || 
                    url.contains(".m3u8", ignoreCase = true)) {
                    viewModel?.updateActiveMediaDownloadTarget(url)
                }

                val encoding = if (contentTypeHeader.contains("charset=")) {
                    contentTypeHeader.substringAfter("charset=").substringBefore(";").trim()
                } else {
                    "UTF-8"
                }
                
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream ?: connection.inputStream
                } else {
                    connection.inputStream
                }
                
                return WebResourceResponse(
                    contentType,
                    encoding,
                    responseCode,
                    responseMessage,
                    responseHeaders,
                    inputStream
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (url != null) {
            // PDF path validation block
            if (url.contains("viewer.html", ignoreCase = true) || url.contains("pdfjs", ignoreCase = true)) {
                if (!url.startsWith("file:///android_asset/pdfjs/")) {
                    return WebResourceResponse("text/plain", "UTF-8", "Blocked: PDF viewer must load strictly from local app resources.".byteInputStream())
                }
            }
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: ""
            if (isMalicious(host)) {
                return generateSecurityWarningResponse()
            }
            if (shieldsEngine.shouldBlock(url)) {
                onTrackerBlocked(url)
                return shieldsEngine.generateBlankResponse()
            }
        }
        return super.shouldInterceptRequest(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        
        // HTTPS-Only Upgrade
        if (viewModel?.httpsOnlyMode?.value == true && url.startsWith("http://")) {
            val secureUrl = url.replaceFirst("http://", "https://")
            view?.loadUrl(secureUrl)
            return true
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false
        }
        if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("whatsapp:") || url.startsWith("intent:")) {
            try {
                val context = view?.context ?: return true
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return true
            }
        }
        return true
    }

    private fun isMalicious(host: String): Boolean {
        // Deep Network Verification: Evaluate host against shields engine intelligence
        return shieldsEngine.isMaliciousHost(host)
    }

    private fun generateSecurityWarningResponse(): WebResourceResponse {
        val html = """
            <html>
            <body style="background-color: #FEF2F2; color: #991B1B; font-family: sans-serif; padding: 40px; text-align: center;">
                <h1 style="font-size: 48px;">⚠️ Security Warning</h1>
                <p style="font-size: 20px;">The Secure Browser Firewall has intercepted and neutralized a connection to a known malicious or phishing domain.</p>
                <p style="font-size: 16px; color: #7F1D1D;">Connection Terminates Here for your protection.</p>
                <button onclick="window.history.back()" style="background-color: #B91C1C; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-size: 16px; margin-top: 20px;">Return to Safety</button>
            </body>
            </html>
        """.trimIndent()
        return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // Hard drop connection immediately on certificate validation failures
        handler?.cancel()

        // Securely destroy the view hierarchy and remove it from parent to completely eliminate MITM or sniffing vulnerabilities
        view?.post {
            try {
                view.stopLoading()
                view.clearHistory()
                view.clearCache(true)
                view.loadUrl("about:blank")
                val parent = view.parent as? android.view.ViewGroup
                parent?.removeView(view)
                view.destroy()
            } catch (e: Exception) {
                // Safeguard against runtime exceptions during disposal
            }
        }
    }

    private fun getFingerprintInjectionScript(): String {
        return FingerprintProtector.antiFingerprintScript
    }


    private fun getDarkModeEngineScript(): String {
        return """
            (function() {
                if (window.v_dark_mode_active) return;
                window.v_dark_mode_active = true;
                const style = document.createElement('style');
                style.id = 'v-dark-mode-engine';
                style.textContent = `
                    @media (prefers-color-scheme: dark) {
                        html, body { background-color: #0F172A !important; color: #E2E8F0 !important; }
                        img, video, canvas, iframe { filter: brightness(.8) contrast(1.2) !important; }
                        * { border-color: #334155 !important; }
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()
    }

    private fun registerDocumentStartScripts(view: WebView) {
        val hash = System.identityHashCode(view)
        if (registeredWebViews.contains(hash)) return

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                // Primary Anti-Fingerprinting Virtualization
                WebViewCompat.addDocumentStartJavaScript(view, getFingerprintInjectionScript(), setOf("*"))
                
                // LOCAL CUSTOM USER SCRIPT MANAGER
                // Systematically evaluate and inject local user modifications (.user.js data streams)
                viewModel?.userScriptsList?.value?.forEach { script ->
                    WebViewCompat.addDocumentStartJavaScript(view, script.second, setOf("*"))
                }
                
                // Advanced Web Layout Dark Mode Injection
                if (viewModel?.forcedDarkModeEnabled?.value == true) {
                    WebViewCompat.addDocumentStartJavaScript(view, getDarkModeEngineScript(), setOf("*"))
                }

                if (viewModel?.webRtcPrivacyEnabled?.value == true) {
                    WebViewCompat.addDocumentStartJavaScript(view, shieldsEngine.webRtcShieldScript, setOf("*"))
                }

                if (viewModel?.deAMPEnabled?.value == true) {
                    WebViewCompat.addDocumentStartJavaScript(view, shieldsEngine.ampNeutralizerScript, setOf("*"))
                }
                
                if (viewModel?.webRtcPrivacyEnabled?.value == true) {
                    WebViewCompat.addDocumentStartJavaScript(view, shieldsEngine.webRtcShieldScript, setOf("*"))
                }
                registeredWebViews.add(hash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        sweepPreviousOrigin(url)
        if (view != null) {
            registerDocumentStartScripts(view)
            
            val currentUA = view.settings.userAgentString
            val googleUA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            if (currentUA != googleUA) {
                baseUserAgent = currentUA
            }
            
            url?.let {
                try {
                    val uri = android.net.Uri.parse(it)
                    val host = uri.host
                    if (host != null && (host.endsWith("google.com") || host == "google.com" || host.contains(".google."))) {
                        view.settings.userAgentString = googleUA
                        view.settings.useWideViewPort = false
                        view.settings.loadWithOverviewMode = false
                        view.settings.textZoom = 100
                    } else {
                        val restoreUa = baseUserAgent
                        if (restoreUa != null) {
                            view.settings.userAgentString = restoreUa
                            if (restoreUa.contains("Mobile") || restoreUa.contains("Android")) {
                                view.settings.useWideViewPort = false
                                view.settings.loadWithOverviewMode = false
                                view.settings.textZoom = 100
                            } else {
                                view.settings.useWideViewPort = true
                                view.settings.loadWithOverviewMode = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        // Force-inject early scripts as a fallback if synchronous script binding is not supported
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            view?.evaluateJavascript(getFingerprintInjectionScript(), null)
            if (viewModel?.deAMPEnabled?.value == true) {
                view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
            }
            if (viewModel?.webRtcPrivacyEnabled?.value == true) {
                view?.evaluateJavascript(shieldsEngine.webRtcShieldScript, null)
            }
        }
        view?.let { injectCustomUserScripts(it) }
        url?.let {
            val cleanedUrl = if (viewModel?.stripTrackingEnabled?.value == true) cleanTrackingParameters(it) else it
            onPageStartedCallback(cleanedUrl)
        }
        view?.let {
            viewModel?.updateNavigationState(it.canGoBack(), it.canGoForward())
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        sweepPreviousOrigin(url)
        // Reinforce injection on completion just in case of iframe or state resets
        view?.evaluateJavascript(getFingerprintInjectionScript(), null)
        if (viewModel?.deAMPEnabled?.value == true) {
            view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        }
        if (viewModel?.webRtcPrivacyEnabled?.value == true) {
            view?.evaluateJavascript(shieldsEngine.webRtcShieldScript, null)
        }
        view?.let { injectCustomUserScripts(it) }
        val title = view?.title ?: ""
        url?.let { onPageFinishedCallback(it, title) }
        view?.let {
            viewModel?.updateNavigationState(it.canGoBack(), it.canGoForward())
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        // Crash Resilience Gateway: Intercept subsystem crashes and fire ViewModel watchdog
        viewModel?.handleRenderProcessCrash(view)
        return true // Prevent OS application force-close
    }

    private fun injectCustomUserScripts(view: WebView) {
        val scripts = viewModel?.userScriptsList?.value ?: return
        for (script in scripts) {
            val jsCode = script.second
            view.evaluateJavascript(jsCode, null)
        }
    }
}
