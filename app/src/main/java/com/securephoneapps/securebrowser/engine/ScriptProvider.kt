package com.securephoneapps.securebrowser.engine

object ScriptProvider {
    val darkModeScript = """
        (function() {
            if (window.v_custom_dark_active) return;
            window.v_custom_dark_active = true;
            var style = document.createElement('style');
            style.innerHTML = 'html, body { background-color: #121212 !important; color: #E0E0E0 !important; } p, span, h1, h2, h3, h4, h5, h6, li, a, td, th { color: #E0E0E0 !important; }';
            document.head.appendChild(style);
        })();
    """.trimIndent()

    val webRtcShieldScript = """
        (function() {
            if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                navigator.mediaDevices.getUserMedia = function() {
                    return Promise.reject(new Error('WebRTC is disabled for privacy.'));
                };
            }
            var RTCPeerConnection = window.RTCPeerConnection || window.webkitRTCPeerConnection || window.mozRTCPeerConnection;
            if (RTCPeerConnection) {
                window.RTCPeerConnection = function() { return null; };
                if (window.webkitRTCPeerConnection) window.webkitRTCPeerConnection = function() { return null; };
                if (window.mozRTCPeerConnection) window.mozRTCPeerConnection = function() { return null; };
            }
        })();
    """.trimIndent()

    val ampNeutralizerScript = """
        (function() {
            if (window.location.hostname.includes('google.com') && window.location.pathname.includes('/amp/')) {
                var realUrl = window.location.href.split('/amp/s/')[1] || window.location.href.split('/amp/')[1];
                if (realUrl) window.location.replace('https://' + realUrl);
            }
        })();
    """.trimIndent()

    val gpcScript = """
        (function() {
            if (!('globalPrivacyControl' in navigator)) {
                Object.defineProperty(Navigator.prototype, 'globalPrivacyControl', {
                    get: function() { return true; },
                    enumerable: true,
                    configurable: false
                });
            }
        })();
    """.trimIndent()

    val backgroundMediaPlaybackScript = """
        (function() {
            try {
                Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
            } catch (e) {}

            var blockEvents = ['visibilitychange', 'webkitvisibilitychange', 'pagehide'];
            var originalAddEventListener = EventTarget.prototype.addEventListener;
            EventTarget.prototype.addEventListener = function(type, listener, options) {
                if (blockEvents.indexOf(type) !== -1) {
                    return;
                }
                return originalAddEventListener.call(this, type, listener, options);
            };
        })();
    """.trimIndent()
}
