package com.example.mbhm.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.data.sync.LocalSyncClient
import com.example.mbhm.data.sync.LocalSyncServer
import com.example.mbhm.data.sync.NetworkUtils
import com.example.mbhm.data.sync.SYNC_SERVER_PORT
import com.example.mbhm.data.sync.SyncManager
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.SecondaryButton
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Toggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PEER_PORT = 8080

@Composable
fun DeviceSyncScreen(
    appRepository: AppRepository,
    onBack: () -> Unit,
    showToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncManager = remember { SyncManager.getInstance(context) }
    val syncState by syncManager.state.collectAsState()

    val peerServer = remember { LocalSyncServer(context, appRepository, PEER_PORT) }
    val peerClient = remember { LocalSyncClient(appRepository) }
    val localIp = remember { NetworkUtils.getLocalIpAddress(context) }

    var hostInput by remember(syncState.host) { mutableStateOf(syncState.host) }
    var peerRunning by remember { mutableStateOf(peerServer.isRunning()) }
    var peerHostIp by remember { mutableStateOf("") }
    var peerStatus by remember { mutableStateOf<String?>(null) }
    var busyPeer by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { peerServer.stop() }
    }

    Screen(title = "Wireless Sync", onBack = onBack) {
        SectionHeader(title = "Shared Database (Computer)")
        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Run the MBHM sync server on your computer, then enter its address below. " +
                        "Every device on the same Wi-Fi will share one live database.",
                    color = C.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Input(
                    label = "Server Address",
                    value = hostInput,
                    onChange = { hostInput = it },
                    placeholder = "192.168.1.5",
                    keyboardType = KeyboardType.Uri
                )
                Text(
                    "Dashboard on your computer: http://<computer-ip>:$SYNC_SERVER_PORT",
                    color = C.muted,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = "Save Address",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            syncManager.setHost(hostInput)
                            scope.launch {
                                val outcome = syncManager.syncNow()
                                if (outcome.success) {
                                    showToast("Connected · ${outcome.received} records received")
                                } else {
                                    showToast("Saved, but server not reachable")
                                }
                            }
                        }
                    )
                    PrimaryButton(
                        text = if (syncState.busy) "Syncing..." else "Sync Now",
                        enabled = !syncState.busy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            syncManager.setHost(hostInput)
                            scope.launch { syncManager.syncNow() }
                        }
                    )
                }

                Toggle(
                    label = "Automatic Sync",
                    sub = "Exchange changes every few seconds",
                    checked = syncState.autoSync,
                    onChange = { syncManager.setAutoSync(it) }
                )
            }
        }

        PCard(
            bg = when {
                syncState.connected == true -> C.sageLt
                syncState.connected == false -> C.dangerLt
                else -> C.bg
            },
            borderColor = null
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Sync Status",
                    color = C.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(syncState.lastMessage, color = C.text, fontSize = 14.sp, lineHeight = 19.sp)
                syncState.lastSyncAt?.let { at ->
                    Text(
                        "Last attempt ${timeLabel(at)}" +
                            (syncState.serverVersion?.let { " · database version $it" } ?: ""),
                        color = C.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        SectionHeader(title = "This Device")
        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(label = "Device", value = NetworkUtils.deviceName())
                InfoRow(
                    label = "Address",
                    value = localIp?.let { "http://$it" } ?: "Not connected to a network"
                )
                InfoRow(label = "Client ID", value = syncManager.clientId.take(8))
            }
        }

        SectionHeader(title = "Direct Device Sync (no computer)")
        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(
                    label = "Sharing on port",
                    value = if (peerRunning) "On · port $PEER_PORT" else "Off"
                )
                PrimaryButton(
                    text = if (peerRunning) "Stop Sharing" else "Start Sharing",
                    bg = if (peerRunning) C.dangerLt else C.primary,
                    textColor = if (peerRunning) C.danger else Color.White,
                    enabled = !busyPeer,
                    onClick = {
                        scope.launch {
                            busyPeer = true
                            withContext(Dispatchers.IO) {
                                if (peerRunning) peerServer.stop() else peerServer.start()
                            }
                            peerRunning = peerServer.isRunning()
                            busyPeer = false
                            showToast(if (peerRunning) "Sharing on port $PEER_PORT" else "Sharing stopped")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Input(
                    label = "Other Device IP",
                    value = peerHostIp,
                    onChange = { peerHostIp = it },
                    placeholder = "192.168.1.7",
                    keyboardType = KeyboardType.Uri
                )
                PrimaryButton(
                    text = if (busyPeer) "Syncing..." else "Sync With Device",
                    enabled = !busyPeer,
                    onClick = {
                        val ip = peerHostIp.trim()
                        if (ip.isEmpty()) {
                            showToast("Enter the other device's IP address")
                            return@PrimaryButton
                        }
                        scope.launch {
                            busyPeer = true
                            peerStatus = "Syncing with $ip ..."
                            val result = withContext(Dispatchers.IO) {
                                peerClient.syncWithHost(ip, PEER_PORT)
                            }
                            peerStatus = if (result.success) {
                                "Direct sync complete — ${result.itemsSynced} records"
                            } else result.message
                            busyPeer = false
                            showToast(if (result.success) "Direct sync complete" else result.message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                peerStatus?.let { Text(it, color = C.muted, fontSize = 12.sp, lineHeight = 16.sp) }
            }
        }

        PCard(bg = C.primaryLt, borderColor = null) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Setup", color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "1. On your computer, double-click server\\start-server.bat.\n" +
                        "2. Connect both phones to the same Wi-Fi as the computer.\n" +
                        "3. Enter the computer's IP address above and tap Sync Now.\n" +
                        "4. Open http://<computer-ip>:$SYNC_SERVER_PORT in a browser to see the shared database.",
                    color = C.text,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = C.muted, fontSize = 12.sp)
        Text(
            value,
            color = C.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
