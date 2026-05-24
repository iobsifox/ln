package com.example

import com.example.data.AppSettings
import com.example.data.ConnectionMode
import com.example.dns.DnsWire
import com.example.dns.DohClient
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDefaultSettingsAndSerialization() {
        val settings = AppSettings()
        // Ensure defaults match design specifications
        assertEquals(ConnectionMode.DOH_DIRECT, settings.connectionMode)
        assertEquals("cloudflare", settings.dohDirectProvider)
        assertTrue(settings.localHttpProxyEnabled)
        assertTrue(settings.localSocks5Enabled)
        assertFalse(settings.lanShareEnabled)
        assertFalse(settings.vpnEnabled)
        assertFalse(settings.externalCoresEnabled)
        assertTrue(settings.selectedTunnelApps.isEmpty())
        assertTrue(settings.selectedBypassApps.isEmpty())
    }

    @Test
    fun testDohEndpointSelection() {
        // Ensure all built-in DNS-Over-HTTPS providers listed are verified
        assertTrue(DohClient.providers.containsKey("cloudflare"))
        assertTrue(DohClient.providers.containsKey("google"))
        assertTrue(DohClient.providers.containsKey("quad9"))
        assertTrue(DohClient.providers.containsKey("adguard"))

        assertEquals("https://cloudflare-dns.com/dns-query", DohClient.providers["cloudflare"])
        assertEquals("https://dns.google/dns-query", DohClient.providers["google"])
        assertEquals("https://dns.quad9.net/dns-query", DohClient.providers["quad9"])
        assertEquals("https://dns.adguard-dns.com/dns-query", DohClient.providers["adguard"])
    }

    @Test
    fun testDnsWireFormatQueryBuilder() {
        // Build query for "google.com" Type A
        val query = DnsWire.buildQuery("google.com", DnsWire.TYPE_A)
        
        // Header must be at least 12 bytes
        assertTrue(query.size >= 12)
        
        // Check standard header properties: Questions == 1 (bytes indices 4 and 5)
        assertEquals(0, query[4].toInt())
        assertEquals(1, query[5].toInt())
        
        // Verify label serialization for "google.com" -> 6 'google' 3 'com' 0 at offset 12
        assertEquals(6, query[12].toInt()) // Length of label 'google'
        val googleStr = String(query, 13, 6, Charsets.US_ASCII)
        assertEquals("google", googleStr)
        
        assertEquals(3, query[19].toInt()) // Length of label 'com'
        val comStr = String(query, 20, 3, Charsets.US_ASCII)
        assertEquals("com", comStr)
        
        assertEquals(0, query[23].toInt()) // Null terminator
    }

    @Test
    fun testDnsWireResponseParser() {
        // Construct a real mock binary response packet containing:
        // - 12-byte header (ID, Flags, QD=1, AN=1, NS=0, AR=0)
        // - 12-byte Question ("google.com" serialized)
        // - 16-byte Answer (A record resolving to 1.2.3.4)
        val response = ByteArray(44)
        
        // Header
        response[0] = 0x12; response[1] = 0x34 // Tx ID
        response[2] = 0x81.toByte(); response[3] = 0x80.toByte() // Flags
        response[4] = 0x00; response[5] = 0x01 // QDCount = 1
        response[6] = 0x00; response[7] = 0x01 // ANCount = 1
        // rest NS, AR are 0
        
        // Question ("google.com" labels: 6,'g','o','o','g','l','e',3,'c','o','m', 0)
        response[12] = 6
        System.arraycopy("google".toByteArray(Charsets.US_ASCII), 0, response, 13, 6)
        response[19] = 3
        System.arraycopy("com".toByteArray(Charsets.US_ASCII), 0, response, 20, 3)
        response[23] = 0 // terminating
        
        // Query Type A, Class IN
        response[24] = 0x00; response[25] = 0x01
        response[26] = 0x00; response[27] = 0x01

        // Answer
        // Pointer to name at offset 12 (0xC00C)
        response[28] = 0xC0.toByte(); response[29] = 0x0C
        // Type A (0x0001)
        response[30] = 0x00; response[31] = 0x01
        // Class IN (0x0001)
        response[32] = 0x00; response[33] = 0x01
        // TTL (0x000000AC)
        response[34] = 0; response[35] = 0; response[36] = 0; response[37] = 0xAC.toByte()
        // RDLength = 4
        response[38] = 0x00; response[39] = 0x04
        // RData IP = 1.2.3.4
        response[40] = 1; response[41] = 2; response[42] = 3; response[43] = 4

        val parsedIps = DnsWire.parseResponse(response)
        assertEquals(1, parsedIps.size)
        assertEquals("1.2.3.4", parsedIps[0])
    }
}
