package com.example.mbhm.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared column layouts so every attendance table reads the same way. */
private val presentTableColumns = listOf(
    TableColumn("Boarder", 1.5f),
    TableColumn("Check-in", 0.85f, end = true),
    TableColumn("Method", 1.05f, end = true)
)

private val absentTableColumns = listOf(
    TableColumn("Boarder", 1.5f),
    TableColumn("Reason", 1.5f, end = true)
)

private val pendingTableColumns = listOf(
    TableColumn("Boarder", 1.7f),
    TableColumn("Status", 1.0f, end = true)
)

@Composable
fun GAttendance(
    boarders: List<Boarder>,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    setRecords: (List<AttendanceRecord>) -> Unit,
    absenceAlerts: List<AbsenceAlertRecord> = emptyList(),
    onSendAlerts: (List<AbsenceAlertRecord>) -> Unit = {},
    schedule: WorshipSchedule,
    setSchedule: (WorshipSchedule) -> Unit,
    showToast: (String) -> Unit,
    deepLinkBoarder: Boarder? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    var screen by remember { mutableStateOf("main") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var session by remember { mutableStateOf("morning") }
    var scanSheet by remember { mutableStateOf(false) }
    var lastScanned by remember { mutableStateOf<String?>(null) }

    fun openBoarderRecords(b: Boarder) {
        screen = "individual"
        screenData = b
    }

    LaunchedEffect(deepLinkBoarder) {
        val b = deepLinkBoarder ?: return@LaunchedEffect
        openBoarderRecords(b)
        onDeepLinkConsumed()
    }

    if (screen == "override") {
        GAttn_Override(boarders, sessions, records, setRecords, showToast, onBack = { screen = "main" })
        return
    }
    if (screen == "export") {
        GAttn_Export(showToast, onBack = { screen = "main" })
        return
    }
if (screen == "individual") {
        val individual = boarders.firstOrNull { it.id == (screenData as? Boarder)?.id }
            ?: (screenData as? Boarder)
        if (individual != null) {
            GAttn_Individual(
                boarder = individual,
                sessions = sessions,
                records = records,
                onBack = { screen = "main"; screenData = null }
            )
        } else {
            screen = "main"
        }
        return
    }
    if (screen == "history") {
        GAttn_History(
            boarders = boarders,
            sessions = sessions,
            records = records,
            onOpenBoarder = { b -> openBoarderRecords(b) },
            onBack = { screen = "main" }
        )
        return
    }

    val todayLabel = remember {
        SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
    }
    val now = rememberNow(10_000L)
    val todaySessions = sessions.filter { it.date == todayLabel }
    val currentSession = todaySessions.find { it.type.name.lowercase() == session }
    val windowEndLabel = currentSession?.windowEnd
        ?: if (session == "morning") schedule.morningEnd else schedule.eveningEnd
    val windowDateLabel = currentSession?.date ?: todayLabel
    val isWindowLive = windowMillis(windowDateLabel, windowEndLabel)?.let { now < it } ?: true
    val sessionRecords = currentSession?.let { cs -> records.filter { it.sessionId == cs.id } } ?: emptyList()
    val present = sessionRecords.filter { it.status == AttendanceStatus.PRESENT }
    val absent = sessionRecords.filter { it.status == AttendanceStatus.ABSENT }
val alertStates = remember(boarders, records, absenceAlerts, schedule.absenceThreshold) {
        absenceAlertStates(boarders, records, absenceAlerts, schedule.absenceThreshold)
    }
    val pendingAlerts = alertStates.filter { it.needsAlert }
    val settledAlerts = alertStates.filter { !it.needsAlert }
    val scannedName = lastScanned

    Box(Modifier.fillMaxSize()) {
        Screen(
            title = "Attendance",
            right = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.clip(CircleShape)
                            .background(C.primaryLt)
                            .clickable {
                                scanSheet = true
                                lastScanned = null
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("📷 Scan", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    listOf(
                        "📈" to "history",
                        "✏️" to "override",
                        "📊" to "export"
                    ).forEach { (icon, target) ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(C.bg).clickable { screen = target },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(icon, fontSize = 16.sp)
                        }
                    }
                }
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.card)
                    .border(1.dp, C.border, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                attnSessionTab(
                    Modifier.weight(1f),
                    "☀️",
                    "Morning",
                    todaySessions.find { it.type == SessionType.MORNING }
                        ?.let { "${it.windowStart}–${it.windowEnd}" },
                    session == "morning"
                ) { session = "morning" }
                attnSessionTab(
                    Modifier.weight(1f),
                    "🌙",
                    "Evening",
                    todaySessions.find { it.type == SessionType.EVENING }
                        ?.let { "${it.windowStart}–${it.windowEnd}" },
                    session == "evening"
                ) { session = "evening" }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(todayLabel, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (isWindowLive) C.sageLt else C.bg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        (currentSession?.let { "${it.windowStart}–${it.windowEnd}" }
                            ?: if (session == "morning") "${schedule.morningStart}–${schedule.morningEnd}"
                            else "${schedule.eveningStart}–${schedule.eveningEnd}") +
                            if (isWindowLive) "  ·  Live" else "  ·  Ended",
                        color = if (isWindowLive) C.greenText else C.muted,
                        fontSize = 12.sp
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Present",
                    value = present.size,
                    sub = "of ${boarders.size}",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Absent",
                    value = absent.size,
                    sub = "recorded",
                    bg = C.dangerLt,
                    textColor = C.danger,
                    modifier = Modifier.weight(1f)
                )
            }

if (alertStates.isNotEmpty() &&
                rememberPrefBoolean("g_notif_attendance_alerts", true).value
            ) {
                val allSettled = pendingAlerts.isEmpty()
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (allSettled) C.sageLt else C.warnLt)
                        .border(1.dp, if (allSettled) C.sage else C.warn.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        if (allSettled) "✓ Absence Alerts Up to Date" else "⚠ Absence Threshold Reached",
                        color = if (allSettled) C.greenText else C.warn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        if (allSettled)
                            "Everyone over the ${schedule.absenceThreshold}-absence limit has already been notified. You will be prompted again only if they miss another session."
                        else
                            "${pendingAlerts.size} boarder${if (pendingAlerts.size == 1) "" else "s"} need${if (pendingAlerts.size == 1) "s" else ""} notifying — ${schedule.absenceThreshold} misses triggers an alert.",
                        color = if (allSettled) C.greenText else C.warn,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    alertStates.forEach { state ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.needsAlert) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f))
                                .clickable { openBoarderRecords(state.boarder) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(initials = state.boarder.initials, color = state.boarder.avatarColor, size = 28)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${state.boarder.name} — ${state.absenceCount} missed",
                                    color = if (state.needsAlert) C.warn else C.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (state.needsAlert)
                                        "Not yet notified at ${state.absenceCount} absences"
                                    else "Alerted at ${state.lastAlertedAt} absences · ${state.absenceCount} total",
                                    color = C.muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                            Text(
                                if (state.needsAlert) "Notify" else "✓ Sent",
                                color = if (state.needsAlert) C.warn else C.greenText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("›", color = C.muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!allSettled) {
                        Box(
                            Modifier
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(C.warn)
                                .clickable {
                                    val built = buildAbsenceAlerts(pendingAlerts, todayLabel)
                                    if (built.isEmpty()) {
                                        showToast("No new alerts to send")
                                    } else {
                                        onSendAlerts(built)
                                        showToast(
                                            if (built.size == 1) "Absence alert sent to 1 guardian"
                                            else "Absence alerts sent to ${built.size} guardians"
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Send ${pendingAlerts.size} Alert${if (pendingAlerts.size == 1) "" else "s"}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (settledAlerts.isNotEmpty() && !allSettled) {
                        Text(
                            "${settledAlerts.size} already notified — they will only appear again after another miss.",
                            color = C.muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(title = "Present (${present.size})")
                if (present.isEmpty()) {
                    PCard(padding = PaddingValues(14.dp)) {
                        Text("Nobody checked in yet", color = C.muted, fontSize = 12.sp)
                    }
                } else {
                    TableCard(presentTableColumns) {
                        present.forEachIndexed { index, r ->
                            val b = boarders.find { it.id == r.boarderId } ?: return@forEachIndexed
                            TableRow(
                                columns = presentTableColumns,
                                divider = index > 0,
                                onClick = { openBoarderRecords(b) },
                                cells = listOf(
                                    {
                                        Column {
                                            Text(
                                                b.name,
                                                color = C.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (r.overrideReason != null) {
                                                Text(
                                                    r.overrideReason,
                                                    color = C.muted,
                                                    fontSize = 10.sp,
                                                    fontStyle = FontStyle.Italic,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    {
                                        Text(
                                            r.checkInTime ?: "—",
                                            color = C.text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                if (r.method == AttendanceMethod.QR) "📱 QR Scan" else "✏️ Manual",
                                                color = C.muted,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                "View ›",
                                                color = C.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }

            if (absent.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Absent (${absent.size})")
                    TableCard(absentTableColumns) {
                        absent.forEachIndexed { index, r ->
                            val b = boarders.find { it.id == r.boarderId } ?: return@forEachIndexed
                            TableRow(
                                columns = absentTableColumns,
                                divider = index > 0,
                                onClick = { openBoarderRecords(b) },
                                cells = listOf(
                                    {
                                        Column {
                                            Text(
                                                b.name,
                                                color = C.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "Missed check-in",
                                                color = C.danger,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    },
                                    {
Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                r.overrideReason ?: "No check-in recorded",
                                                color = C.muted,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.End,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "View ›",
                                                color = C.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }

            val recordedIds = sessionRecords.map { it.boarderId }
            val unrecorded = boarders.filter { b -> b.id !in recordedIds }
            if (unrecorded.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Not Yet Recorded (${unrecorded.size})")
                    TableCard(pendingTableColumns) {
                        unrecorded.forEachIndexed { index, b ->
                            TableRow(
                                columns = pendingTableColumns,
                                divider = index > 0,
                                onClick = { openBoarderRecords(b) },
                                cells = listOf(
                                    {
                                        Column {
                                            Text(
                                                b.name,
                                                color = C.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "Room ${b.room}",
                                                color = C.muted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    },
                                    {
Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (isWindowLive) "Not yet scanned" else "Missed",
                                    color = if (isWindowLive) C.muted else C.danger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    "View records ›",
                                    color = C.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        BottomSheet(
            open = scanSheet,
            onClose = { scanSheet = false },
            title = "📷 Scan Boarder QR — ${if (session == "morning") "Morning" else "Evening"}"
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (scannedName != null) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.sageLt)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅", fontSize = 24.sp)
                        Text(
                            scannedName,
                            color = C.greenText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "Marked present for $session session",
                            color = C.greenText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Box(
                            Modifier.padding(top = 12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(C.greenText)
                                .clickable { lastScanned = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Scan Another", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    if (rememberPrefBoolean("g_qr_auto_refresh", true).value) {
                        Text(
                            "♻️ QR auto-refreshes for every session — no manual refresh needed",
                            color = C.muted,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "🔒 QR stays static until you refresh it manually",
                            color = C.muted,
                            fontSize = 12.sp
                        )
                    }
                    QRScanner(
                        onScan = { data ->
                            val boarderId = if (data.startsWith("boarder-")) data.substringAfter("boarder-") else null
                            val boarder = boarderId?.let { id -> boarders.find { it.id == id } }
                            if (boarder == null) {
                                showToast("⚠ Unrecognized QR code")
                            } else if (currentSession == null) {
                                showToast("⚠ No active $session session right now")
                            } else {
                                val sessId = currentSession.id
                                val existing = records.find { r -> r.boarderId == boarder.id && r.sessionId == sessId }
                                if (existing == null) {
                                    val timeStr = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date())
                                    setRecords(
                                        records + AttendanceRecord(
                                            id = "qr" + System.currentTimeMillis(),
                                            boarderId = boarder.id,
                                            sessionId = sessId,
                                            status = AttendanceStatus.PRESENT,
                                            checkInTime = timeStr,
                                            method = AttendanceMethod.QR
                                        )
                                    )
                                }
                                lastScanned = if (existing != null) "${boarder.name} (already checked in)" else boarder.name
                            }
                        },
                        onClose = { scanSheet = false },
                        label = "Point camera at the boarder's QR code to mark them present"
                    )
                }

                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.card)
                        .border(1.dp, C.border, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        buildAnnotatedString {
                            append("Scan each boarder's personal QR code to record their attendance for the ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(session)
                            }
                            append(" session. Each scan is timestamped and logged automatically.")
                        },
                        color = C.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun attnSessionTab(
    modifier: Modifier,
    emoji: String,
    label: String,
    window: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fg = if (selected) Color.White else C.muted
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) C.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$emoji $label",
            color = fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (window != null) {
            Text(
                window,
                color = fg.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun GAttn_Schedule(
    schedule: WorshipSchedule,
    setSchedule: (WorshipSchedule) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var s by remember { mutableStateOf(schedule) }

    Screen(title = "Worship Schedule", onBack = onBack) {
        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("☀️ Morning Session", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Input(
                        label = "Start Time",
                        value = s.morningStart,
                        onChange = { v -> s = s.copy(morningStart = v) },
                        modifier = Modifier.weight(1f)
                    )
                    Input(
                        label = "End Time",
                        value = s.morningEnd,
                        onChange = { v -> s = s.copy(morningEnd = v) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Input(
                    label = "QR Code Validity (minutes)",
                    value = s.morningQRMins.toString(),
                    onChange = { v -> s = s.copy(morningQRMins = v.toIntOrNull() ?: 0) },
                    keyboardType = KeyboardType.Number
                )
            }
        }

        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🌙 Evening Session", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Input(
                        label = "Start Time",
                        value = s.eveningStart,
                        onChange = { v -> s = s.copy(eveningStart = v) },
                        modifier = Modifier.weight(1f)
                    )
                    Input(
                        label = "End Time",
                        value = s.eveningEnd,
                        onChange = { v -> s = s.copy(eveningEnd = v) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Input(
                    label = "QR Code Validity (minutes)",
                    value = s.eveningQRMins.toString(),
                    onChange = { v -> s = s.copy(eveningQRMins = v.toIntOrNull() ?: 0) },
                    keyboardType = KeyboardType.Number
                )
            }
        }

        PCard {
            Select(
                label = "Auto-Alert After N Missed Worships",
                value = s.absenceThreshold.toString(),
                onChange = { v -> s = s.copy(absenceThreshold = v.toIntOrNull() ?: s.absenceThreshold) },
                options = listOf(1, 2, 3, 5, 7).map { n ->
                    Option(n.toString(), "$n missed worship" + if (n > 1) "s" else "")
                }
            )
        }

        PrimaryButton(
            text = "Save Schedule",
            onClick = {
                setSchedule(s)
                showToast("Schedule saved")
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GAttn_Override(
    boarders: List<Boarder>,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    setRecords: (List<AttendanceRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var boarderId by remember(boarders) { mutableStateOf(boarders.firstOrNull()?.id ?: "") }
    var sessionId by remember(sessions) { mutableStateOf(sessions.firstOrNull()?.id ?: "") }
    var status by remember { mutableStateOf("present") }
    var reason by remember { mutableStateOf("") }
    val auditAlerts = rememberPrefBoolean("g_qr_manual_override_alerts", true).value

    if (boarders.isEmpty() || sessions.isEmpty()) {
        EmptyState(icon = "📅", title = "No boarders or sessions yet")
        return
    }

    fun submit() {
        val existing = records.find { r -> r.boarderId == boarderId && r.sessionId == sessionId }
        val newStatus = if (status == "present") AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
        if (existing != null) {
            setRecords(
                records.map { r ->
                    if (r.id == existing.id) {
                        r.copy(
                            status = newStatus,
                            overrideReason = reason,
                            overriddenBy = "Guardian",
                            method = AttendanceMethod.MANUAL
                        )
                    } else r
                }
            )
        } else {
            setRecords(
                records + AttendanceRecord(
                    id = "o" + System.currentTimeMillis(),
                    boarderId = boarderId,
                    sessionId = sessionId,
                    status = newStatus,
                    method = AttendanceMethod.MANUAL,
                    overrideReason = reason,
                    overriddenBy = "Guardian"
                )
            )
        }
        showToast(
            if (auditAlerts) "📝 Manual check saved — audit note recorded for your review"
            else "Attendance record updated"
        )
        onBack()
    }

    Screen(title = "Manual Checking", onBack = onBack) {
        if (auditAlerts) {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.primaryLt)
                    .border(1.dp, C.primary, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "📝 Audit alerts are on — every manual check you save is logged to your attendance review.",
                    color = C.primary,
                    fontSize = 12.sp
                )
            }
        }
        Select(
            label = "Boarder",
            value = boarderId,
            onChange = { boarderId = it },
            options = boarders.map { Option(it.id, "${it.name} — Room ${it.room}") }
        )
        Select(
            label = "Session",
            value = sessionId,
            onChange = { sessionId = it },
            options = sessions.map { s ->
                Option(s.id, (if (s.type == SessionType.MORNING) "☀️ Morning" else "🌙 Evening") + " — ${s.date}")
            }
        )

        Column(Modifier.fillMaxWidth()) {
            Text(
                "Mark As",
                color = C.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                attnSegBtn(Modifier.weight(1f), "✅ Present", status == "present", C.sage) { status = "present" }
                attnSegBtn(Modifier.weight(1f), "❌ Absent", status == "absent", C.danger) { status = "absent" }
            }
        }

        Textarea(
            value = reason,
            onChange = { reason = it },
            label = "Reason (required for audit)",
            placeholder = "Reason for manual check...",
            rows = 3
        )

        PrimaryButton(
            text = "Save Manual Check",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = reason.isNotBlank()
        )
    }
}

@Composable
private fun attnSegBtn(modifier: Modifier, text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) accent else C.card)
            .then(if (selected) Modifier else Modifier.border(1.dp, C.border, shape))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else C.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GAttn_Export(showToast: (String) -> Unit, onBack: () -> Unit) {
    val iso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var from by remember { mutableStateOf(iso.format(java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))) }
    var to by remember { mutableStateOf(iso.format(java.util.Date())) }
    var fmt by remember { mutableStateOf("csv") }

    Screen(title = "Export Attendance", onBack = onBack) {
        Input(label = "From", value = from, onChange = { from = it })
        Input(label = "To", value = to, onChange = { to = it })

        Column(Modifier.fillMaxWidth()) {
            Text(
                "Format",
                color = C.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                attnSegBtn(Modifier.weight(1f), ".CSV", fmt == "csv", C.primary) { fmt = "csv" }
                attnSegBtn(Modifier.weight(1f), ".PDF", fmt == "pdf", C.primary) { fmt = "pdf" }
            }
        }

        PrimaryButton(
            text = "Export",
            onClick = {
                showToast("Attendance exported as ${fmt.uppercase()}")
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GAttn_History(
    boarders: List<Boarder>,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    onOpenBoarder: (Boarder) -> Unit,
    onBack: () -> Unit
) {
    var fromIso by remember { mutableStateOf("") }
    var toIso by remember { mutableStateOf("") }
    var sessionFilter by remember { mutableStateOf("all") }

val filteredSessions = sessions.filter { s ->
        val typeOk = sessionFilter == "all" || s.type.name.lowercase() == sessionFilter
        typeOk && withinDateRange(s.date, fromIso, toIso)
    }
    val filteredSessionIds = filteredSessions.map { it.id }.toSet()
    val visibleRecords = records.filter { it.sessionId in filteredSessionIds }

    val presentCount = visibleRecords.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = visibleRecords.count { it.status == AttendanceStatus.ABSENT }
    val totalMarked = presentCount + absentCount
    val rate = if (totalMarked > 0) (presentCount * 100) / totalMarked else 0

    // Newest sessions first so the list matches the ordering used everywhere else.
    val orderedSessions = filteredSessions.sortedByDescending {
        parseDisplayDate(it.date) ?: 0L
    }
    val latestSessionId = orderedSessions.firstOrNull()?.id
    val sessionDateById = filteredSessions.associate { it.id to it.date }
    val sortedRecords = visibleRecords.sortedByDescending {
        sessionDateById[it.sessionId]?.let { d -> parseDisplayDate(d) } ?: 0L
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Attendance History", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Sessions",
                    value = filteredSessions.size,
                    sub = "in selected range",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Attendance Rate",
                    value = "$rate%",
                    sub = "$presentCount of $totalMarked marked",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Present",
                    value = presentCount,
                    sub = "records",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Absent",
                    value = absentCount,
                    sub = "records",
                    bg = C.dangerLt,
                    textColor = C.danger,
                    modifier = Modifier.weight(1f)
                )
            }

            SectionHeader(title = "Filter by date")
            DateRangeFilter(from = fromIso, to = toIso, onChange = { f, t -> fromIso = f; toIso = t })

            ChipBar(
                options = listOf(
                    "All sessions" to "all",
                    "☀️ Morning" to "morning",
                    "🌙 Evening" to "evening"
                ),
                value = sessionFilter,
                onChange = { sessionFilter = it }
            )

SectionHeader(
                title = "Boarder summary",
                action = { Text("Newest sessions first", color = C.muted, fontSize = 11.sp) }
            )

boarders.forEach { b ->
                val mine = sortedRecords.filter { it.boarderId == b.id }
                val present = mine.count { it.status == AttendanceStatus.PRESENT }
                val absent = mine.count { it.status == AttendanceStatus.ABSENT }
                val boarderRate = if (present + absent > 0) (present * 100) / (present + absent) else 0
                // Whether the most recent session was missed, so the card can warn even
                // when the overall rate looks acceptable.
                val missedLatest = latestSessionId?.let { id ->
                    mine.any { it.sessionId == id && it.status == AttendanceStatus.ABSENT }
                } ?: false
                PCard(modifier = Modifier.clickable { onOpenBoarder(b) }) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(initials = b.initials, color = b.avatarColor)
                        Column(Modifier.weight(1f)) {
                            Text(b.name, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$present present · $absent absent · ${mine.size} records",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (absent >= 1) {
                                Text(
                                    "Missed latest session",
                                    color = C.danger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (missedLatest) C.dangerLt else if (boarderRate >= 80) C.sageLt else C.warnLt)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "$boarderRate%",
                                    color = if (missedLatest) C.danger else if (boarderRate >= 80) C.greenText else C.warn,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("View records ›", color = C.primary, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (boarders.isEmpty()) {
                EmptyState(icon = "👥", title = "No boarders yet")
            } else if (visibleRecords.isEmpty()) {
                EmptyState(icon = "📋", title = "No attendance records", sub = "Try another date range or session filter")
            }
        }
    }
}

@Composable
private fun GAttn_Individual(
    boarder: Boarder,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    onBack: () -> Unit
) {
    val mine = records.filter { it.boarderId == boarder.id }
    val present = mine.count { it.status == AttendanceStatus.PRESENT }
    val absent = mine.count { it.status == AttendanceStatus.ABSENT }
    val rate = if (present + absent > 0) (present * 100) / (present + absent) else 0
    val sessionDates = sessions.associate { it.id to it }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = boarder.name, onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            PCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(initials = boarder.initials, color = boarder.avatarColor, size = 48)
                    Column(Modifier.weight(1f)) {
                        Text(boarder.name, color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Room ${boarder.room} · ${boarder.job}", color = C.muted, fontSize = 12.sp)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Present",
                    value = present,
                    sub = "records",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Absent",
                    value = absent,
                    sub = "records",
                    bg = C.dangerLt,
                    textColor = C.danger,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Rate",
                    value = "$rate%",
                    sub = "attendance",
                    modifier = Modifier.weight(1f)
                )
            }

            val showCalendarColors = rememberPrefBoolean("notif_calendar_colors", true)
            val cal = remember { java.util.Calendar.getInstance() }
            val calYear = cal.get(java.util.Calendar.YEAR)
            val calMonth = cal.get(java.util.Calendar.MONTH)
            val todayDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val monthLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(java.util.Date()) }
            val daysInMonth = remember(calYear, calMonth) {
                java.util.Calendar.getInstance().apply { set(calYear, calMonth, 1) }
                    .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            }
            val firstDayOffset = remember(calYear, calMonth) {
                java.util.Calendar.getInstance().apply { set(calYear, calMonth, 1) }
                    .get(java.util.Calendar.DAY_OF_WEEK) - 1
            }
            val dayFromSession = remember(sessionDates, calYear, calMonth) {
                sessionDates.mapValues { (_, s) ->
                    parseDisplayDate(s.date)?.let { ms ->
                        val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
                        if (c.get(java.util.Calendar.YEAR) == calYear &&
                            c.get(java.util.Calendar.MONTH) == calMonth
                        ) c.get(java.util.Calendar.DAY_OF_MONTH) else null
                    }
                }
            }
            val presentDays = mine.filter { it.status == AttendanceStatus.PRESENT }
                .mapNotNull { dayFromSession[it.sessionId] }.distinct()
            val absentDays = mine.filter { it.status == AttendanceStatus.ABSENT }
                .mapNotNull { dayFromSession[it.sessionId] }.distinct()
                .filterNot { it in presentDays }

            PCard {
                Text(
                    monthLabel,
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                Row(Modifier.fillMaxWidth()) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                        Box(
                            Modifier.weight(1f).padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                d,
                                color = C.muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                val cells: List<Int?> =
                    List<Int?>(firstDayOffset) { null } + (1..daysInMonth).toList()
                cells.chunked(7).forEach { week ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            Box(
                                Modifier.weight(1f).aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isPresent = day in presentDays && showCalendarColors.value
                                    val isAbsentDay = day in absentDays && showCalendarColors.value
                                    val isToday = day == todayDay
                                    val isFuture = day > todayDay
                                    val cellBg = when {
                                        isToday -> C.primary
                                        isPresent -> C.sageLt
                                        isAbsentDay -> C.dangerLt
                                        else -> Color.Transparent
                                    }
                                    val cellFg = when {
                                        isToday -> Color.White
                                        isPresent -> C.greenText
                                        isAbsentDay -> C.danger
                                        isFuture -> C.chevron
                                        else -> C.muted
                                    }
                                    Box(
                                        Modifier.fillMaxSize()
                                            .clip(CircleShape)
                                            .background(cellBg)
                                            .then(
                                                if (isToday) Modifier.border(2.dp, C.primary, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                day.toString(),
                                                color = cellFg,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (isPresent || isAbsentDay) {
                                                Box(
                                                    Modifier.padding(top = 2.dp)
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isPresent) C.greenText else C.danger
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(
                        "Present" to C.sageLt,
                        "Absent" to C.dangerLt,
                        "Today" to C.primary
                    ).forEach { (label, dotColor) ->
                        Row(
                            Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(12.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Text(label, color = C.muted, fontSize = 12.sp)
                        }
                    }
                }
            }

            SectionHeader(title = "Records (${mine.size})")

mine.sortedByDescending { sessionDates[it.sessionId]?.let { s -> parseDisplayDate(s.date) } ?: 0L }
.forEach { r ->
                    val session = sessionDates[r.sessionId]
                    val isPresent = r.status == AttendanceStatus.PRESENT
                    PCard(padding = PaddingValues(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPresent) C.sageLt else C.dangerLt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isPresent) "✓" else "✗", color = if (isPresent) C.greenText else C.danger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${session?.date ?: "Unknown date"} · ${session?.let { if (it.type == SessionType.MORNING) "☀️ Morning" else "🌙 Evening" } ?: "Session"}",
                                    color = C.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    buildList {
                                        r.checkInTime?.let { add("In $it") }
                                        add(if (r.method == AttendanceMethod.QR) "📱 QR Scan" else "✏️ Manual Check")
                                        r.overrideReason?.let { add(it) }
                                    }.joinToString(" · "),
                                    color = C.muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

            if (mine.isEmpty()) {
                EmptyState(icon = "📋", title = "No attendance records yet")
            }
        }
    }
}
