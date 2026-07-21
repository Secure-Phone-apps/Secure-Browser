package com.securephoneapps.securebrowser.engine

object FingerprintProtector {
    /**
     * The definitive, stage-zero anti-fingerprinting JavaScript payload.
     * This script intercepts and virtualizes high-entropy browser APIs to prevent tracking.
     */
    val antiFingerprintScript: String = """
        (function() {
            try {
                // 1. Webdriver masking - clean reCAPTCHA natural override
                delete navigator.__proto__.webdriver;

                // 2. WebRTC Isolation & IP leak prevention
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

                // 3. Canvas Fingerprint protection (0.0001% text variance & pixel noise)
                if (HTMLCanvasElement.prototype.getContext) {
                    const orgGetContext = HTMLCanvasElement.prototype.getContext;
                    HTMLCanvasElement.prototype.getContext = function(type) {
                        const ctx = orgGetContext.apply(this, arguments);
                        if (ctx && type === '2d') {
                            const orgGetImageData = ctx.getImageData;
                            ctx.getImageData = function(x, y, w, h) {
                                const imgData = orgGetImageData.apply(this, arguments);
                                if (imgData && imgData.data && imgData.data.length >= 16) {
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

                // 4. WebGL GPU Extension & Vendor stripping
                if (window.WebGLRenderingContext) {
                    const orgGetParameter = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(pname) {
                        // UNMASKED_VENDOR_WEBGL (37445) or UNMASKED_RENDERER_WEBGL (37446)
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

                const hookWebGL = (proto) => {
                    if (proto && proto.getExtension) {
                        const orgExt = proto.getExtension;
                        proto.getExtension = function(name) {
                            if (name && (name.indexOf('WEBGL_debug_renderer_info') !== -1 || name.toLowerCase().indexOf('renderer_info') !== -1)) {
                                if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("webgl_extension_blocked");
                                return null;
                            }
                            return orgExt.apply(this, arguments);
                        };
                    }
                };
                if (window.WebGLRenderingContext) hookWebGL(WebGLRenderingContext.prototype);
                if (window.WebGL2RenderingContext) hookWebGL(WebGL2RenderingContext.prototype);

                // 5. AudioContext Frequency Scrambling
                if (window.AudioContext || window.webkitAudioContext) {
                    const Context = window.AudioContext || window.webkitAudioContext;
                    const orgAnalyser = Context.prototype.createAnalyser;
                    Context.prototype.createAnalyser = function() {
                        const analyser = orgAnalyser.apply(this, arguments);
                        const orgFreq = analyser.getByteFrequencyData;
                        analyser.getByteFrequencyData = function(array) {
                            const res = orgFreq.apply(this, arguments);
                            for (let i = 0; i < array.length; i++) {
                                array[i] = Math.max(0, Math.min(255, array[i] + (Math.random() * 2 - 1) * 0.0001));
                            }
                            return res;
                        };
                        return analyser;
                    };
                }

                // 6. Battery Mock Status
                const mockBattery = () => Promise.resolve({
                    charging: true,
                    chargingTime: 0,
                    dischargingTime: Infinity,
                    level: 1.0,
                    onchargingchange: null,
                    onchargingtimechange: null,
                    ondischargingtimechange: null,
                    onlevelchange: null,
                    addEventListener: () => {},
                    removeEventListener: () => {},
                    dispatchEvent: () => true
                });
                if (navigator.getBattery) {
                    Object.defineProperty(navigator, 'getBattery', { value: mockBattery, configurable: true });
                }

                // 7. TimeZone & Language Normalization
                Object.defineProperty(navigator, 'language', { get: () => 'en-US' });
                Object.defineProperty(navigator, 'languages', { get: () => ['en-US'] });
                const orgOpt = Intl.DateTimeFormat.prototype.resolvedOptions;
                Intl.DateTimeFormat.prototype.resolvedOptions = function() {
                    return { ...orgOpt.apply(this, arguments), timeZone: 'UTC' };
                };

                // 8. Automated Clipboard Access Restrictions
                let lastInteraction = 0;
                ['click', 'touchstart', 'keydown'].forEach(e => window.addEventListener(e, () => lastInteraction = Date.now(), true));

                if (navigator.clipboard && navigator.clipboard.readText) {
                    const orgRead = navigator.clipboard.readText;
                    navigator.clipboard.readText = function() {
                        if (Date.now() - lastInteraction > 1000) {
                            if (window.FingerprintShield) window.FingerprintShield.onFingerprintMockTriggered("clipboard_access_blocked");
                            return Promise.reject(new DOMException("Interaction required", "NotAllowedError"));
                        }
                        return orgRead.apply(this, arguments);
                    };
                }

                // 9. Hardware Sensor Neutralization
                window.DeviceOrientationEvent = () => ({ alpha: 0, beta: 0, gamma: 0, absolute: false });
                window.DeviceMotionEvent = () => ({ acceleration: {x:0, y:0, z:0}, rotationRate: {alpha:0, beta:0, gamma:0}, interval: 16 });

            } catch (e) { console.error("Fingerprint Shield Error", e); }
        })();
    """.trimIndent()
}
