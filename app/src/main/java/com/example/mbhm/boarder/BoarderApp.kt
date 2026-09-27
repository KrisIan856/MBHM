package com.example.mbhm.boarder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.SampleData
import com.example.mbhm.model.Announcement
import com.example.mbhm.model.AttendanceRecord
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.Priority
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.NavBar
import com.example.mbhm.ui.components.NavItem
import com.example.mbhm.ui.components.QRCodeDisplay
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Toast
import com.example.mbhm.ui.components.money

@Composable
fun BoarderApp(onLogout: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var tab by remember { mutableStateOf("home") }

    BackHandler {
        if (tab != "home") {
            tab = "home"
        } else {
            activity?.moveTaskToBack(true)
        }
    }
    val boarder = SampleData.loggedInBoarder
    val payments = remember { mutableStateOf(SampleData.payments) }
    val attnRecords = remember { mutableStateOf(SampleData.attendanceRecords) }
    val announcements = remember { mutableStateOf(SampleData.announcements) }
    val maintenance = remember { mutableStateOf(SampleData.maintenanceReports) }
    val curfew = remember { mutableStateOf(SampleData.curfewRecords) }
    var toast by remember { mutableStateOf<String?>(null) }

    val navItems = listOf(
        NavItem("home", "Home", "🏠"),
        NavItem("payments", "Payments", "💳"),
        NavItem("attendance", "Attendance", "📅"),
        NavItem("security", "Security", "🛡️"),
        NavItem("more", "More", "••"),
    )

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "home" -> BHome(boarder, payments.value, announcements.value, attnRecords.value, onTabChange = { tab = it }, showToast = { toast = it })
                    "payments" -> BPayments(boarder, payments.value, setPayments = { payments.value = it }, showToast = { toast = it })
                    "attendance" -> BAttendance(boarder, attnRecords.value, setAttnRecords = { attnRecords.value = it }, showToast = { toast = it })
                    "security" -> BSecurity(boarder, announcements.value, setAnnouncements = { announcements.value = it }, maintenance.value, setMaintenance = { maintenance.value = it }, curfew.value, setCurfew = { curfew.value = it }, showToast = { toast = it })
                    "more" -> BMore(boarder, onLogout, showToast = { toast = it })
                }
            }
            NavBar(navItems, tab, onSelect = { tab = it })
        }
        toast?.let { msg ->
            Toast(msg, onDone = { toast = null }, Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp))
        }
    }
}

@Composable
fun BHome(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    announcements: List<Announcement>,
    attnRecords: List<AttendanceRecord>,
    onTabChange: (String) -> Unit,
    showToast: (String) -> Unit
) {
    val myAnn = announcements.filter { a -> a.isFor(boarder.id) }
    val unread = myAnn.filter { a -> !a.readBy.contains(boarder.id) }

    val payBg = when (boarder.paymentStatus) {
        PayStatus.PAID -> C.sage
        PayStatus.PENDING -> C.primary
        else -> C.danger
    }

    ScrollBody(
        padding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        spacing = 16
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Hi, ${boarder.name.split(' ')[0]}! 👋",
                    color = C.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Room ${boarder.room} · Casa Marigold",
                    color = C.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box {
                Avatar(initials = boarder.initials, color = boarder.avatarColor, size = 46)
                if (unread.isNotEmpty()) {
                    Box(
                        Modifier.align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(C.danger),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            unread.size.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
                .clickable { onTabChange("attendance") }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .border(1.dp, C.border, RoundedCornerShape(10.dp))
            ) {
                QRCodeDisplay(seed = "boarder-${boarder.id}", size = 64)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "My Attendance QR",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Show this to your guardian to check in",
                    color = C.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Box(
                    Modifier.padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(C.primaryLt)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Tap to enlarge →",
                        color = C.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(payBg)
                .padding(20.dp)
        ) {
            Text(
                "September 2026",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "Monthly Rent",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        money(boarder.monthlyRate),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (boarder.paymentStatus == PayStatus.PAID) {
                    Box(
                        Modifier.padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "✓ Paid",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Column(
                        Modifier.padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Due", color = C.muted, fontSize = 10.sp)
                        Text(
                            "Sep 10",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (boarder.paymentStatus != PayStatus.PAID) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onTabChange("payments") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Upload Receipt →",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.dangerLt)
                .border(1.dp, Color(0xFFF5C4C4), RoundedCornerShape(14.dp))
                .clickable { showToast("🆘 SOS alert sent to guardian!") }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🆘", fontSize = 30.sp)
            Column {
                Text(
                    "Emergency SOS",
                    color = C.danger,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap to alert guardian immediately",
                    color = C.danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (unread.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = "Announcements (${unread.size} unread)",
                    action = {
                        Text(
                            "See all",
                            color = C.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onTabChange("security") }
                        )
                    }
                )
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    unread.take(2).forEach { a ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(C.card)
                                .border(
                                    1.dp,
                                    if (a.priority == Priority.CRITICAL) Color(0xFFF5C4C4) else C.border,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (a.priority == Priority.CRITICAL) "🔴" else "📢",
                                fontSize = 18.sp
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    a.title,
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    a.body,
                                    color = C.muted,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    a.sentAt,
                                    color = C.muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
