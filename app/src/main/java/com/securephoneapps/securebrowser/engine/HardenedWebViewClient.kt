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
        if (shieldsEngine.shouldBlock(url)) {
            onTrackerBlocked(url)
            return shieldsEngine.generateBlankResponse()
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (url != null && shieldsEngine.shouldBlock(url)) {
            onTrackerBlocked(url)
            return shieldsEngine.generateBlankResponse()
        }
        return super.shouldInterceptRequest(view, url)
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
