package com.securephoneapps.securebrowser.engine

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

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
    private val onFingerprintMocked: (type: String) -> Unit
) : WebViewClient() {

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

        val secureErrorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
                        background-color: #0D0D0D;
                        color: #FF453A;
                        text-align: center;
                        padding: 32px 16px;
                        margin: 0;
                    }
                    .container {
                        max-width: 480px;
                        margin: 40px auto 0 auto;
                        border: 1px solid #FF453A;
                        border-radius: 16px;
                        padding: 32px;
                        background-color: #161616;
                        box-shadow: 0 8px 30px rgba(0,0,0,0.7);
                    }
                    h1 { font-size: 22px; margin-bottom: 12px; font-weight: 700; letter-spacing: -0.5px; }
                    p { color: #AEAEB2; line-height: 1.5; font-size: 14px; margin: 8px 0; }
                    .badge {
                        display: inline-block;
                        background-color: rgba(255, 69, 58, 0.15);
                        color: #FF453A;
                        border: 1px solid #FF453A;
                        padding: 6px 14px;
                        border-radius: 20px;
                        font-size: 11px;
                        font-weight: 700;
                        text-transform: uppercase;
                        margin-bottom: 24px;
                        letter-spacing: 0.5px;
                    }
                    .icon {
                        font-size: 40px;
                        margin-bottom: 8px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">🛡️</div>
                    <div class="badge">MITM Protection Active</div>
                    <h1>SECURE CONNECTION BLOCKED</h1>
                    <p>
                        The Secure Browser engine detected an invalid, expired, or untrusted SSL certificate for this host.
                    </p>
                    <p style="color: #8E8E93; font-size: 13px; margin-top: 16px;">
                        To protect your sensitive credentials, local identifiers, and browser safety, fallback options have been completely locked out.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()

        view?.loadDataWithBaseURL(null, secureErrorHtml, "text/html", "UTF-8", null)
    }

    private val fingerprintInjectionScript: String = """
        (function() {
            try {
                // 1. Webdriver masking
                Object.defineProperty(navigator, 'webdriver', { get: () => false });

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

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Inject early shields
        view?.evaluateJavascript(fingerprintInjectionScript, null)
        view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // Reinforce injection on completion
        view?.evaluateJavascript(fingerprintInjectionScript, null)
        view?.evaluateJavascript(shieldsEngine.ampNeutralizerScript, null)
    }
}
