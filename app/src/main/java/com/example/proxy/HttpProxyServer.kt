package com.example.proxy

import com.example.logs.LogRepository
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class HttpProxyServer(
    private val port: Int = 8080,
    private val allowLan: Boolean = false
) {
    private val TAG = "HttpProxy"
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
                serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName(bindAddr))
                LogRepository.i(TAG, "HTTP Proxy Server started on $bindAddr:$port")

                while (isActive && isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    LogRepository.e(TAG, "HTTP Server Error: ${e.message}")
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
        LogRepository.i(TAG, "HTTP Proxy Server stopped")
    }

    private suspend fun handleClient(clientSocket: Socket) {
        val clientIn = clientSocket.getInputStream()
        val clientOut = clientSocket.getOutputStream()

        try {
            // Read first line of request
            val headerLine = readLine(clientIn) ?: return
            if (headerLine.isBlank()) return

            LogRepository.d(TAG, "Request: $headerLine")
            val parts = headerLine.split(" ")
            if (parts.size < 2) {
                clientSocket.close()
                return
            }

            val method = parts[0]
            val uri = parts[1]

            if (method.uppercase() == "CONNECT") {
                // CONNECT domain.com:443 HTTP/1.1
                val hostParts = uri.split(":")
                val host = hostParts[0]
                val targetPort = if (hostParts.size > 1) hostParts[1].toInt() else 443

                LogRepository.i(TAG, "CONNECT Tunnel to $host:$targetPort requested")

                var targetSocket: Socket? = null
                try {
                    withContext(Dispatchers.IO) {
                        targetSocket = Socket(host, targetPort)
                    }
                } catch (e: Exception) {
                    LogRepository.e(TAG, "Failed to connect to target $host:$targetPort: ${e.message}")
                    clientOut.write("HTTP/1.1 502 Bad Gateway\r\n\r\n".toByteArray())
                    clientSocket.close()
                    return
                }

                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                clientOut.flush()

                val targetIn = targetSocket!!.getInputStream()
                val targetOut = targetSocket!!.getOutputStream()

                // Tunnel bidirectionally
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
            } else {
                // Regular GET/POST absolute URL proxy request
                // e.g. "http://example.com/index.html"
                var rawHost = ""
                var targetPort = 80
                var parsedUri = uri

                if (uri.startsWith("http://", ignoreCase = true)) {
                    val cleanUri = uri.substring(7)
                    val slashIdx = cleanUri.indexOf('/')
                    val hostPart = if (slashIdx != -1) cleanUri.substring(0, slashIdx) else cleanUri
                    parsedUri = if (slashIdx != -1) cleanUri.substring(slashIdx) else "/"
                    
                    if (hostPart.contains(":")) {
                        val hostSlashParts = hostPart.split(":")
                        rawHost = hostSlashParts[0]
                        targetPort = hostSlashParts[1].toInt()
                    } else {
                        rawHost = hostPart
                    }
                }

                if (rawHost.isBlank()) {
                    clientSocket.close()
                    return
                }

                var targetSocket: Socket? = null
                try {
                    withContext(Dispatchers.IO) {
                        targetSocket = Socket(rawHost, targetPort)
                    }
                } catch (e: Exception) {
                    clientSocket.close()
                    return
                }

                val targetOut = targetSocket!!.getOutputStream()
                val targetIn = targetSocket!!.getInputStream()

                // Forward original line with modified internal target url if needed, followed by remaining headers
                val newHeaderLine = "$method $parsedUri ${parts[2]}\r\n"
                targetOut.write(newHeaderLine.toByteArray())
                
                // Copy rest of request headers from client to target
                copyHeaders(clientIn, targetOut)
                targetOut.flush()

                // Tunnel bidirectionally
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
            }
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

    private fun readLine(inputStream: InputStream): String? {
        val baos = java.io.ByteArrayOutputStream()
        var b: Int
        while (true) {
            b = inputStream.read()
            if (b == -1) break
            if (b == '\n'.code) break
            if (b != '\r'.code) {
                baos.write(b)
            }
        }
        return if (baos.size() == 0 && b == -1) null else baos.toString("UTF-8")
    }

    private fun copyHeaders(clientIn: InputStream, targetOut: OutputStream) {
        var line: String?
        while (true) {
            line = readLine(clientIn)
            if (line == null || line.isBlank()) {
                targetOut.write("\r\n".toByteArray())
                break
            }
            targetOut.write((line + "\r\n").toByteArray())
        }
    }

    private fun copyStream(input: InputStream, output: java.io.OutputStream) {
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
            // Socket closed or idle timeout
        }
    }
}
