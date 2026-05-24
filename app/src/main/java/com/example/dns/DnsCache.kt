package com.example.dns

import java.util.concurrent.ConcurrentHashMap

object DnsCache {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    data class CacheEntry(
        val ipList: List<String>,
        val expiresAt: Long
    )

    fun get(domain: String, type: Int): List<String>? {
        val key = "$domain|$type"
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }
        return entry.ipList
    }

    fun put(domain: String, type: Int, ips: List<String>, ttlSeconds: Long = 360) {
        val key = "$domain|$type"
        val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000)
        cache[key] = CacheEntry(ips, expiresAt)
    }

    fun clear() {
        cache.clear()
    }
}
