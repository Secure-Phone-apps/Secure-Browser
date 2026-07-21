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
                val uri = android.net.Uri.parse(it)
                val currentHost = uri.host
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
                e.printStackTrace()
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
        
        // PDF path validation block
        if (url.contains("viewer.html", ignoreCase = true) || url.contains("pdfjs", ignoreCase = true)) {
            if (!url.startsWith("file:///android_asset/pdfjs/")) {
                return WebResourceResponse("text/plain", "UTF-8", "Blocked: PDF viewer must load strictly from local app resources.".byteInputStream())
            }
        }
        
        // PHISHING & MALWARE FIREWALL INTERCEPTION
        // High-speed malicious domain analyzer check
        val host = request.url.host ?: ""
        if (isMalicious(host)) {
            return shieldsEngine.generateBlankResponse()
        }

        if (shieldsEngine.shouldBlock(url)) {
            onTrackerBlocked(url)
            return shieldsEngine.generateBlankResponse()
        }

        // REFERRER STRIPPING INTERCEPTION LOOP
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                val requestHeaders = request.requestHeaders?.toMutableMap() ?: mutableMapOf()
                val refererKeys = requestHeaders.keys.filter { it.equals("Referer", ignoreCase = true) }
                for (key in refererKeys) {
                    requestHeaders.remove(key)
                }
                requestHeaders["Referer"] = "no-referrer"

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
                return shieldsEngine.generateBlankResponse()
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
        val audioActive = isAudioShieldActive()
        return """
        (function() {
            try {
                // 1. Webdriver masking - clean reCAPTCHA natural override
                delete navigator.__proto__.webdriver;

                // WebRTC Isolation & IP leak prevention
                if (window.RTCPeerConnection || window.webkitRTCPeerConnection) {
                    if (window.FingerprintShield) {
                        window.FingerprintShield.onFingerprintMockTriggered("webrtc");
                    }
                    try {
                        Object.defineProperty(window, 'RTCPeerConnection', { value: undefined, writable: false });
                        Object.defineProperty(window, 'webkitRTCPeerConnection', { value: undefined, writable: false });
                        Object.defineProperty(window, 'RTCIceCandidate', { value: undefined, writable: false });
                        Object.defineProperty(window, 'RTCSessionDescription', { value: undefined, writable: false });
                    } catch (webrtcErr) {}
                }

                // 2. Canvas Fingerprint protection via slight noise injection
                if (HTMLCanvasElement.prototype.getContext) {
                    const orgGetContext = HTMLCanvasElement.prototype.getContext;
                    HTMLCanvasElement.prototype.getContext = function(type) {
                        const ctx = orgGetContext.apply(this, arguments);
                        if (ctx && type === '2d') {
                            const orgGetImageData = ctx.getImageData;
                            ctx.getImageData = function(x, y, w, h) {
                                const imgData = orgGetImageData.apply(this, arguments);
                                if (imgData && imgData.data && imgData.data.length >= 16) {
                                    // Inject consistent shift into the last pixel quadrant during heavy operations
                                    const len = imgData.data.length;
                                    const lastPixelIdx = len - 4;
                                    imgData.data[lastPixelIdx] = (imgData.data[lastPixelIdx] + 1) % 256;
                                }
                                if (window.FingerprintShield) {
                                    window.FingerprintShield.onCanvasFakeTriggered();
                                }
                                return imgData;
                            };
                        }
                        return ctx;
                    };
                }

                // Canvas blending fillText opacity shift
                if (window.CanvasRenderingContext2D) {
                    const orgFillText = CanvasRenderingContext2D.prototype.fillText;
                    CanvasRenderingContext2D.prototype.fillText = function(text, x, y, maxWidth) {
                        const originalAlpha = this.globalAlpha;
                        const variance = 0.00001;
                        this.globalAlpha = Math.max(0, originalAlpha - variance);
                        const res = orgFillText.apply(this, arguments);
                        this.globalAlpha = originalAlpha;
                        return res;
                    };
                }



                // 3. WebGL GPU and Vendor virtualization fakes
                if (window.WebGLRenderingContext) {
                    const orgGetParameter = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(pname) {
                        // UNMASKED_VENDOR_WEBGL (0x9245) or UNMASKED_RENDERER_WEBGL (0x9246)
                        if (pname === 37445) {
                            if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("webgl_vendor");
                            return "Google Inc.";
                        }
                        if (pname === 37446) {
                            if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("webgl_renderer");
                            return "Google SwiftShader";
                        }
                        return orgGetParameter.apply(this, arguments);
                    };
                }
                const hookWebGLGetExtension = (proto) => {
                    if (proto && proto.getExtension) {
                        const orgGetExtension = proto.getExtension;
                        proto.getExtension = function(name) {
                            if (name && (name.indexOf('WEBGL_debug_renderer_info') !== -1 || name.toLowerCase().indexOf('renderer_info') !== -1)) {
                                if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("webgl_extension_blocked");
                                return null;
                            }
                            return orgGetExtension.apply(this, arguments);
                        };
                    }
                };
                if (window.WebGLRenderingContext) {
                    hookWebGLGetExtension(WebGLRenderingContext.prototype);
                }
                if (window.WebGL2RenderingContext) {
                    hookWebGLGetExtension(WebGL2RenderingContext.prototype);
                }

                // 4. Audio API protection fakes
                if ($audioActive) {
                    if (window.AudioContext) {
                        const orgCreateAnalyser = AudioContext.prototype.createAnalyser;
                        AudioContext.prototype.createAnalyser = function() {
                            const analyser = orgCreateAnalyser.apply(this, arguments);
                            try {
                                const orgGetByteFrequencyData = analyser.getByteFrequencyData;
                                analyser.getByteFrequencyData = function(array) {
                                    const res = orgGetByteFrequencyData.apply(this, arguments);
                                    for (let i = 0; i < array.length; i++) {
                                        array[i] = Math.max(0, Math.min(255, array[i] + (Math.random() * 2 - 1) * 0.00001));
                                    }
                                    return res;
                                };
                            } catch (e) {}
                            return analyser;
                        };
                    }
                    if (window.OfflineAudioContext) {
                        const orgCreateAnalyser = OfflineAudioContext.prototype.createAnalyser;
                        if (orgCreateAnalyser) {
                            OfflineAudioContext.prototype.createAnalyser = function() {
                                const analyser = orgCreateAnalyser.apply(this, arguments);
                                try {
                                    const orgGetByteFrequencyData = analyser.getByteFrequencyData;
                                    analyser.getByteFrequencyData = function(array) {
                                        const res = orgGetByteFrequencyData.apply(this, arguments);
                                        for (let i = 0; i < array.length; i++) {
                                            array[i] = Math.max(0, Math.min(255, array[i] + (Math.random() * 2 - 1) * 0.00001));
                                        }
                                        return res;
                                    };
                                } catch (e) {}
                                return analyser;
                            };
                        }
                    }
                    if (window.AudioBuffer) {
                        const orgGetChannelData = AudioBuffer.prototype.getChannelData;
                        AudioBuffer.prototype.getChannelData = function() {
                            const data = orgGetChannelData.apply(this, arguments);
                            if (data && data.length > 0) {
                                // Inject minor sound noise float to distort canvas sound fingerprinters
                                for (let i = 0; i < Math.min(data.length, 100); i++) {
                                    data[i] = data[i] + (Math.random() * 0.00002 - 0.00001);
                                }
                            }
                            if (window.FingerprintShield) {
                                window.FingerprintShield.onFingerprintMockTriggered("audio");
                            }
                            return data;
                        };
                    }
                }

                // 5. Battery profiling isolation
                const fakeBattery = function() {
                    if (window.FingerprintShield) {
                        window.FingerprintShield.onFingerprintMockTriggered("battery");
                    }
                    return Promise.resolve({
                        charging: true,
                        chargingTime: 0,
                        dischargingTime: Infinity,
                        level: 1.0,
                        onchargingchange: null,
                        onchargingtimechange: null,
                        ondischargingtimechange: null,
                        onlevelchange: null
                    });
                };
                Object.defineProperty(navigator, 'getBattery', {
                    value: fakeBattery,
                    writable: true,
                    configurable: true,
                    enumerable: true
                });
                if (typeof Navigator !== 'undefined' && Navigator.prototype) {
                    Object.defineProperty(Navigator.prototype, 'getBattery', {
                        value: fakeBattery,
                        writable: true,
                        configurable: true,
                        enumerable: true
                    });
                }

                // 6. Language & Localization Masking
                Object.defineProperty(navigator, 'language', { get: () => 'en-US' });
                Object.defineProperty(navigator, 'languages', { get: () => ['en-US'] });
                
                // 7. Time Zone Masking
                const orgResolvedOptions = Intl.DateTimeFormat.prototype.resolvedOptions;
                Intl.DateTimeFormat.prototype.resolvedOptions = function() {
                    const options = orgResolvedOptions.apply(this, arguments);
                    return { ...options, timeZone: 'UTC' };
                };

                // 8. Motion Sensor Cloaking
                const zeroEvent = {
                    alpha: 0, beta: 0, gamma: 0,
                    absolute: false,
                    acceleration: { x: 0, y: 0, z: 0 },
                    accelerationIncludingGravity: { x: 0, y: 0, z: 0 },
                    rotationRate: { alpha: 0, beta: 0, gamma: 0 },
                    interval: 16
                };
                window.DeviceOrientationEvent = function() { return zeroEvent; };
                window.DeviceMotionEvent = function() { return zeroEvent; };

                // 9. WebRTC Media Device Masking
                if (navigator.mediaDevices && navigator.mediaDevices.enumerateDevices) {
                    const orgEnumerateDevices = navigator.mediaDevices.enumerateDevices;
                    navigator.mediaDevices.enumerateDevices = function() {
                        return orgEnumerateDevices.apply(this, arguments).then(devices => {
                            return devices.map(device => {
                                let label = "Standard Device";
                                if (device.kind === 'audioinput') label = "Standard Microphone Input";
                                if (device.kind === 'audiooutput') label = "Standard Speaker Output";
                                if (device.kind === 'videoinput') label = "Default Rear Camera";
                                
                                return {
                                    deviceId: "default",
                                    kind: device.kind,
                                    label: label,
                                    groupId: "default"
                                };
                            });
                        });
                    };
                }

                // 10. Web Crypto API Spoofing
                if (window.crypto && window.crypto.subtle && window.crypto.subtle.generateKey) {
                    const orgGenerateKey = window.crypto.subtle.generateKey;
                    window.crypto.subtle.generateKey = function(algorithm, extractable, keyUsages) {
                        if (window.FingerprintShield) {
                            window.FingerprintShield.onFingerprintMockTriggered("webcrypto");
                        }
                        // Inject small metadata variation in the algorithm object to randomize analytic entity key-gen fingerprint entropy
                        const modifiedAlgorithm = typeof algorithm === 'string' ? algorithm : { ...algorithm, length: algorithm.length || 256, salt: new Uint8Array([Math.floor(Math.random() * 256)]) };
                        return orgGenerateKey.call(this, modifiedAlgorithm, extractable, keyUsages);
                    };
                }

                // 11. Force Sans-Serif Font Rendering Substitution to Defeat Font Enumeration Fingerprinting
                const injectGlobalFontSubstitution = () => {
                    const style = document.createElement('style');
                    style.id = 'font-scrambler-override';
                    style.textContent = 'body, html, * { font-family: sans-serif !important; }';
                    if (document.head) {
                        document.head.appendChild(style);
                    } else if (document.documentElement) {
                        document.documentElement.appendChild(style);
                    }
                };
                injectGlobalFontSubstitution();
                document.addEventListener('DOMContentLoaded', injectGlobalFontSubstitution);

                // 12. Background video playback & visibility event neutralization
                const handleVisibility = (e) => {
                    e.stopImmediatePropagation();
                };
                document.addEventListener('visibilitychange', handleVisibility, true);
                document.addEventListener('webkitvisibilitychange', handleVisibility, true);
                Object.defineProperty(document, 'visibilityState', {
                    get: () => 'visible',
                    configurable: true
                });
                Object.defineProperty(document, 'hidden', {
                    get: () => false,
                    configurable: true
                });

                // 13. Clipboard security protector
                let lastPhysicalUserInteraction = 0;
                const recordUserInteraction = () => {
                    lastPhysicalUserInteraction = Date.now();
                };
                window.addEventListener('click', recordUserInteraction, true);
                window.addEventListener('touchstart', recordUserInteraction, true);
                window.addEventListener('keydown', recordUserInteraction, true);

                if (navigator.clipboard && navigator.clipboard.readText) {
                    const orgReadText = navigator.clipboard.readText;
                    navigator.clipboard.readText = function() {
                        const timeSinceInteraction = Date.now() - lastPhysicalUserInteraction;
                        if (timeSinceInteraction > 1000) {
                            if (window.FingerprintShield) {
                                window.FingerprintShield.onFingerprintMockTriggered("clipboard_access_blocked");
                            }
                            return Promise.reject(new DOMException("Clipboard read access denied without active physical user interaction.", "NotAllowedError"));
                        }
                        return orgReadText.apply(this, arguments);
                    };
                }

                document.addEventListener('paste', (e) => {
                    const timeSinceInteraction = Date.now() - lastPhysicalUserInteraction;
                    if (timeSinceInteraction > 1000) {
                        e.stopImmediatePropagation();
                        e.preventDefault();
                        if (window.FingerprintShield) {
                            window.FingerprintShield.onFingerprintMockTriggered("clipboard_paste_blocked");
                        }
                    }
                }, true);

                // 14. Battery status virtualization
                if (navigator.getBattery) {
                    navigator.getBattery = function() {
                        if (window.FingerprintShield) {
                            window.FingerprintShield.onFingerprintMockTriggered("battery_virtualized");
                        }
                        return Promise.resolve({
                            charging: true,
                            chargingTime: 0,
                            dischargingTime: Infinity,
                            level: 1.0,
                            onchargingchange: null,
                            onchargingtimechange: null,
                            ondischargingtimechange: null,
                            onlevelchange: null,
                            addEventListener: function() {},
                            removeEventListener: function() {},
                            dispatchEvent: function() { return true; }
                        });
                    };
                }
            } catch (e) {
                console.error("Fingerprint shielding exception:", e);
            }
        })();
        """.trimIndent()
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

                WebViewCompat.addDocumentStartJavaScript(view, shieldsEngine.ampNeutralizerScript, setOf("*"))
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
            view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        }
        view?.let { injectCustomUserScripts(it) }
        url?.let {
            val cleanedUrl = cleanTrackingParameters(it)
            onPageStartedCallback(cleanedUrl)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        sweepPreviousOrigin(url)
        // Reinforce injection on completion just in case of iframe or state resets
        view?.evaluateJavascript(getFingerprintInjectionScript(), null)
        view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        view?.let { injectCustomUserScripts(it) }
        val title = view?.title ?: ""
        url?.let { onPageFinishedCallback(it, title) }
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
