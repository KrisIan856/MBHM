package com.example.mbhm.data.sync

import android.content.Context
import com.example.mbhm.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class SyncState(
    val host: String = "",
    val autoSync: Boolean = true,
    val hasToken: Boolean = false,
    val connected: Boolean? = null,
    val busy: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastMessage: String = "Not synced yet",
    val serverVersion: Long? = null
)

class SyncManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val tokenStore = SecureTokenStore(appContext)
    private val repository = AppRepository.getInstance(appContext)
    private val client = RemoteSyncClient(repository)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncing = AtomicBoolean(false)
    private var loopJob: Job? = null

    private val _state = MutableStateFlow(
        SyncState(
            host = prefs.getString(KEY_HOST, "") ?: "",
            autoSync = prefs.getBoolean(KEY_AUTO, true),
            hasToken = tokenStore.get().isNotBlank()
        )
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    val clientId: String = prefs.getString(KEY_CLIENT_ID, null)
        ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_CLIENT_ID, it).apply()
        }

    fun startAutoSync() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                val current = _state.value
                if (current.autoSync && current.host.isNotBlank() && current.hasToken && !current.busy) {
                    syncNow()
                }
            }
        }
    }

    fun setHost(host: String) {
        val trimmed = host.trim()
        prefs.edit().putString(KEY_HOST, trimmed).apply()
        _state.value = _state.value.copy(host = trimmed, connected = null)
    }

    /** Stores the shared-server bearer token encrypted with Android Keystore. */
    fun setToken(token: String) {
        val trimmed = token.trim()
        val saved = runCatching {
            tokenStore.set(trimmed)
            trimmed.isEmpty() || tokenStore.get() == trimmed
        }.getOrDefault(false)
        _state.value = _state.value.copy(
            hasToken = saved,
            connected = null,
            lastMessage = when {
                !saved -> "Could not save the token securely"
                trimmed.isEmpty() -> "Server token cleared"
                else -> "Server token saved"
            }
        )
    }

    fun setAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        _state.value = _state.value.copy(autoSync = enabled)
    }

    suspend fun syncNow(): RemoteSyncOutcome {
        if (!syncing.compareAndSet(false, true)) {
            return RemoteSyncOutcome(false, "Sync already in progress")
        }
        _state.value = _state.value.copy(busy = true)

        val host = _state.value.host
        val token = kotlinx.coroutines.withContext(Dispatchers.IO) { tokenStore.get() }
        if (host.isBlank()) {
            _state.value = _state.value.copy(busy = false, connected = false, lastMessage = "Enter the server address first")
            syncing.set(false)
            return RemoteSyncOutcome(false, "Enter the server address first")
        }
        if (token.isBlank()) {
            _state.value = _state.value.copy(busy = false, connected = false, lastMessage = "Enter the server access token")
            syncing.set(false)
            return RemoteSyncOutcome(false, "Enter the server access token")
        }

        val since = prefs.getLong(KEY_SINCE, 0L)
        val outcome = try {
            client.sync(host, token, clientId, since)
        } finally {
            _state.value = _state.value.copy(busy = false)
            syncing.set(false)
        }

        if (outcome.success) {
            if (outcome.serverVersion >= 0) {
                prefs.edit().putLong(KEY_SINCE, outcome.serverVersion).apply()
            }
            val detail = buildString {
                append("${outcome.received} records in")
                if (outcome.removed > 0) append(", ${outcome.removed} deleted")
                append(", ${outcome.sent} out")
            }
            _state.value = _state.value.copy(
                connected = true,
                lastSyncAt = System.currentTimeMillis(),
                lastMessage = "${outcome.message} · $detail",
                serverVersion = outcome.serverVersion.takeIf { it >= 0 }
                    ?: _state.value.serverVersion
            )
        } else {
            _state.value = _state.value.copy(
                connected = false,
                lastSyncAt = System.currentTimeMillis(),
                lastMessage = outcome.message
            )
        }
        return outcome
    }

    companion object {
        private const val PREFS = "mbhm_sync"
        private const val KEY_HOST = "server_host"
        private const val KEY_AUTO = "auto_sync"
        private const val KEY_SINCE = "since_version"
        private const val KEY_CLIENT_ID = "client_id"
        private const val INTERVAL_MS = 30_000L

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
