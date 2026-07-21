package com.securephoneapps.securebrowser.engine

object ScriptProvider {
    val darkModeScript = """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = 'html, body { filter: invert(0.9) hue-rotate(180deg) !important; background: #121212 !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }';
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
}
