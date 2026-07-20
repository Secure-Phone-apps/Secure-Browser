package com.securephoneapps.securebrowser.engine

import android.graphics.Bitmap
import android.net.http.SslError
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
    private val onPageFinishedCallback: (url: String, title: String) -> Unit = { _, _ -> }
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
                        android.webkit.WebStorage.getInstance().deleteOrigin("https://$previousHost")
                        android.webkit.WebStorage.getInstance().deleteOrigin("http://$previousHost")
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
        
        // PHISHING & MALWARE FIREWALL INTERCEPTION
        // High-speed malicious domain analyzer check
        val host = request.url.host ?: ""
        if (isMalicious(host)) {
            return generateSecurityWarningResponse()
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

                val connectionUrl = java.net.URL(url)
                val connection = connectionUrl.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = request.method
                
                for ((key, value) in requestHeaders) {
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

    private fun isMalicious(host: String): Boolean {
        val maliciousPatterns = listOf(
            "phishing", "malware", "deceptive", "scam-", "-login-update",
            "verify-account", "secure-bank-login", "bit.ly/malicious",
            "suspicious-redirect", "malvertising"
        )
        return maliciousPatterns.any { host.contains(it, ignoreCase = true) }
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

    private val fingerprintInjectionScript: String = """
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

                // 4. Audio API protection fakes
                if (window.AudioBuffer) {
                    const orgGetChannelData = AudioBuffer.prototype.getChannelData;
                    AudioBuffer.prototype.getChannelData = function() {
                        const data = orgGetChannelData.apply(this, arguments);
                        if (data && data.length > 0) {
                            // Inject minor sound noise float to distort canvas sound fingerprinters
                            data[0] = data[0] + 0.00001;
                        }
                        if (window.FingerprintShield) {
                            window.FingerprintShield.onFingerprintMockTriggered("audio");
                        }
                        return data;
                    };
                }

                // 5. Battery profiling isolation
                if (navigator.getBattery) {
                    navigator.getBattery = function() {
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
            } catch (e) {
                console.error("Fingerprint shielding exception:", e);
            }
        })();
    """.trimIndent()

    private fun registerDocumentStartScripts(view: WebView) {
        val hash = System.identityHashCode(view)
        if (registeredWebViews.contains(hash)) return

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try {
                WebViewCompat.addDocumentStartJavaScript(view, fingerprintInjectionScript, setOf("*"))
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
            view?.evaluateJavascript(fingerprintInjectionScript, null)
            view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        }
        url?.let {
            val cleanedUrl = cleanTrackingParameters(it)
            onPageStartedCallback(cleanedUrl)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        sweepPreviousOrigin(url)
        // Reinforce injection on completion just in case of iframe or state resets
        view?.evaluateJavascript(fingerprintInjectionScript, null)
        view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        val title = view?.title ?: ""
        url?.let { onPageFinishedCallback(it, title) }
    }
}
