package com.example.mbhm.guardian

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.SampleData
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*

@Composable
fun GuardianApp(onLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var tab by remember { mutableStateOf("dashboard") }

    BackHandler {
        if (tab != "dashboard") {
            tab = "dashboard"
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    val boarders = remember { mutableStateOf(SampleData.boarders) }
    val payments = remember { mutableStateOf(SampleData.payments) }
    val attnRecords = remember { mutableStateOf(SampleData.attendanceRecords) }
    val announcements = remember { mutableStateOf(SampleData.announcements) }
    val incidents = remember { mutableStateOf(SampleData.incidents) }
    val maintenance = remember { mutableStateOf(SampleData.maintenanceReports) }
    val curfewRecords = remember { mutableStateOf(SampleData.curfewRecords) }
    val billing = remember { mutableStateOf(SampleData.defaultBilling) }
    val schedule = remember { mutableStateOf(SampleData.worshipSchedule) }
    var leaveNotices by remember { mutableStateOf(SampleData.leaveNotices) }
    var toast by remember { mutableStateOf<String?>(null) }
    var securityDeepLink by remember { mutableStateOf<String?>(null) }
    var showProfileModal by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("dashboard", "Home", "🏠"),
        NavItem("payments", "Payments", "💳"),
        NavItem("attendance", "Attendance", "📅"),
        NavItem("security", "Security", "🛡️"),
        NavItem("more", "More", "••"),
    )

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "dashboard" -> GDashboard(
                        boarders = boarders.value,
                        payments = payments.value,
                        attnRecords = attnRecords.value,
                        leaveNotices = leaveNotices,
                        onTabChange = { tab = it },
                        onEmergencyContacts = { securityDeepLink = "contacts"; tab = "security" },
                        onProfileClick = { showProfileModal = true }
                    )
                    "payments" -> GPayments(
                        boarders = boarders.value,
                        payments = payments.value,
                        billing = billing.value,
                        setBoarders = { boarders.value = it },
                        setPayments = { payments.value = it },
                        setBilling = { billing.value = it },
                        showToast = { toast = it }
                    )
                    "attendance" -> GAttendance(
                        boarders = boarders.value,
                        sessions = SampleData.sessions,
                        records = attnRecords.value,
                        setRecords = { attnRecords.value = it },
                        schedule = schedule.value,
                        setSchedule = { schedule.value = it },
                        showToast = { toast = it }
                    )
                    "security" -> GSecurity(
                        boarders = boarders.value,
                        setBoarders = { boarders.value = it },
                        announcements = announcements.value,
                        setAnnouncements = { announcements.value = it },
                        incidents = incidents.value,
                        setIncidents = { incidents.value = it },
                        maintenance = maintenance.value,
                        setMaintenance = { maintenance.value = it },
                        curfew = curfewRecords.value,
                        setCurfew = { curfewRecords.value = it },
                        deepLink = securityDeepLink,
                        onDeepLinkConsumed = { securityDeepLink = null },
                        showToast = { toast = it }
                    )
                    "more" -> GMore(onLogout = onLogout, showToast = { toast = it })
                }
            }
            NavBar(navItems, tab, onSelect = { tab = it })
        }

        toast?.let { msg ->
            Toast(msg, onDone = { toast = null }, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp))
        }

        // Guardian Profile Modal (Req 2.A)
        Modal(
            open = showProfileModal,
            onClose = { showProfileModal = false },
            title = "Guardian Account"
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(C.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Text("Sandra Santos", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("House Guardian · Casa Marigold", color = C.muted, fontSize = 13.sp)

                HorizontalDivider(color = C.border, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.dangerLt)
                        .clickable {
                            showProfileModal = false
                            onLogout()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sign Out", color = C.danger, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GDashboard(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    attnRecords: List<AttendanceRecord>,
    leaveNotices: List<LeaveNotice>,
    onTabChange: (String) -> Unit,
    onEmergencyContacts: () -> Unit,
    onProfileClick: () -> Unit
) {
    val sepPay = payments.filter { it.period == "Sep 2026" }
    val paid = sepPay.count { it.status == PayStatus.PAID }
    val partial = sepPay.count { it.status == PayStatus.PARTIAL }
    val pending = sepPay.count { it.status == PayStatus.PENDING }
    val overdue = sepPay.count { it.status == PayStatus.OVERDUE }
    val present = attnRecords.count { it.sessionId == "s1" && it.status == AttendanceStatus.PRESENT }
    val fullCollected = sepPay.filter { it.status == PayStatus.PAID }.sumOf { it.amount }
    val partialCollected = sepPay.filter { it.status == PayStatus.PARTIAL }.sumOf { it.amountPaid ?: 0 }
    val collected = fullCollected + partialCollected
    val totalExpected = boarders.sumOf { it.monthlyRate }

    ScrollBody(spacing = 20, padding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)) {
        // Top Header with Top-Right Guardian Profile Avatar (Req 2.A)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good morning, 👋", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Sandra Santos · Casa Marigold", color = C.muted, fontSize = 14.sp)
            }

            // Circular Profile Picture (Req 2.A)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(C.primary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("S", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Fully Paid",
                value = paid,
                sub = "of ${boarders.size} boarders",
                bg = C.sageLt,
                textColor = C.greenText,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Partially Paid",
                value = partial,
                sub = "balance still due",
                bg = C.blueLt,
                textColor = C.blueText,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Present Today",
                value = present,
                sub = "${boarders.size - present} absent",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Action Needed",
                value = pending + overdue,
                sub = "$overdue overdue · $pending pending",
                bg = C.warnLt,
                textColor = C.warn,
                modifier = Modifier.weight(1f)
            )
        }

        PCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("September Collection", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Sep 2026", color = C.muted, fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(money(collected), color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("of ${money(totalExpected)}", color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(C.sage))
                    Text("Paid ${money(fullCollected)}", color = C.muted, fontSize = 11.sp)
                }
                if (partialCollected > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(C.blueBar))
                        Text("Partial ${money(partialCollected)}", color = C.muted, fontSize = 11.sp)
                    }
                }
            }
            dashCollectionBar(fullCollected, partialCollected, totalExpected)
            Text(
                "${paid} fully paid · ${partial} partial · ${pending + overdue} outstanding",
                color = C.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Reusable Consistent Quick Actions (Req 2.B)
        GuardianQuickActions(
            onPaymentsClick = { onTabChange("payments") },
            onAttendanceClick = { onTabChange("attendance") },
            onEmergencyContactsClick = onEmergencyContacts,
            onSecurityClick = { onTabChange("security") }
        )
    }
}

// Reusable Consistent Quick Actions Component (Req 2.B)
@Composable
fun GuardianQuickActions(
    onPaymentsClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit,
    onSecurityClick: () -> Unit
) {
    Column {
        SectionHeader(title = "Quick Actions")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashAction(Modifier.weight(1f), "💳", "Payments", C.card, C.border, C.text, onPaymentsClick)
            dashAction(Modifier.weight(1f), "📅", "Attendance", C.card, C.border, C.text, onAttendanceClick)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dashAction(Modifier.weight(1f), "🛡️", "Security", C.card, C.border, C.text, onSecurityClick)
            dashAction(Modifier.weight(1f), "📞", "Emergency", C.dangerLt, Color(0xFFF5C4C4), C.danger, onEmergencyContactsClick)
        }
    }
}

@Composable
private fun dashCollectionBar(full: Int, partial: Int, total: Int) {
    val fullFrac = if (total > 0) (full.toFloat() / total).coerceIn(0f, 1f) else 0f
    val partialFrac = if (total > 0) (partial.toFloat() / total).coerceIn(0f, 1f) else 0f
    val restFrac = (1f - fullFrac - partialFrac).coerceAtLeast(0f)
    Box(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(C.bg)
    ) {
        Row(Modifier.fillMaxSize()) {
            if (fullFrac > 0f) {
                Box(Modifier.weight(fullFrac).fillMaxHeight().background(C.sage))
            }
            if (partialFrac > 0f) {
                Box(Modifier.weight(partialFrac).fillMaxHeight().background(C.blueBar))
            }
            if (restFrac > 0f) {
                Box(Modifier.weight(restFrac).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun dashAction(
    modifier: Modifier,
    icon: String,
    label: String,
    bg: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
