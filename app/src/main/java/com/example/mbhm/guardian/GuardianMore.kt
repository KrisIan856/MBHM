package com.example.mbhm.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*

private data class GProfile(
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    val houseName: String,
    val houseAddress: String
)

@Composable
fun GMore(onLogout: () -> Unit, showToast: (String) -> Unit) {
    var screen by remember { mutableStateOf("main") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var profile by remember {
        mutableStateOf(
            GProfile(
                name = "Sandra Ramos",
                role = "Guardian · House Owner",
                phone = "09171234567",
                email = "sandra.ramos@gmail.com",
                houseName = "Casa Marigold",
                houseAddress = "Cebu City"
            )
        )
    }
    var form by remember { mutableStateOf(profile) }

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
                        profile = form
                        showToast("Profile updated")
                        screen = "main"
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    if (screen == "settings") {
        Screen(title = "Settings", onBack = { screen = "main" }) {
            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Notifications",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Toggle(
                        label = "Attendance Alerts",
                        sub = "Alert on absence threshold",
                        checked = true,
                        onChange = { showToast("Preference saved") }
                    )
                    Toggle(
                        label = "SOS Notifications",
                        sub = "Immediate alert on boarder SOS",
                        checked = true,
                        onChange = { showToast("Preference saved") }
                    )
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "QR & Attendance",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Toggle(
                        label = "Auto-Refresh QR",
                        sub = "Regenerate QR every session",
                        checked = true,
                        onChange = { showToast("Preference saved") }
                    )
                    Toggle(
                        label = "Manual Override Alerts",
                        sub = "Notify when attendance is overridden",
                        checked = false,
                        onChange = { showToast("Preference saved") }
                    )
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Privacy & Data",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Toggle(
                        label = "Require Confirmation on Payments",
                        sub = "Confirm before recording payment",
                        checked = true,
                        onChange = { showToast("Preference saved") }
                    )
                    Toggle(
                        label = "Show Boarder Contact Info",
                        sub = "Display phone numbers in emergency contacts",
                        checked = true,
                        onChange = { showToast("Preference saved") }
                    )
                }
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
                icon = "⚙️",
                label = "Settings",
                sub = "Notifications, QR, privacy preferences",
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
