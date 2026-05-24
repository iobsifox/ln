package com.example.proxy

import com.example.logs.LogRepository
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class Socks5ProxyServer(
    private val port: Int = 1080,
    private val allowLan: Boolean = false
) {
    private val TAG = "Socks5Proxy"
    private var serverSocket: ServerSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                val bindAddr = if (allowLan) "0.0.0.0" else "127.0.0.1"
                serverSocket = ServerSocket(port, 50, InetAddress.getByName(bindAddr))
                LogRepository.i(TAG, "SOCKS5 Proxy Server started on $bindAddr:$port")

                while (isActive && isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    LogRepository.e(TAG, "SOCKS5 Server error: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
        scope.cancel()
        LogRepository.i(TAG, "SOCKS5 Proxy Server stopped")
    }

    private suspend fun handleClient(clientSocket: Socket) {
        val clientIn = clientSocket.getInputStream()
        val clientOut = clientSocket.getOutputStream()

        try {
            // 1. Handshake Greeting
            // Read version (1 byte) + number of methods (1 byte)
            val ver = clientIn.read()
            if (ver != 0x05) {
                clientSocket.close()
                return
            }

            val numMethods = clientIn.read()
            if (numMethods <= 0) {
                clientSocket.close()
                return
            }

            val methods = ByteArray(numMethods)
            var bytesRead = clientIn.read(methods)
            if (bytesRead < numMethods) {
                clientSocket.close()
                return
            }

            // Respond with No Authentication Required (0x05, 0x00)
            clientOut.write(byteArrayOf(0x05, 0x00))
            clientOut.flush()

            // 2. Read SOCKS Request
            // 1 byte: Version (0x05)
            // 1 byte: Command (0x01 = CONNECT, 0x02 = BIND, 0x03 = UDP ASSOCIATE)
            // 1 byte: Reserved (0x00)
            // 1 byte: Address Type (0x01 = IPv4, 0x03 = Domain Name, 0x04 = IPv6)
            val reqVer = clientIn.read()
            val cmd = clientIn.read()
            val rsv = clientIn.read()
            val atyp = clientIn.read()

            if (reqVer != 0x05 || cmd != 0x01) {
                // Return Command Not Supported (0x07) or general failure
                sendReply(clientOut, 0x07)
                clientSocket.close()
                return
            }

            var destHost = ""
            when (atyp) {
                0x01 -> { // IPv4
                    val ipv4Bytes = ByteArray(4)
                    clientIn.read(ipv4Bytes)
                    destHost = InetAddress.getByAddress(ipv4Bytes).hostAddress ?: ""
                }
                0x03 -> { // Domain name
                    val len = clientIn.read()
                    if (len <= 0) {
                        clientSocket.close()
                        return
                    }
                    val domainBytes = ByteArray(len)
                    clientIn.read(domainBytes)
                    destHost = String(domainBytes, Charsets.US_ASCII)
                }
                0x04 -> { // IPv6
                    val ipv6Bytes = ByteArray(16)
                    clientIn.read(ipv6Bytes)
                    destHost = InetAddress.getByAddress(ipv6Bytes).hostAddress ?: ""
                }
                else -> {
                    sendReply(clientOut, 0x08) // Address type not supported
                    clientSocket.close()
                    return
                }
            }

            // Read 2-byte Port (Big-Endian)
            val portHigh = clientIn.read()
            val portLow = clientIn.read()
            val destPort = (portHigh shl 8) or portLow

            LogRepository.i(TAG, "SOCKS5 Tunnel to $destHost:$destPort requested")

            var targetSocket: Socket? = null
            try {
                withContext(Dispatchers.IO) {
                    targetSocket = Socket(destHost, destPort)
                }
            } catch (e: Exception) {
                LogRepository.e(TAG, "Failed SOCKS5 target connection to $destHost:$destPort: ${e.message}")
                sendReply(clientOut, 0x04) // Host unreachable
                clientSocket.close()
                return
            }

            // SOCKS Successful Connection response
            sendReply(clientOut, 0x00)

            val targetIn = targetSocket!!.getInputStream()
            val targetOut = targetSocket!!.getOutputStream()

            // Tunnel streams bi-directionally
            coroutineScope {
                val clientToTarget = launch(Dispatchers.IO) {
                    copyStream(clientIn, targetOut)
                }
                val targetToClient = launch(Dispatchers.IO) {
                    copyStream(targetIn, clientOut)
                }
                clientToTarget.join()
                targetToClient.join()
            }

            targetSocket?.close()
        } catch (e: Exception) {
            // handle error
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) {
                // ignored
            }
        }
    }

    private fun sendReply(output: OutputStream, status: Byte) {
        val reply = ByteArray(10)
        reply[0] = 0x05 // Ver
        reply[1] = status // Status Rep
        reply[2] = 0x00 // Rsv
        reply[3] = 0x01 // ATYP (IPv4 = 1)
        // Bind address 0.0.0.0:0
        reply[4] = 0x00
        reply[5] = 0x00
        reply[6] = 0x00
        reply[7] = 0x00
        reply[8] = 0x00
        reply[9] = 0x00
        output.write(reply)
        output.flush()
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        try {
            while (true) {
                bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (e: Exception) {
            // connection reset/finished
        }
    }
}
