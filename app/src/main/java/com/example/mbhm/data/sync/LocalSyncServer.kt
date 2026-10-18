package com.example.mbhm.data.sync

import android.content.Context
import android.os.Build
import com.example.mbhm.data.repository.AppRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class LocalSyncServer(
    private val context: Context,
    private val appRepository: AppRepository,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    val deviceName: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    @Synchronized
    fun start(): Boolean {
        if (serverSocket != null && !serverSocket!!.isClosed) return true
        return try {
            serverSocket = ServerSocket(port)
            serverJob = scope.launch {
                while (serverSocket?.isClosed == false) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        handleClient(socket)
                    } catch (e: Exception) {
                        // Socket closed or connection error
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isRunning(): Boolean {
        return serverSocket != null && !serverSocket!!.isClosed
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = socket.getOutputStream()

                val requestLine = reader.readLine() ?: run {
                    socket.close()
                    return@launch
                }

                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    sendResponse(output, 400, "Bad Request")
                    socket.close()
                    return@launch
                }

                val method = parts[0]
                val path = parts[1]

                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                    if (line!!.lowercase().startsWith("content-length:")) {
                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                var body = ""
                if (contentLength > 0) {
                    val charArray = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(charArray, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    body = String(charArray, 0, read)
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
                        val payload = gson.fromJson(body, SyncPayload::class.java)
                        val count = if (payload != null) {
                            appRepository.importSyncPayload(payload)
                        } else 0
                        val resp = SyncResult(
                            success = true,
                            message = "Sync successful",
                            itemsSynced = count
                        )
                        sendJsonResponse(output, 200, gson.toJson(resp))
                    }
                    else -> {
                        sendResponse(output, 404, "Not Found")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendResponse(output: OutputStream, statusCode: Int, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $text\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }
}
