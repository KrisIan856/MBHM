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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.repository.AppRepository
import com.example.mbhm.model.Announcement
import com.example.mbhm.model.AttendanceRecord
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.currentPeriod
import com.example.mbhm.model.monthAccount
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.Priority
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.NavBar
import com.example.mbhm.ui.components.NavItem
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.QRCodeDisplay
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Toast
import com.example.mbhm.ui.components.money
import com.example.mbhm.ui.components.isoToday
import com.example.mbhm.ui.components.rememberPrefBoolean
import com.example.mbhm.ui.components.withinDateRange
import com.example.mbhm.data.entity.UserEntity
import kotlinx.coroutines.launch

@Composable
fun BoarderApp(
    user: UserEntity,
    appRepository: AppRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("home") }
    var announcementsOpen by remember { mutableStateOf(false) }
    var pendingScreen by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (announcementsOpen) {
            announcementsOpen = false
        } else if (tab != "home") {
            tab = "home"
        } else {
            activity?.moveTaskToBack(true)
        }
    }
    val boarder = remember { mutableStateOf<com.example.mbhm.model.Boarder?>(null) }
    val payments by appRepository.getPaymentsByBoarder(user.boarderId ?: "").collectAsState(initial = emptyList())
    val attnRecords by appRepository.getAttendanceByBoarder(user.boarderId ?: "").collectAsState(initial = emptyList())
    val maintenance by appRepository.getMaintenanceByBoarder(user.boarderId ?: "").collectAsState(initial = emptyList())
    val announcements by appRepository.getAllAnnouncements().collectAsState(initial = emptyList())
    val incidents by appRepository.getAllIncidents().collectAsState(initial = emptyList())
    val sessions by appRepository.getAllAttendanceSessions().collectAsState(initial = emptyList())
    val schedule by appRepository.observeWorshipSchedule().collectAsState(initial = null)
    val absenceAlerts by appRepository.getAllAbsenceAlerts().collectAsState(initial = emptyList())
    var toast by remember { mutableStateOf<String?>(null) }

    fun go(target: String, screen: String? = null) {
        pendingScreen = screen
        announcementsOpen = false
        tab = target
    }

    val boarderId = user.boarderId

    LaunchedEffect(boarderId) {
        if (boarderId == null) return@LaunchedEffect
        boarder.value = appRepository.getBoarderById(boarderId)
    }

    fun updateBoarder(b: com.example.mbhm.model.Boarder) {
        boarder.value = b
        scope.launch { appRepository.updateBoarder(b) }
    }

    fun sendSos() {
        val id = boarderId ?: return
        scope.launch {
            appRepository.insertIncident(
                com.example.mbhm.model.Incident(
                    id = "sos" + System.currentTimeMillis(),
                    type = com.example.mbhm.model.IncidentType.SOS,
                    boarderId = id,
                    title = "SOS Alert",
                    description = "${boarder.value?.name ?: "A boarder"} triggered an emergency SOS.",
                    timestamp = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.ENGLISH).format(java.util.Date()),
                    resolved = false
                )
            )
        }
    }

    val setPayments: (List<com.example.mbhm.model.PaymentRecord>) -> Unit = { list ->
        scope.launch { appRepository.upsertPayments(list) }
    }
    val setAttnRecords: (List<com.example.mbhm.model.AttendanceRecord>) -> Unit = { list ->
        scope.launch { appRepository.upsertAttendanceRecords(list) }
    }
    val setAnnouncements: (List<com.example.mbhm.model.Announcement>) -> Unit = { list ->
        scope.launch { appRepository.upsertAnnouncements(list) }
    }
    val setMaintenance: (List<com.example.mbhm.model.MaintenanceReport>) -> Unit = { list ->
        scope.launch { appRepository.upsertMaintenanceReports(list) }
    }

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
                val b = boarder.value
                if (b != null && announcementsOpen) {
                    BAnnouncements(
                        boarder = b,
                        announcements = announcements,
                        setAnnouncements = setAnnouncements,
                        onBack = { announcementsOpen = false }
                    )
                } else if (b != null) {
                    when (tab) {
                        "home" -> BHome(
                            boarder = b,
                            payments = payments,
                            announcements = announcements,
                            attnRecords = attnRecords,
                            onNavigate = ::go,
                            onAnnouncements = { announcementsOpen = true },
                            onSendSos = ::sendSos,
                            showToast = { toast = it }
                        )
                        "payments" -> BPayments(b, payments, setPayments = setPayments, showToast = { toast = it })
                        "attendance" -> BAttendance(
                            boarder = b,
                            sessions = sessions,
                            attnRecords = attnRecords,
                            startScreen = pendingScreen ?: "main",
                            showToast = { toast = it },
                            schedule = schedule,
                            absenceAlerts = absenceAlerts
                        )
                        "security" -> BSecurity(
                            boarder = b,
                            startScreen = pendingScreen ?: "overview",
                            maintenance = maintenance,
                            setMaintenance = setMaintenance,
                            onBoarderUpdate = ::updateBoarder,
                            onSendSos = ::sendSos,
                            showToast = { toast = it }
                        )
                        "more" -> BMore(b, onBoarderUpdate = ::updateBoarder, onLogout = onLogout, showToast = { toast = it })
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = if (boarderId == null) "🔗" else "🏠",
                            title = if (boarderId == null) "Account not linked" else "Loading…",
                            sub = if (boarderId == null)
                                "This login isn't linked to a boarder profile yet."
                            else null
                        )
                    }
                }
            }
            NavBar(navItems, tab, onSelect = { pendingScreen = null; tab = it })
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
    onNavigate: (tab: String, screen: String?) -> Unit,
    onAnnouncements: () -> Unit,
    onSendSos: () -> Unit,
    showToast: (String) -> Unit
) {
    val myAnn = announcements.filter { a -> a.isFor(boarder.id) }
    val unread = myAnn.filter { a -> !a.readBy.contains(boarder.id) }
    val todayIso = isoToday()
    val todayAnn = myAnn.filter { a -> withinDateRange(a.sentAt, todayIso, todayIso) }
    val sortedAnn = myAnn.sortedWith(
        compareByDescending<Announcement> { withinDateRange(it.sentAt, todayIso, todayIso) }
            .thenByDescending { !it.readBy.contains(boarder.id) }
    )

    // Same derived account as the payments screen and the guardian's view, so the home
    // summary can never contradict what the boarder sees on their payment page.
    val myPay = payments.filter { it.boarderId == boarder.id }
    val homePeriod = remember { currentPeriod() }
    val homeAccount = remember(boarder, myPay, homePeriod) {
        monthAccount(boarder, myPay, homePeriod)
    }

    val payBg = when (homeAccount.status) {
        PayStatus.PAID -> C.sage
        PayStatus.PENDING -> C.primary
        else -> C.danger
    }

    val monthLabel = homePeriod
    val dueLabel = remember(boarder.dueDay) {
        java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(java.util.Date()) + " ${boarder.dueDay}"
    }
    var showSosConfirm by remember { mutableStateOf(false) }

    ScrollBody(
        padding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        spacing = 16
    ) {
        val greeting = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "$greeting, ${boarder.name.split(' ')[0]}! 👋",
                        color = C.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(C.primaryLt)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Boarder",
                            color = C.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    "Room ${boarder.room} · Casa Marigold",
                    color = C.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
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

        if (rememberPrefBoolean("notif_announcement_alerts", true).value) {
        Column(Modifier.fillMaxWidth()) {
            SectionHeader(
                    title = if (todayAnn.isNotEmpty()) "Today's Announcement" else "Announcement",
                action = {
                    Text(
                        "See all →",
                        color = C.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onAnnouncements() }
                    )
                }
            )
            if (myAnn.isEmpty()) {
                PCard(bg = C.bg, padding = PaddingValues(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onAnnouncements() },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📢", fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "No announcement yet",
                                color = C.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Your guardian posts updates here",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text("›", color = C.muted, fontSize = 18.sp)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sortedAnn.take(3).forEach { a ->
                        val isRead = a.readBy.contains(boarder.id)
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(C.card)
                                .border(
                                    1.dp,
                                    if (!isRead && a.priority == Priority.CRITICAL) C.danger.copy(alpha = 0.35f)
                                    else C.border,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onAnnouncements() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (a.priority == Priority.CRITICAL) "🔴" else "📢",
                                fontSize = 18.sp
                            )
                            Column(Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isRead) {
                                        Box(
                                            Modifier.size(7.dp)
                                                .clip(CircleShape)
                                                .background(C.primary)
                                        )
                                    }
                                    Text(
                                        a.title,
                                        color = C.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (!isRead) {
                                Text("New", color = C.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        }

        Column(Modifier.fillMaxWidth()) {
            SectionHeader(title = "Quick Access")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    icon = "📱",
                    label = "Attendance QR",
                    onClick = { onNavigate("attendance", "qr") },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = "💳",
                    label = "Payments",
                    onClick = { onNavigate("payments", null) },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = "📈",
                    label = "My History",
                    onClick = { onNavigate("attendance", "history") },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    icon = "🔧",
                    label = "Maintenance",
                    onClick = { onNavigate("security", "maintenance") },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = "🛡️",
                    label = "Security",
                    onClick = { onNavigate("security", null) },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = "📞",
                    label = "Emergency",
                    onClick = { onNavigate("security", "emergency-contact") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
                .clickable { onNavigate("attendance", null) }
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
                monthLabel,
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
                    money(homeAccount.due),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
                if (homeAccount.isFullyPaid) {
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
                            dueLabel,
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (!homeAccount.isFullyPaid &&
                rememberPrefBoolean("notif_upload_prompt", true).value
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onNavigate("payments", null) }
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
                .border(1.dp, C.danger.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable { showSosConfirm = true }
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

    }

    if (showSosConfirm) {
        Modal(
            open = showSosConfirm,
            onClose = { showSosConfirm = false },
            title = "Send Emergency SOS?"
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🆘", fontSize = 56.sp)
                Text(
                    "This will immediately alert the house guardian of an emergency. Only use in genuine emergency situations.",
                    color = C.muted,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            ActionPair(
                onCancel = { showSosConfirm = false },
                onConfirm = {
                    showSosConfirm = false
                    onSendSos()
                    showToast("🆘 SOS alert sent! Guardian has been notified.")
                },
                confirmLabel = "Send SOS Alert",
                confirmDanger = true
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, C.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.primaryLt),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Text(
            label,
            color = C.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
