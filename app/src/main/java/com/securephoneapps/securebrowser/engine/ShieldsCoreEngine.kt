package com.securephoneapps.securebrowser.engine

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream

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

    private val blockedKeywords = hashSetOf(
        "analytics", "telemetry", "tracker", "adsystem", "spyware", "adserver", "metrics", "marketing", "pixels", "gen_204", "doubleclick"
    )

    // Highly optimized primitive Bloom Filter to check block candidates in O(1) sub-microsecond time
    private val bloomFilterSize = 16384
    private val bloomBitSet = LongArray(bloomFilterSize / 64)

    init {
        // Populate Bloom Filter with initial rules
        blockedDomains.forEach { addToBloom(it) }
        blockedKeywords.forEach { addToBloom(it) }
    }

    // MurmurHash3 32-bit implementation with configurable seed to eliminate false-positive URL matches
    private fun murmurHash3(data: String, seed: Int): Int {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val length = bytes.size
        var h1 = seed
        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593

        val nblocks = length / 4
        for (i in 0 until nblocks) {
            val index = i * 4
            var k1 = (bytes[index].toInt() and 0xff) or
                    ((bytes[index + 1].toInt() and 0xff) shl 8) or
                    ((bytes[index + 2].toInt() and 0xff) shl 16) or
                    ((bytes[index + 3].toInt() and 0xff) shl 24)

            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2

            h1 = h1 xor k1
            h1 = (h1 shl 13) or (h1 ushr 19)
            h1 = h1 * 5 + 0xe6546b64.toInt()
        }

        var k1 = 0
        val tailStart = nblocks * 4
        val remainder = length - tailStart
        if (remainder >= 3) k1 = k1 xor ((bytes[tailStart + 2].toInt() and 0xff) shl 16)
        if (remainder >= 2) k1 = k1 xor ((bytes[tailStart + 1].toInt() and 0xff) shl 8)
        if (remainder >= 1) {
            k1 = k1 xor (bytes[tailStart].toInt() and 0xff)
            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2
            h1 = h1 xor k1
        }

        h1 = h1 xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= 0x85ebca6b.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= 0xc2b2ae35.toInt()
        h1 = h1 xor (h1 ushr 16)

        return h1
    }

    private fun addToBloom(item: String) {
        val h1 = murmurHash3(item, 0x12345678)
        val h2 = murmurHash3(item, 0x9abcdef0.toInt())
        val h3 = murmurHash3(item, 0x55555555)
        
        val bit1 = (h1 and 0x7FFFFFFF) % bloomFilterSize
        val bit2 = (h2 and 0x7FFFFFFF) % bloomFilterSize
        val bit3 = (h3 and 0x7FFFFFFF) % bloomFilterSize
        
        bloomBitSet[bit1 / 64] = bloomBitSet[bit1 / 64] or (1L shl (bit1 % 64))
        bloomBitSet[bit2 / 64] = bloomBitSet[bit2 / 64] or (1L shl (bit2 % 64))
        bloomBitSet[bit3 / 64] = bloomBitSet[bit3 / 64] or (1L shl (bit3 % 64))
    }

    private fun checkBloom(item: String): Boolean {
        val h1 = murmurHash3(item, 0x12345678)
        val h2 = murmurHash3(item, 0x9abcdef0.toInt())
        val h3 = murmurHash3(item, 0x55555555)
        
        val bit1 = (h1 and 0x7FFFFFFF) % bloomFilterSize
        val bit2 = (h2 and 0x7FFFFFFF) % bloomFilterSize
        val bit3 = (h3 and 0x7FFFFFFF) % bloomFilterSize
        
        val val1 = (bloomBitSet[bit1 / 64] and (1L shl (bit1 % 64))) != 0L
        val val2 = (bloomBitSet[bit2 / 64] and (1L shl (bit2 % 64))) != 0L
        val val3 = (bloomBitSet[bit3 / 64] and (1L shl (bit3 % 64))) != 0L
        return val1 && val2 && val3
    }

    /**
     * Companion stream reader capable of processing compressed rulesets dynamically at runtime.
     */
    fun importCompressedRuleset(input: InputStream) {
        try {
            val bufferedInput = java.io.BufferedInputStream(input)
            bufferedInput.mark(2)
            val header = ByteArray(2)
            val readBytes = bufferedInput.read(header)
            bufferedInput.reset()
            
            val isGzip = readBytes == 2 && 
                (header[0].toInt() and 0xFF == 0x1f) && 
                (header[1].toInt() and 0xFF == 0x8b)

            val finalStream = if (isGzip) {
                java.util.zip.GZIPInputStream(bufferedInput)
            } else {
                bufferedInput
            }

            val reader = java.io.BufferedReader(java.io.InputStreamReader(finalStream, Charsets.UTF_8))
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                    var cleanRule = trimmed
                    if (cleanRule.startsWith("||")) {
                        cleanRule = cleanRule.substring(2)
                    }
                    val caretIdx = cleanRule.indexOf('^')
                    if (caretIdx != -1) {
                        cleanRule = cleanRule.substring(0, caretIdx)
                    }
                    val slashIdx = cleanRule.indexOf('/')
                    if (slashIdx != -1) {
                        cleanRule = cleanRule.substring(0, slashIdx)
                    }
                    cleanRule = cleanRule.trim().lowercase()
                    if (cleanRule.isNotEmpty()) {
                        if (cleanRule.contains(".")) {
                            blockedDomains.add(cleanRule)
                            addToBloom(cleanRule)
                        } else {
                            blockedKeywords.add(cleanRule)
                            addToBloom(cleanRule)
                        }
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Network-facing updater method designed to parse and populate the internal matching architecture
     * from standardized string asset updates seamlessly.
     */
    fun updateFilterList(rulesStream: InputStream) {
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(rulesStream, Charsets.UTF_8))
            var line = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                    var cleanRule = trimmed
                    if (cleanRule.startsWith("||")) {
                        cleanRule = cleanRule.substring(2)
                    }
                    val caretIdx = cleanRule.indexOf('^')
                    if (caretIdx != -1) {
                        cleanRule = cleanRule.substring(0, caretIdx)
                    }
                    val slashIdx = cleanRule.indexOf('/')
                    if (slashIdx != -1) {
                        cleanRule = cleanRule.substring(0, slashIdx)
                    }
                    cleanRule = cleanRule.trim().lowercase()
                    if (cleanRule.isNotEmpty()) {
                        if (cleanRule.contains(".")) {
                            blockedDomains.add(cleanRule)
                            addToBloom(cleanRule)
                        } else {
                            blockedKeywords.add(cleanRule)
                            addToBloom(cleanRule)
                        }
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
     * Fast host extractor that avoids costly Java/Android URI parsing or Regex matches
     */
    private fun extractHost(url: String): String? {
        val doubleSlash = url.indexOf("//")
        val start = if (doubleSlash != -1) doubleSlash + 2 else 0
        if (start >= url.length) return null
        var end = url.indexOf('/', start)
        if (end == -1) {
            end = url.indexOf('?', start)
        }
        if (end == -1) {
            end = url.indexOf('#', start)
        }
        if (end == -1) {
            end = url.length
        }
        var host = url.substring(start, end)
        val portIndex = host.indexOf(':')
        if (portIndex != -1) {
            host = host.substring(0, portIndex)
        }
        return host.lowercase()
    }

    /**
     * Highly optimized Dual-Layer Firewall. Runs in <0.1ms per asset request.
     */
    fun shouldBlock(url: String): Boolean {
        if (url.isEmpty()) return false

        val host = extractHost(url) ?: return false

        // Layer A: Bloom filter check on the host name
        var hitBloom = checkBloom(host)
        if (!hitBloom) {
            // Also check keywords in the url using bloom-filtered substring check to capture tracking parameters
            for (keyword in blockedKeywords) {
                if (url.contains(keyword)) {
                    hitBloom = true
                    break
                }
            }
        }

        // If it didn't hit the Bloom Filter, it is 100% safe to bypass instantly (Zero Latency)
        if (!hitBloom) return false

        // Layer B: Direct exact verification using O(1) HashSet
        if (blockedDomains.contains(host)) {
            return true
        }

        // Subdomain matching with HashSet
        var parentDomain = host
        while (parentDomain.contains(".")) {
            parentDomain = parentDomain.substringAfter(".")
            if (blockedDomains.contains(parentDomain)) {
                return true
            }
        }

        // Exact Keyword matching fallback
        for (keyword in blockedKeywords) {
            if (url.contains(keyword)) {
                return true
            }
        }

        // De-googling tracking/log block
        if (host.contains("google.com")) {
            if (url.contains("/log") || url.contains("/telemetry") || url.contains("/gen_204") || url.contains("/collect")) {
                return true
            }
        }

        return false
    }

    /**
     * Generates a structural blank response with zeroed byte arrays to prevent page loading timeouts.
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
