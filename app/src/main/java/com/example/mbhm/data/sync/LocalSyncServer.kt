package com.example.mbhm.data.sync

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.mbhm.data.repository.AppRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class LocalSyncServer(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val appRepository: AppRepository,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The temporary token a peer must send while sharing is active. */
    @Volatile
    var pairingToken: String? = null
        private set

    val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    @Synchronized
    fun start(): Boolean {
        if (serverSocket != null && !serverSocket!!.isClosed) return true
        return try {
            val socket = ServerSocket(port)
            serverSocket = socket
            pairingToken = generateToken()
            serverJob = scope.launch {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        handleClient(client)
                    } catch (_: Exception) {
                        // Socket closed or a connection failed; keep accepting.
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start direct sync server", e)
            pairingToken = null
            false
        }
    }

    @Synchronized
    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            pairingToken = null
        } catch (e: Exception) {
            Log.e(TAG, "Unable to stop direct sync server", e)
        }
    }

    fun isRunning(): Boolean {
        return serverSocket != null && !serverSocket!!.isClosed
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                socket.soTimeout = SOCKET_TIMEOUT_MS
                val input = socket.getInputStream().buffered()
                val output = socket.getOutputStream()

                val requestLine = try {
                    readAsciiLine(input, MAX_LINE)
                } catch (_: RequestTooLargeException) {
                    sendResponse(output, 431, "Request Header Fields Too Large")
                    return@launch
                } ?: run {
                    socket.close()
                    return@launch
                }
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    sendResponse(output, 400, "Bad Request")
                    return@launch
                }

                val method = parts[0]
                val path = parts[1].substringBefore('?')
                val headers = try {
                    readHeaders(input)
                } catch (_: RequestTooLargeException) {
                    sendResponse(output, 431, "Request Header Fields Too Large")
                    return@launch
                }

                val expectedToken = pairingToken
                val suppliedToken = headers["x-pairing-token"]
                    ?: headers["authorization"]
                        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                        ?.substring(7)
                        ?.trim()
                if (expectedToken.isNullOrBlank() || !constantTimeEquals(suppliedToken, expectedToken)) {
                    sendResponse(output, 401, "Unauthorized")
                    return@launch
                }

                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength < 0 || contentLength > MAX_BODY) {
                    sendResponse(output, 413, "Payload Too Large")
                    return@launch
                }
                val body = if (contentLength > 0) {
                    val bytes = input.readNBytes(contentLength)
                    if (bytes.size != contentLength) {
                        sendResponse(output, 400, "Incomplete Request Body")
                        return@launch
                    }
                    String(bytes, StandardCharsets.UTF_8)
                } else {
                    ""
                }

                when {
                    method == "GET" && path == "/api/status" -> {
                        val resp = SyncStatusResponse(
                            status = "OK",
                            deviceName = deviceName,
                            serverTime = System.currentTimeMillis()
                        )
                        sendJsonResponse(output, 200, gson.toJson(resp))
                    }

                    method == "GET" && path == "/api/sync/pull" -> {
                        val payload = appRepository.exportSyncPayload(deviceName)
                        sendJsonResponse(output, 200, gson.toJson(payload))
                    }

                    method == "POST" && path == "/api/sync/push" -> {
                        val payload = runCatching {
                            gson.fromJson(body, SyncPayload::class.java)
                        }.getOrNull()
                        if (payload == null) {
                            sendJsonResponse(
                                output,
                                400,
                                gson.toJson(SyncResult(false, "Invalid sync payload"))
                            )
                            return@launch
                        }
                        val count = appRepository.importSyncPayload(payload)
                        val resp = SyncResult(
                            success = true,
                            message = "Sync successful",
                            itemsSynced = count
                        )
                        sendJsonResponse(output, 200, gson.toJson(resp))
                    }

                    else -> sendResponse(output, 404, "Not Found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct sync request failed", e)
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun readHeaders(input: InputStream): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        var totalBytes = 0
        var lineCount = 0
        while (true) {
            val line = readAsciiLine(input, MAX_LINE) ?: break
            totalBytes += line.length + 2
            lineCount++
            if (totalBytes > MAX_HEADER_BYTES || lineCount > MAX_HEADER_COUNT) {
                throw RequestTooLargeException()
            }
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase()] =
                    line.substring(separator + 1).trim()
            }
        }
        return headers
    }

    private fun readAsciiLine(input: InputStream, maxBytes: Int): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            if (buffer.size() >= maxBytes) throw RequestTooLargeException()
            val byte = input.read()
            if (byte == -1) {
                return if (buffer.size() == 0) null else buffer.toString(StandardCharsets.UTF_8.name())
            }
            if (byte == '\n'.code) {
                val bytes = buffer.toByteArray()
                val length = if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) {
                    bytes.size - 1
                } else {
                    bytes.size
                }
                return String(bytes, 0, length, StandardCharsets.UTF_8)
            }
            buffer.write(byte)
        }
    }

    private class RequestTooLargeException : Exception()

    private fun constantTimeEquals(left: String?, right: String): Boolean {
        if (left == null) return false
        return MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.UTF_8),
            right.toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 $statusCode ${reasonPhrase(statusCode)}\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendResponse(output: OutputStream, statusCode: Int, text: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 $statusCode ${reasonPhrase(statusCode)}\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun reasonPhrase(statusCode: Int): String = when (statusCode) {
        200 -> "OK"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        404 -> "Not Found"
        413 -> "Payload Too Large"
        431 -> "Request Header Fields Too Large"
        else -> "Error"
    }

    private companion object {
        const val TAG = "LocalSyncServer"
        const val MAX_BODY = 8 * 1024 * 1024
        const val SOCKET_TIMEOUT_MS = 10_000
        const val MAX_LINE = 8 * 1024
        const val MAX_HEADER_BYTES = 32 * 1024
        const val MAX_HEADER_COUNT = 100
    }
}
