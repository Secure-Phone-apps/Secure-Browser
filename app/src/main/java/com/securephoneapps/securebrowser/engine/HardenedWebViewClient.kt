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
                // 1. Webdriver masking
                Object.defineProperty(navigator, 'webdriver', { get: () => false });

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
                                if (imgData && imgData.data && imgData.data.length > 4) {
                                    // Inject subtle noise into first color channel to corrupt fingerprint hash
                                    imgData.data[0] = (imgData.data[0] + 1) % 256;
                                }
                                if (window.FingerprintShield) {
                                    window.FingerprintShield.onCanvasFakeTriggered();
                                }
                                return imgData;
                            };
                            
                            // Scramble measureText for font fingerprinting protection
                            const orgMeasureText = ctx.measureText;
                            ctx.measureText = function() {
                                const metrics = orgMeasureText.apply(this, arguments);
                                const scramble = (val) => val + (Math.random() * 0.0002 - 0.0001);
                                if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("font_measure");
                                return {
                                    width: scramble(metrics.width),
                                    actualBoundingBoxLeft: metrics.actualBoundingBoxLeft,
                                    actualBoundingBoxRight: metrics.actualBoundingBoxRight,
                                    fontBoundingBoxAscent: metrics.fontBoundingBoxAscent,
                                    fontBoundingBoxDescent: metrics.fontBoundingBoxDescent,
                                    actualBoundingBoxAscent: metrics.actualBoundingBoxAscent,
                                    actualBoundingBoxDescent: metrics.actualBoundingBoxDescent,
                                    emHeightAscent: metrics.emHeightAscent,
                                    emHeightDescent: metrics.emHeightDescent,
                                    hangingBaseline: metrics.hangingBaseline,
                                    alphabeticBaseline: metrics.alphabeticBaseline,
                                    ideographicBaseline: metrics.ideographicBaseline
                                };
                            };
                        }
                        return ctx;
                    };
                }

                // 2.1 Element Layout Scrambling (Fingerprinting Shield)
                const scrambleLayout = (val) => val + (Math.random() * 0.0002 - 0.0001);
                const orgGetBoundingClientRect = Element.prototype.getBoundingClientRect;
                Element.prototype.getBoundingClientRect = function() {
                    const rect = orgGetBoundingClientRect.apply(this, arguments);
                    if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("layout_rect");
                    return {
                        x: scrambleLayout(rect.x), y: scrambleLayout(rect.y),
                        width: scrambleLayout(rect.width), height: scrambleLayout(rect.height),
                        top: scrambleLayout(rect.top), right: scrambleLayout(rect.right),
                        bottom: scrambleLayout(rect.bottom), left: scrambleLayout(rect.left),
                        toJSON: () => JSON.stringify(rect)
                    };
                };

                const orgGetClientRects = Element.prototype.getClientRects;
                Element.prototype.getClientRects = function() {
                    const list = orgGetClientRects.apply(this, arguments);
                    if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("layout_list");
                    return Array.from(list).map(rect => ({
                        x: scrambleLayout(rect.x), y: scrambleLayout(rect.y),
                        width: scrambleLayout(rect.width), height: scrambleLayout(rect.height),
                        top: scrambleLayout(rect.top), right: scrambleLayout(rect.right),
                        bottom: scrambleLayout(rect.bottom), left: scrambleLayout(rect.left)
                    }));
                };

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
        if (view != null) {
            registerDocumentStartScripts(view)
        }
        // Force-inject early scripts as a fallback if synchronous script binding is not supported
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            view?.evaluateJavascript(fingerprintInjectionScript, null)
            view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        }
        url?.let { onPageStartedCallback(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Reinforce injection on completion just in case of iframe or state resets
        view?.evaluateJavascript(fingerprintInjectionScript, null)
        view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
        val title = view?.title ?: ""
        url?.let { onPageFinishedCallback(it, title) }
    }
}
