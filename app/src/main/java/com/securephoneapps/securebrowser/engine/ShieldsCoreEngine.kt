package com.securephoneapps.securebrowser.engine

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream

class ShieldsCoreEngine {

    companion object {
        val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
        )
    }

    private var uaIndex = 0

    fun getRandomizedUserAgent(): String {
        synchronized(this) {
            val ua = USER_AGENTS[uaIndex]
            uaIndex = (uaIndex + 1) % USER_AGENTS.size
            return ua
        }
    }

    private val trackingParameters = setOf("fbclid", "utm_source", "utm_medium", "gclid", "utm_campaign", "utm_term", "utm_content", "utm_id", "gclsrc")

    fun stripTrackingParameters(url: String): String {
        try {
            val queryStart = url.indexOf('?')
            if (queryStart == -1) return url
            
            val baseUrl = url.substring(0, queryStart)
            val queryAndHash = url.substring(queryStart + 1)
            
            val hashStart = queryAndHash.indexOf('#')
            val queryString = if (hashStart != -1) queryAndHash.substring(0, hashStart) else queryAndHash
            val fragment = if (hashStart != -1) queryAndHash.substring(hashStart) else ""
            
            if (queryString.isEmpty()) return url
            
            val cleanedParams = queryString.split('&')
                .filter { param ->
                    val eqIdx = param.indexOf('=')
                    val key = if (eqIdx != -1) param.substring(0, eqIdx) else param
                    !trackingParameters.contains(key.trim().lowercase())
                }
            
            val newQuery = if (cleanedParams.isNotEmpty()) {
                "?" + cleanedParams.joinToString("&")
            } else {
                ""
            }
            
            return baseUrl + newQuery + fragment
        } catch (e: Exception) {
            return url
        }
    }

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
        "adcolony.com",
        "adnxs.com",
        "advertising.com",
        "amazon-adsystem.com",
        "applovin.com",
        "chartboost.com",
        "criteo.com",
        "facebook.net",
        "fbcdn.net",
        "moatads.com",
        "mopub.com",
        "outbrain.com",
        "pubmatic.com",
        "quantserve.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "taboola.com",
        "unity3d.com",
        "vungle.com"
    )

    private val blockedKeywords = hashSetOf(
        "analytics", "telemetry", "tracker", "adsystem", "spyware", "adserver", "metrics", "marketing"
    )

    // Highly optimized primitive Bloom Filter
    private val bloomFilterSize = 16384
    private val bloomBitSet = LongArray(bloomFilterSize / 64)

    init {
        blockedDomains.forEach { addToBloom(it) }
        blockedKeywords.forEach { addToBloom(it) }
    }

    private fun murmurHash3(data: String, seed: Int): Int {
        val bytes = data.toByteArray(Charsets.UTF_8)
        var h1 = seed
        val c1 = 0xcc9e2d51.toInt()
        val c2 = 0x1b873593
        val len = bytes.size

        for (i in 0 until len / 4) {
            var k1 = (bytes[i * 4].toInt() and 0xff) or ((bytes[i * 4 + 1].toInt() and 0xff) shl 8) or ((bytes[i * 4 + 2].toInt() and 0xff) shl 16) or ((bytes[i * 4 + 3].toInt() and 0xff) shl 24)
            k1 *= c1
            k1 = (k1 shl 15) or (k1 ushr 17)
            k1 *= c2
            h1 = h1 xor k1
            h1 = (h1 shl 13) or (h1 ushr 19)
            h1 = h1 * 5 + 0xe6546b64.toInt()
        }
        return h1
    }

    private fun addToBloom(item: String) {
        val h1 = murmurHash3(item, 0x12345678)
        val h2 = murmurHash3(item, 0x9abcdef0.toInt())
        val bit1 = (h1 and 0x7FFFFFFF) % bloomFilterSize
        val bit2 = (h2 and 0x7FFFFFFF) % bloomFilterSize
        bloomBitSet[bit1 / 64] = bloomBitSet[bit1 / 64] or (1L shl (bit1 % 64))
        bloomBitSet[bit2 / 64] = bloomBitSet[bit2 / 64] or (1L shl (bit2 % 64))
    }

    private fun checkBloom(item: String): Boolean {
        val h1 = murmurHash3(item, 0x12345678)
        val h2 = murmurHash3(item, 0x9abcdef0.toInt())
        val bit1 = (h1 and 0x7FFFFFFF) % bloomFilterSize
        val bit2 = (h2 and 0x7FFFFFFF) % bloomFilterSize
        return (bloomBitSet[bit1 / 64] and (1L shl (bit1 % 64))) != 0L && (bloomBitSet[bit2 / 64] and (1L shl (bit2 % 64))) != 0L
    }

    fun importCompressedRuleset(input: InputStream) {
        synchronized(this) {
            try {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(input, Charsets.UTF_8))
                reader.lineSequence().forEach { line ->
                    val clean = line.trim().removePrefix("||").substringBefore("^").substringBefore("/").lowercase()
                    if (clean.isNotEmpty()) {
                        if (clean.contains(".")) blockedDomains.add(clean) else blockedKeywords.add(clean)
                        addToBloom(clean)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun shouldBlock(url: String): Boolean {
        val host = extractHost(url) ?: return false
        
        // Intranet detection matching filters
        if (host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) return true

        synchronized(this) {
            if (checkBloom(host)) {
                if (blockedDomains.contains(host)) return true
                var parent = host
                while (parent.contains(".")) {
                    parent = parent.substringAfter(".")
                    if (blockedDomains.contains(parent)) return true
                }
            }
            return blockedKeywords.any { url.contains(it) }
        }
    }

    fun generateBlankResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }

    fun isMaliciousHost(host: String): Boolean {
        return blockedDomains.contains(host)
    }

    private fun extractHost(url: String): String? {
        return try {
            val doubleSlash = url.indexOf("//")
            val start = if (doubleSlash != -1) doubleSlash + 2 else 0
            var end = url.indexOf('/', start)
            if (end == -1) end = url.length
            val host = url.substring(start, end).lowercase()
            if (host.contains(":")) host.substringBefore(":") else host
        } catch (e: Exception) { null }
    }

    fun obfuscateHeaderSequence(headers: Map<String, String>): Map<String, String> {
        val result = headers.toMutableMap()
        val keys = result.keys.toList().shuffled()
        val newMap = LinkedHashMap<String, String>()
        for (key in keys) {
            newMap[key] = result[key] ?: ""
        }
        return newMap
    }
}
