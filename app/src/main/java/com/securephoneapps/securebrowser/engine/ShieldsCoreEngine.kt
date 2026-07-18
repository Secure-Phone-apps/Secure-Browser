package com.securephoneapps.securebrowser.engine

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.regex.Pattern

class ShieldsCoreEngine {

    // HashSet for fast O(1) exact and end-with domain checks
    private val blockedDomains = hashSetOf(
        "doubleclick.net",
        "google-analytics.com",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "app-measurement.com",
        "analytics.google.com",
        "telemetry.google.com",
        "pagead2.googlesyndication.com",
        "stats.g.doubleclick.net",
        "scorecardresearch.com",
        "crashlytics.com",
        "adnxs.com",
        "outbrain.com",
        "taboola.com",
        "optimizely.com",
        "mixpanel.com",
        "amplitude.com",
        "hotjar.com",
        "carbonads.net",
        "buysellads.com",
        "amazon-adsystem.com",
        "ads-twitter.com",
        "ads.youtube.com",
        "telemetry",
        "tracker",
        "adsystem",
        "adcolony.com",
        "chartbeat.com",
        "disqus.com",
        "quantserve.com"
    )

    // Regex patterns for advanced path, sub-domain, or query matching
    private val blockedPatterns = listOf(
        Pattern.compile(".*\\banalytics\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\btelemetry\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\btracker\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\badsystem\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\bspyware\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\badserver\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\bmetrics\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\bmarketing\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\bpixels\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*google\\.com/recaptcha/api2/anchor.*", Pattern.CASE_INSENSITIVE)
    )

    // Raw JavaScript script to block and neutralize Google AMP redirections, forcing canonical web loading
    val ampNeutralizerScript: String = """
        (function() {
            function neutralizeAMP() {
                var canonicalLink = document.querySelector('link[rel="canonical"]');
                if (canonicalLink && canonicalLink.href) {
                    var canonicalUrl = canonicalLink.href;
                    if (window.location.href !== canonicalUrl && 
                        (window.location.host.includes('google.com') && window.location.pathname.includes('/amp/'))) {
                        window.location.replace(canonicalUrl);
                        return true;
                    }
                }
                return false;
            }
            if (!neutralizeAMP()) {
                window.addEventListener('DOMContentLoaded', neutralizeAMP);
            }
        })();
    """.trimIndent()

    /**
     * Determines whether the given URL is a tracker, ad network, or diagnostic logging telemetry endpoint.
     */
    fun shouldBlock(url: String): Boolean {
        if (url.isBlank()) return false

        try {
            val uri = URI(url)
            val host = uri.host ?: return false

            // 1. Direct HashSet domain checks
            if (blockedDomains.contains(host)) {
                return true
            }

            // Subdomain matching
            for (blocked in blockedDomains) {
                if (host.endsWith(".$blocked")) {
                    return true
                }
            }

            // 2. Regex and keyword matching
            for (pattern in blockedPatterns) {
                if (pattern.matcher(url).matches()) {
                    return true
                }
            }

            // 3. De-googling: block background diagnostics/logging/gen_204 calls
            if (host.contains("google.com") && 
                (url.contains("/log") || url.contains("/telemetry") || url.contains("/gen_204") || url.contains("/collect"))) {
                return true
            }

        } catch (e: Exception) {
            // Safe fallback substring checks if URI parsing fails
            val lower = url.lowercase()
            if (lower.contains("telemetry") || lower.contains("analytics") || lower.contains("doubleclick")) {
                return true
            }
        }

        return false
    }

    /**
     * Generates a structural blank response with zeroed byte arrays to prevent page loading timeouts and crash elements.
     */
    fun generateBlankResponse(): WebResourceResponse {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        return WebResourceResponse(
            "text/javascript",
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache, no-store, must-revalidate"
            ),
            emptyStream
        )
    }
}
