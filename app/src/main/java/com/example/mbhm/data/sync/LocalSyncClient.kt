package com.example.mbhm.data.sync

import android.os.Build
import com.example.mbhm.data.repository.AppRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LocalSyncClient(
    private val appRepository: AppRepository
) {
    private val gson = Gson()
    private val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    suspend fun checkHostStatus(
        hostIp: String,
        token: String,
        port: Int = 8080
    ): SyncStatusResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$hostIp:$port/api/status")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val responseText = reader.use { it.readText() }
                return@withContext gson.fromJson(responseText, SyncStatusResponse::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun syncWithHost(
        hostIp: String,
        token: String,
        port: Int = 8080
    ): SyncResult = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext SyncResult(false, "Enter the host's pairing token")
        }
        var itemsImported = 0
        try {
            // Step 1: Pull payload from Host
            val pullUrl = URL("http://$hostIp:$port/api/sync/pull")
            val pullConn = pullUrl.openConnection() as HttpURLConnection
            pullConn.requestMethod = "GET"
            pullConn.connectTimeout = 5000
            pullConn.readTimeout = 5000
            pullConn.setRequestProperty("Authorization", "Bearer $token")

            if (pullConn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(pullConn.inputStream))
                val jsonText = reader.use { it.readText() }
                val hostPayload = gson.fromJson(jsonText, SyncPayload::class.java)
                if (hostPayload != null) {
                    itemsImported += appRepository.importSyncPayload(hostPayload)
                }
            } else {
                return@withContext SyncResult(
                    false,
                    if (pullConn.responseCode == 401) {
                        "Host rejected the pairing token"
                    } else {
                        "Failed to pull data from Host (HTTP ${pullConn.responseCode})"
                    }
                )
            }

            // Step 2: Push local payload to Host
            val localPayload = appRepository.exportSyncPayload(deviceName)
            val requestBody = gson.toJson(localPayload).toByteArray(Charsets.UTF_8)
            val pushUrl = URL("http://$hostIp:$port/api/sync/push")
            val pushConn = pushUrl.openConnection() as HttpURLConnection
            pushConn.requestMethod = "POST"
            pushConn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            pushConn.setRequestProperty("Authorization", "Bearer $token")
            pushConn.doOutput = true
            pushConn.setFixedLengthStreamingMode(requestBody.size)
            pushConn.connectTimeout = 5000
            pushConn.readTimeout = 5000

            pushConn.outputStream.use { it.write(requestBody) }

            if (pushConn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(pushConn.inputStream))
                val jsonText = reader.use { it.readText() }
                val pushResult = gson.fromJson(jsonText, SyncResult::class.java)
                itemsImported += pushResult?.itemsSynced ?: 0
                return@withContext SyncResult(true, "Successfully synchronized with $hostIp", itemsImported)
            } else {
                return@withContext SyncResult(
                    false,
                    if (pushConn.responseCode == 401) {
                        "Host rejected the pairing token"
                    } else {
                        "Failed to push data to Host (HTTP ${pushConn.responseCode})"
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext SyncResult(false, "Sync failed: ${e.localizedMessage ?: e.message}")
        }
    }
}
