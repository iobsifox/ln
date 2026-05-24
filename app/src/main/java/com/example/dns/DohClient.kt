package com.example.dns

import com.example.logs.LogRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

object DohClient {
    private const val TAG = "DohClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val providers = mapOf(
        "cloudflare" to "https://cloudflare-dns.com/dns-query",
        "google" to "https://dns.google/dns-query",
        "quad9" to "https://dns.quad9.net/dns-query",
        "adguard" to "https://dns.adguard-dns.com/dns-query"
    )

    /**
     * Resolves a host name using DoH Direct modality (A and AAAA queries in parallel or sequence)
     */
    suspend fun resolveDirect(domain: String, providerKey: String): List<String> {
        val url = providers[providerKey] ?: providers["cloudflare"]!!
        LogRepository.i(TAG, "Resolving Direct: $domain using provider: $providerKey ($url)")
        
        // Return from cache if we have a valid entry
        DnsCache.get(domain, DnsWire.TYPE_A)?.let {
            LogRepository.d(TAG, "Cache Hit (A): $domain -> $it")
            return it
        }

        val ips = mutableListOf<String>()
        try {
            val queryBytes = DnsWire.buildQuery(domain, DnsWire.TYPE_A)
            val responseBytes = postDnsQuery(url, queryBytes)
            val results = DnsWire.parseResponse(responseBytes)
            if (results.isNotEmpty()) {
                ips.addAll(results)
                DnsCache.put(domain, DnsWire.TYPE_A, results)
                LogRepository.i(TAG, "Resolved (A): $domain -> $results")
            } else {
                LogRepository.w(TAG, "No A records found for $domain")
            }
        } catch (e: Exception) {
            LogRepository.e(TAG, "Error resolving direct A for $domain: ${e.message}")
        }

        return ips
    }

    /**
     * Resolves a host name using a custom worker proxy query
     */
    suspend fun resolveWorker(domain: String, workerUrl: String, upstreamKey: String): List<String> {
        if (workerUrl.isBlank()) {
            LogRepository.e(TAG, "DoH Worker URL is empty. Falling back to Google Direct.")
            return resolveDirect(domain, "google")
        }

        val upstreamParam = upstreamKey.ifBlank { "cloudflare" }
        // Worker URL structure: {workerUrl}?upstream={upstream}
        val url = if (workerUrl.contains("?")) {
            "$workerUrl&upstream=$upstreamParam"
        } else {
            "$workerUrl?upstream=$upstreamParam"
        }

        LogRepository.i(TAG, "Resolving Worker: $domain via: $url")

        DnsCache.get(domain, DnsWire.TYPE_A)?.let {
            LogRepository.d(TAG, "Cache Hit (A): $domain -> $it")
            return it
        }

        val ips = mutableListOf<String>()
        try {
            val queryBytes = DnsWire.buildQuery(domain, DnsWire.TYPE_A)
            val responseBytes = postDnsQuery(url, queryBytes)
            val results = DnsWire.parseResponse(responseBytes)
            if (results.isNotEmpty()) {
                ips.addAll(results)
                DnsCache.put(domain, DnsWire.TYPE_A, results)
                LogRepository.i(TAG, "Resolved (A via Worker): $domain -> $results")
            } else {
                LogRepository.w(TAG, "No worker-side A records for $domain")
            }
        } catch (e: Exception) {
            LogRepository.e(TAG, "Error resolving worker A for $domain: ${e.message}")
        }

        return ips
    }

    private fun postDnsQuery(url: String, queryBytes: ByteArray): ByteArray {
        val mediaType = "application/dns-message".toMediaType()
        val body = queryBytes.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/dns-message")
            .header("Accept", "application/dns-message")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP response code: ${response.code}")
            }
            val resBody = response.body ?: throw IOException("Empty response body")
            return resBody.bytes()
        }
    }
}
