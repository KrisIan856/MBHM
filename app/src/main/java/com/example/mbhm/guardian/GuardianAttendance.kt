package com.example.mbhm.guardian

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GAttendance(
    boarders: List<Boarder>,
    sessions: List<AttendanceSession>,
    records: List<AttendanceRecord>,
    setRecords: (List<AttendanceRecord>) -> Unit,
    schedule: WorshipSchedule,
    setSchedule: (WorshipSchedule) -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember { mutableStateOf("main") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var session by remember { mutableStateOf("morning") }
    var scanSheet by remember { mutableStateOf(false) }
    var lastScanned by remember { mutableStateOf<String?>(null) }

    if (screen == "schedule") {
        GAttn_Schedule(schedule, setSchedule, showToast, onBack = { screen = "main" })
        return
    }
    if (screen == "override") {
        GAttn_Override(boarders, sessions, records, setRecords, showToast, onBack = { screen = "main" })
        return
    }
    if (screen == "export") {
        GAttn_Export(showToast, onBack = { screen = "main" })
        return
    }

    val todaySessions = sessions.filter { it.date == "Sep 7, 2026" }
    val currentSession = todaySessions.find { it.type.name.lowercase() == session }
    val sessionRecords = currentSession?.let { cs -> records.filter { it.sessionId == cs.id } } ?: emptyList()
    val present = sessionRecords.filter { it.status == AttendanceStatus.PRESENT }
    val absent = sessionRecords.filter { it.status == AttendanceStatus.ABSENT }
    val absenceWarnings = boarders.filter { b ->
        records.count { r -> r.boarderId == b.id && r.status == AttendanceStatus.ABSENT } >= schedule.absenceThreshold
    }
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
                        "⚙️" to "schedule",
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
                Text("Sep 7, 2026", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(C.sageLt)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (session == "morning") "6:00–8:00 AM" else "8:00–10:00 PM",
                        color = C.greenText,
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

            if (absenceWarnings.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.warnLt)
                        .border(1.dp, Color(0xFFF0DCA8), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠ Absence Threshold Reached",
                        color = C.warn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    absenceWarnings.forEach { b ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(initials = b.initials, color = b.avatarColor, size = 28)
                            Text(
                                "${b.name} — ${records.count { r -> r.boarderId == b.id && r.status == AttendanceStatus.ABSENT }} missed",
                                color = C.warn,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(
                        Modifier.padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(C.warn)
                            .clickable { showToast("Absence alerts sent") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Send Alerts", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(title = "Present (${present.size})")
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    present.forEach { r ->
                        val b = boarders.find { it.id == r.boarderId } ?: return@forEach
                        PCard(padding = PaddingValues(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(initials = b.initials, color = b.avatarColor)
                                Column(Modifier.weight(1f)) {
                                    Text(b.name, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${r.checkInTime ?: ""} · ${if (r.method == AttendanceMethod.QR) "📱 QR Scan" else "✏️ Manual"}",
                                        color = C.muted,
                                        fontSize = 12.sp
                                    )
                                    if (r.overrideReason != null) {
                                        Text(
                                            r.overrideReason,
                                            color = C.muted,
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                                Text("✅", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            if (absent.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Absent (${absent.size})")
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        absent.forEach { r ->
                            val b = boarders.find { it.id == r.boarderId } ?: return@forEach
                            PCard(padding = PaddingValues(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(initials = b.initials, color = b.avatarColor)
                                    Column(Modifier.weight(1f)) {
                                        Text(b.name, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        if (r.overrideReason != null) {
                                            Text(r.overrideReason, color = C.muted, fontSize = 12.sp)
                                        }
                                    }
                                    Text("❌", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            val recordedIds = sessionRecords.map { it.boarderId }
            val unrecorded = boarders.filter { b -> b.id !in recordedIds }
            if (unrecorded.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Not Yet Recorded (${unrecorded.size})")
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        unrecorded.forEach { b ->
                            PCard(padding = PaddingValues(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(initials = b.initials, color = b.avatarColor)
                                    Text(
                                        b.name,
                                        color = C.muted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("—", color = C.muted, fontSize = 12.sp)
                                }
                            }
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
                    QRScanner(
                        onScan = { data ->
                            val boarderId = if (data.startsWith("boarder-")) data.substringAfter("boarder-") else null
                            val boarder = boarderId?.let { id -> boarders.find { it.id == id } }
                            if (boarder == null) {
                                showToast("⚠ Unrecognized QR code")
                            } else {
                                val sessId = currentSession?.id ?: "s1"
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
private fun GAttn_Schedule(
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
                            overriddenBy = "Ate Sandra",
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
                    overriddenBy = "Ate Sandra"
                )
            )
        }
        showToast("Attendance record updated")
        onBack()
    }

    Screen(title = "Manual Override", onBack = onBack) {
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
            placeholder = "Reason for manual override...",
            rows = 3
        )

        PrimaryButton(
            text = "Save Override",
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
    var from by remember { mutableStateOf("2026-09-01") }
    var to by remember { mutableStateOf("2026-09-07") }
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
