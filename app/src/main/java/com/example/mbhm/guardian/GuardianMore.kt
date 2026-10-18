package com.example.mbhm.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import com.example.mbhm.ui.sync.DeviceSyncScreen
import kotlinx.coroutines.launch

private data class GProfile(
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    val houseName: String,
    val houseAddress: String
)

@Composable
fun GMore(
    onLogout: () -> Unit,
    showToast: (String) -> Unit,
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    billing: BillingSettings,
    setBilling: (BillingSettings) -> Unit,
    schedule: WorshipSchedule,
    setSchedule: (WorshipSchedule) -> Unit,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    onSaveRoom: (HouseRoom) -> Unit,
    onDeleteRoom: (String) -> Unit,
    onSaveJob: (HouseJob) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    val context = LocalContext.current
    val appRepository = remember(context) { AppRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val savedProfile by appRepository.observeGuardianProfile().collectAsState(initial = null)
    var screen by remember { mutableStateOf("main") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val profile = GProfile(
        name = savedProfile?.name?.takeIf { it.isNotBlank() } ?: "Guardian",
        role = "Guardian · House Owner",
        phone = savedProfile?.phone.orEmpty(),
        email = savedProfile?.email.orEmpty(),
        houseName = savedProfile?.houseName.orEmpty(),
        houseAddress = savedProfile?.houseAddress.orEmpty()
    )
    var form by remember { mutableStateOf(profile) }

    LaunchedEffect(savedProfile) {
        val saved = savedProfile
        if (saved != null) {
            form = GProfile(
                name = saved.name.ifBlank { "Guardian" },
                role = "Guardian · House Owner",
                phone = saved.phone,
                email = saved.email,
                houseName = saved.houseName,
                houseAddress = saved.houseAddress
            )
        }
    }

    if (screen == "profile") {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "Edit Profile", onBack = { screen = "main" })
            ScrollBody(modifier = Modifier.weight(1f), spacing = 16) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(80.dp)
                            .clip(RoundedCornerShape(50))
                            .background(C.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            form.name.take(1),
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                PCard {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Personal Information",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Input(
                            label = "Full Name",
                            value = form.name,
                            onChange = { v -> form = form.copy(name = v) }
                        )
                        Input(
                            label = "Phone Number",
                            value = form.phone,
                            onChange = { v -> form = form.copy(phone = v) },
                            keyboardType = KeyboardType.Phone
                        )
                        Input(
                            label = "Email Address",
                            value = form.email,
                            onChange = { v -> form = form.copy(email = v) },
                            keyboardType = KeyboardType.Email
                        )
                        Input(
                            label = "Role / Title",
                            value = form.role,
                            onChange = { v -> form = form.copy(role = v) }
                        )
                    }
                }

                PCard {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Boarding House",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Input(
                            label = "House Name",
                            value = form.houseName,
                            onChange = { v -> form = form.copy(houseName = v) }
                        )
                        Input(
                            label = "Address",
                            value = form.houseAddress,
                            onChange = { v -> form = form.copy(houseAddress = v) }
                        )
                    }
                }

                PrimaryButton(
                    text = "Save Changes",
                    onClick = {
                        scope.launch {
                            appRepository.saveGuardianProfile(
                                GuardianProfile(
                                    name = form.name.trim(),
                                    phone = form.phone.trim(),
                                    email = form.email.trim(),
                                    houseName = form.houseName.trim(),
                                    houseAddress = form.houseAddress.trim()
                                )
                            )
                        }
                        showToast("Profile updated")
                        screen = "main"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    if (screen == "sync") {
        DeviceSyncScreen(
            appRepository = appRepository,
            onBack = { screen = "main" },
            showToast = showToast
        )
        return
    }

    if (screen == "schedule") {
        GAttn_Schedule(schedule, setSchedule, showToast, onBack = { screen = "settings" })
        return
    }
    if (screen == "billing") {
        GPay_Settings(
            boarders, setBoarders, billing, setBilling, showToast,
            onBack = { screen = "settings" }
        )
        return
    }

    if (screen == "rooms") {
        GRooms(
            boarders = boarders,
            rooms = rooms,
            jobs = jobs,
            onSaveRoom = onSaveRoom,
            onDeleteRoom = onDeleteRoom,
            onSaveJob = onSaveJob,
            onDeleteJob = onDeleteJob,
            showToast = showToast,
            onBack = { screen = "settings" }
        )
        return
    }

    if (screen == "settings") {
        Screen(title = "Settings", onBack = { screen = "main" }) {

            Text(
                "Preferences save on this device and apply immediately.",
                color = C.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            SettingsGroup(
                title = "Attendance Settings",
                tag = "ATTENDANCE",
                tagBg = C.primaryLt,
                tagFg = C.primary
            ) {
                val attendanceAlerts = rememberPrefBoolean("g_notif_attendance_alerts", true)
                val autoRefreshQr = rememberPrefBoolean("g_qr_auto_refresh", true)
                val manualOverride = rememberPrefBoolean("g_qr_manual_override_alerts", true)
                val calendarColors = rememberPrefBoolean("notif_calendar_colors", true)
                Toggle(
                    label = "Absence Alerts",
                    sub = "Warn me when a boarder hits the absence threshold",
                    checked = attendanceAlerts.value,
                    onChange = { attendanceAlerts.value = it; showToast("Attendance setting saved") }
                )
                Toggle(
                    label = "Calendar Present / Absent Colors",
                    sub = "Color calendar days green when present, red when absent",
                    checked = calendarColors.value,
                    onChange = { calendarColors.value = it; showToast("Attendance setting saved") }
                )
                Toggle(
                    label = "Auto-Refresh QR",
                    sub = "Regenerate the scan code every session",
                    checked = autoRefreshQr.value,
                    onChange = { autoRefreshQr.value = it; showToast("Attendance setting saved") }
                )
                Toggle(
                    label = "Manual Check Alerts",
                    sub = "Show an audit note when attendance is checked manually",
                    checked = manualOverride.value,
                    onChange = { manualOverride.value = it; showToast("Attendance setting saved") }
                )
                SettingsLink(
                    icon = "🗓️",
                    label = "Worship Schedule",
                    sub = "Session times, QR validity & absence threshold",
                    onClick = { screen = "schedule" }
                )
            }

            SettingsGroup(
                title = "Payment Settings",
                tag = "PAYMENT",
                tagBg = C.sageLt,
                tagFg = C.greenText
            ) {
                val confirmPayments = rememberPrefBoolean("g_privacy_confirm_payments", true)
                val receiptAlerts = rememberPrefBoolean("g_payment_receipt_alerts", true)
                Toggle(
                    label = "Confirm Before Recording",
                    sub = "Ask for confirmation before verifying or rejecting a receipt",
                    checked = confirmPayments.value,
                    onChange = { confirmPayments.value = it; showToast("Payment setting saved") }
                )
                Toggle(
                    label = "Receipt Review Alerts",
                    sub = "Show a banner for receipts waiting for review",
                    checked = receiptAlerts.value,
                    onChange = { receiptAlerts.value = it; showToast("Payment setting saved") }
                )
                SettingsLink(
                    icon = "🧾",
                    label = "Billing Terms & Defaults",
                    sub = "Due day, grace period & late payment penalty",
                    onClick = { screen = "billing" }
                )
            }

            SettingsGroup(
                title = "Boarding House",
                tag = "ROOMS",
                tagBg = C.blueLt,
                tagFg = C.blueText
            ) {
                SettingsLink(
                    icon = "🚪",
                    label = "Rooms, Floors & Jobs",
                    sub = "Room list, max capacity & job dropdown options",
                    onClick = { screen = "rooms" }
                )
            }

            SettingsGroup(
                title = "Notifications",
                tag = "GENERAL",
                tagBg = C.warnLt,
                tagFg = C.warn
            ) {
                val sosNotifs = rememberPrefBoolean("g_notif_sos", true)
                val showContacts = rememberPrefBoolean("g_privacy_show_contacts", true)
                Toggle(
                    label = "SOS Notifications",
                    sub = "Immediate alert on boarder SOS",
                    checked = sosNotifs.value,
                    onChange = { sosNotifs.value = it; showToast("Preference saved") }
                )
                Toggle(
                    label = "Show Boarder Contact Info",
                    sub = "Display phone numbers in emergency contacts",
                    checked = showContacts.value,
                    onChange = { showContacts.value = it; showToast("Preference saved") }
                )
            }

            PCard {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "App Info",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "Version" to "1.0.0",
                            "Environment" to "Demo Mode",
                            "Data" to "Sample / Local"
                        ).forEach { (l, v) ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(l, color = C.muted, fontSize = 12.sp)
                                Text(
                                    v,
                                    color = C.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Screen(title = "More") {
        PCard(
            modifier = Modifier.clickable {
                form = profile
                screen = "profile"
            },
            padding = PaddingValues(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(C.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile.name.take(1),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        color = C.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        profile.role,
                        color = C.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        "${profile.phone} · ${profile.email}",
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text("✏ Edit", color = C.primary, fontSize = 14.sp)
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RowItem(
                icon = "🔄",
                label = "Wireless Sync",
                sub = "Shared database over Wi-Fi",
                onClick = { screen = "sync" }
            )
            RowItem(
                icon = "⚙️",
                label = "Settings",
                sub = "Payment, attendance & notification preferences",
                onClick = { screen = "settings" }
            )
        }

        PrimaryButton(
            text = "Sign Out",
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            bg = C.dangerLt,
            textColor = C.danger
        )

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    Button(onClick = { onLogout(); showLogoutDialog = false }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    tag: String,
    tagBg: Color,
    tagFg: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    PCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(tagBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(tag, color = tagFg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    title,
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsLink(
    icon: String,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Column(Modifier.weight(1f)) {
            Text(label, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = C.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", color = C.chevron, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
