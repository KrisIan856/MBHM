package com.example.mbhm.data.sync

import com.example.mbhm.data.repository.AppRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val SYNC_SERVER_PORT = 3000

class RemoteSyncClient(
    private val appRepository: AppRepository,
    private val defaultPort: Int = SYNC_SERVER_PORT
) {
    private val gson = Gson()

    fun baseUrl(host: String): String {
        val trimmed = host.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        val scheme = if (trimmed.startsWith("https://", ignoreCase = true)) "https" else "http"
        val cleaned = if (trimmed.contains("://")) trimmed.substringAfter("://") else trimmed
        if (cleaned.isEmpty()) return ""
        val port = if (cleaned.count { it == ':' } == 0) {
            if (scheme == "https") 443 else defaultPort
        } else {
            null
        }
        val withPort = if (port == null) cleaned else "$cleaned:$port"
        return "$scheme://$withPort"
    }

    suspend fun status(host: String, token: String): ServerStatusResponse? = withContext(Dispatchers.IO) {
        val base = baseUrl(host)
        if (base.isEmpty() || token.isBlank()) return@withContext null
        try {
            val conn = open("$base/api/status", "GET", token)
            if (conn.responseCode == 200) {
                gson.fromJson(readAll(conn), ServerStatusResponse::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sync(host: String, token: String, clientId: String, since: Long): RemoteSyncOutcome =
        withContext(Dispatchers.IO) {
            val base = baseUrl(host)
            if (base.isEmpty()) {
                return@withContext RemoteSyncOutcome(false, "Enter the server address first")
            }
            if (token.isBlank()) {
                return@withContext RemoteSyncOutcome(false, "Enter the server access token")
            }

            val payload = appRepository.exportSyncPayload(NetworkUtils.deviceName())
            val sent = payload.totalRecords()

            val pushed = try {
                val body = gson.toJson(SyncPushRequest(clientId = clientId, payload = payload))
                val conn = open("$base/api/push", "POST", token, hasBody = true)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
                if (conn.responseCode == 200) {
                    gson.fromJson(readAll(conn), SyncPushResponse::class.java)
                } else {
                    return@withContext RemoteSyncOutcome(
                        false,
                        rejectedMessage(conn.responseCode),
                        sent = sent,
                        serverVersion = since
                    )
                }
            } catch (e: Exception) {
                return@withContext RemoteSyncOutcome(
                    false,
                    "Cannot reach server at $base",
                    sent = sent,
                    serverVersion = since
                )
            }

            val pulled = try {
                val conn = open("$base/api/pull?since=$since", "GET", token)
                if (conn.responseCode == 200) {
                    gson.fromJson(readAll(conn), RemotePullResponse::class.java)
                } else null
            } catch (e: Exception) {
                null
            }

            if (pulled == null) {
                return@withContext RemoteSyncOutcome(
                    false,
                    "Uploaded, but download from server failed",
                    sent = sent,
                    serverVersion = pushed?.version ?: since
                )
            }

            var received = 0
            var removed = 0
            pulled.upserts?.let { incoming ->
                received = appRepository.importSyncPayload(incoming)
            }
            pulled.deletes?.let { deletes ->
                removed = deletes.values.sumOf { it.size }
                appRepository.deleteSyncedRecords(deletes)
            }

            val displayHost = base.removePrefix("https://").removePrefix("http://")
            RemoteSyncOutcome(
                success = true,
                message = "Synced with $displayHost",
                sent = sent,
                received = received,
                removed = removed,
                serverVersion = pulled.version
            )
        }

    private fun rejectedMessage(statusCode: Int): String = when (statusCode) {
        401, 403 -> "Server rejected the access token"
        413 -> "The sync payload is too large for the server"
        else -> "Server rejected the upload (HTTP $statusCode)"
    }

    private fun open(
        url: String,
        method: String,
        token: String,
        hasBody: Boolean = false
    ): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 4000
        conn.readTimeout = 20000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (hasBody) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return conn
    }

    private fun readAll(conn: HttpURLConnection): String {
        return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
    }
}

private fun SyncPayload.totalRecords(): Int =
    users.orEmpty().size +
        boarders.orEmpty().size +
        paymentRecords.orEmpty().size +
        attendanceSessions.orEmpty().size +
        attendanceRecords.orEmpty().size +
        announcements.orEmpty().size +
        visitorEntries.orEmpty().size +
        incidents.orEmpty().size +
        maintenanceReports.orEmpty().size +
        curfewRecords.orEmpty().size +
        worshipSchedules.orEmpty().size +
        billingSettings.orEmpty().size +
        leaveNotices.orEmpty().size +
        absenceAlerts.orEmpty().size +
        rooms.orEmpty().size +
        jobs.orEmpty().size +
        guardianProfile.orEmpty().size
