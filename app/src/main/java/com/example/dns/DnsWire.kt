package com.example.dns

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.security.SecureRandom
import kotlin.experimental.and

object DnsWire {

    private val random = SecureRandom()

    // Query types
    const val TYPE_A = 1
    const val TYPE_AAAA = 28
    const val CLASS_IN = 1

    /**
     * Builds a binary DNS query for a given domain and record type (A = 1, AAAA = 28)
     */
    fun buildQuery(domain: String, type: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        
        // Header
        // 1. Transaction ID: 2 bytes
        val txId = ByteArray(2)
        random.nextBytes(txId)
        baos.write(txId)

        // 2. Flags: 2 bytes (Standard query with recursion desired, 0x0100)
        baos.write(0x01)
        baos.write(0x00)

        // 3. Questions Count: 2 bytes (1 query = 0x0001)
        baos.write(0x00)
        baos.write(0x01)

        // 4. Answer RRs: 2 bytes (0 = 0x0000)
        baos.write(0x00)
        baos.write(0x00)

        // 5. Authority RRs: 2 bytes (0 = 0x0000)
        baos.write(0x00)
        baos.write(0x00)

        // 6. Additional RRs: 2 bytes (0 = 0x0000)
        baos.write(0x00)
        baos.write(0x00)

        // Question section
        // 1. Domain labels: e.g., "www.example.com" -> [3, 'w', 'w', 'w', 7, 'e', 'x', 'a', 'm', 'p', 'l', 'e', 3, 'c', 'o', 'm', 0]
        val labels = domain.split(".")
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            baos.write(bytes.size)
            baos.write(bytes)
        }
        baos.write(0x00) // End of domain

        // 2. Type: 2 bytes (A = 0x0001 or AAAA = 0x001C)
        baos.write((type ushr 8) and 0xFF)
        baos.write(type and 0xFF)

        // 3. Class: 2 bytes (IN = 0x0001)
        baos.write((CLASS_IN ushr 8) and 0xFF)
        baos.write(CLASS_IN and 0xFF)

        return baos.toByteArray()
    }

    /**
     * Parses a binary DNS response and returns a list of resolved IP strings.
     */
    fun parseResponse(response: ByteArray): List<String> {
        val resolvedIps = mutableListOf<String>()
        if (response.size < 12) return resolvedIps

        var idx = 0
        
        // Skip header transaction ID, flags
        idx += 4
        
        // Read question count and answer count
        val qdCount = ((response[idx].toInt() and 0xFF) shl 8) or (response[idx + 1].toInt() and 0xFF)
        idx += 2
        val anCount = ((response[idx].toInt() and 0xFF) shl 8) or (response[idx + 1].toInt() and 0xFF)
        idx += 2
        
        // Skip NS section size and Add section size
        idx += 4

        // Skip Questions
        for (i in 0 until qdCount) {
            idx = skipName(response, idx)
            idx += 4 // Skip Type and Class
        }

        // Parse Answers
        for (i in 0 until anCount) {
            if (idx >= response.size) break
            
            // Skip answer name (pointer or label sequence)
            idx = skipName(response, idx)
            if (idx + 10 > response.size) break

            val type = ((response[idx].toInt() and 0xFF) shl 8) or (response[idx + 1].toInt() and 0xFF)
            idx += 2
            
            val cls = ((response[idx].toInt() and 0xFF) shl 8) or (response[idx + 1].toInt() and 0xFF)
            idx += 2
            
            val ttl = ((response[idx].toInt() and 0xFF) shl 24) or 
                      ((response[idx + 1].toInt() and 0xFF) shl 16) or 
                      ((response[idx + 2].toInt() and 0xFF) shl 8) or 
                      (response[idx + 3].toInt() and 0xFF)
            idx += 4
            
            val rdLength = ((response[idx].toInt() and 0xFF) shl 8) or (response[idx + 1].toInt() and 0xFF)
            idx += 2

            if (idx + rdLength > response.size) break

            if (type == TYPE_A && rdLength == 4) {
                val ipBytes = ByteArray(4)
                System.arraycopy(response, idx, ipBytes, 0, 4)
                val ipAddr = InetAddress.getByAddress(ipBytes).hostAddress
                if (ipAddr != null) {
                    resolvedIps.add(ipAddr)
                }
            } else if (type == TYPE_AAAA && rdLength == 16) {
                val ipBytes = ByteArray(16)
                System.arraycopy(response, idx, ipBytes, 0, 16)
                val ipAddr = InetAddress.getByAddress(ipBytes).hostAddress
                if (ipAddr != null) {
                    resolvedIps.add(ipAddr)
                }
            }

            idx += rdLength
        }

        return resolvedIps
    }

    private fun skipName(response: ByteArray, index: Int): Int {
        var idx = index
        while (idx < response.size) {
            val b = response[idx].toInt() and 0xFF
            if (b == 0) {
                idx++
                break
            }
            if ((b and 0xC0) == 0xC0) {
                idx += 2 // Compressed pointer is 2 bytes long
                break
            } else {
                idx += 1 + b // Skip label and its content
            }
        }
        return idx
    }
}
