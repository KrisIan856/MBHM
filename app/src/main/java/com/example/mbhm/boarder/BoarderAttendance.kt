package com.example.mbhm.boarder

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.AttendanceRecord
import com.example.mbhm.model.AbsenceAlertRecord
import com.example.mbhm.model.AbsenceAlertState
import com.example.mbhm.model.AttendanceStatus
import com.example.mbhm.model.lastAlertedAbsenceCounts
import com.example.mbhm.model.Boarder
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.DateRangeFilter
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.QRCodeDisplay
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.StatCard
import com.example.mbhm.ui.components.rememberPrefBoolean
import com.example.mbhm.ui.components.rememberNow
import com.example.mbhm.ui.components.windowMillis
import com.example.mbhm.ui.components.withinDateRange
import com.example.mbhm.ui.components.parseDisplayDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BAttendance(
    boarder: Boarder,
    sessions: List<com.example.mbhm.model.AttendanceSession>,
    attnRecords: List<AttendanceRecord>,
    startScreen: String = "main",
    showToast: (String) -> Unit,
    schedule: com.example.mbhm.model.WorshipSchedule? = null,
    absenceAlerts: List<AbsenceAlertRecord> = emptyList()
) {
    var screen by remember(startScreen) { mutableStateOf(startScreen) }
    var activeSession by remember { mutableStateOf("morning") }

    val showCalendarColors = rememberPrefBoolean("notif_calendar_colors", true)
    val absenceAlertsOn = rememberPrefBoolean("notif_absence_alerts", true)
    val sessionReminders = rememberPrefBoolean("notif_attendance_reminders", true)

    val myRecords = attnRecords.filter { r -> r.boarderId == boarder.id }
    val present = myRecords.filter { r -> r.status == AttendanceStatus.PRESENT }
    val absent = myRecords.filter { r -> r.status == AttendanceStatus.ABSENT }

    // Reads the guardian's configured limit and the real sent-alert history, so this
    // banner tells the boarder exactly what the guardian actually knows.
    val threshold = schedule?.absenceThreshold ?: 3
    val myAlerts = absenceAlerts.filter { it.boarderId == boarder.id }
    val lastAlerted = lastAlertedAbsenceCounts(myAlerts)[boarder.id] ?: 0
    val absenceState = AbsenceAlertState(
        boarder = boarder,
        absenceCount = absent.size,
        threshold = threshold,
        lastAlertedAt = lastAlerted
    )

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH) }
    val monthFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }
    val today = remember { dateFmt.format(Date()) }
    val monthLabel = remember { monthFmt.format(Date()) }
    val todaySessions = remember(sessions, today) { sessions.filter { it.date == today } }
    val morningSession = todaySessions.find { it.type == com.example.mbhm.model.SessionType.MORNING }
    val eveningSession = todaySessions.find { it.type == com.example.mbhm.model.SessionType.EVENING }
    val todayMorning = morningSession?.let { s -> myRecords.find { r -> r.sessionId == s.id } }
    val todayEvening = eveningSession?.let { s -> myRecords.find { r -> r.sessionId == s.id } }

    val now = rememberNow(10_000L)
    val morningEnd = morningSession?.let { windowMillis(it.date, it.windowEnd) }
    val eveningEnd = eveningSession?.let { windowMillis(it.date, it.windowEnd) }
    val isMorningOpen = morningSession != null && (morningEnd?.let { now < it } ?: true)
    val isEveningOpen = eveningSession != null && (eveningEnd?.let { now < it } ?: true)

    val calendar = remember { java.util.Calendar.getInstance() }
    val calYear = calendar.get(java.util.Calendar.YEAR)
    val calMonth = calendar.get(java.util.Calendar.MONTH)
    val todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val daysInMonth = remember(calYear, calMonth) {
        java.util.Calendar.getInstance().apply { set(calYear, calMonth, 1) }
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }
    val firstDayOffset = remember(calYear, calMonth) {
        java.util.Calendar.getInstance().apply { set(calYear, calMonth, 1) }
            .get(java.util.Calendar.DAY_OF_WEEK) - 1
    }
    val dayFromSession = remember(sessions, calYear, calMonth) {
        sessions.associate { s ->
            s.id to (runCatching { dateFmt.parse(s.date) }.getOrNull()?.let { d ->
                val c = java.util.Calendar.getInstance().apply { time = d }
                if (c.get(java.util.Calendar.YEAR) == calYear && c.get(java.util.Calendar.MONTH) == calMonth) {
                    c.get(java.util.Calendar.DAY_OF_MONTH)
                } else null
            })
        }
    }
    val presentDays = present.mapNotNull { dayFromSession[it.sessionId] }.distinct()
    val absentDays = absent.mapNotNull { dayFromSession[it.sessionId] }.distinct()

    fun openQR(sess: String) {
        activeSession = sess
        screen = "qr"
    }

    if (screen == "history") {
        BAttn_History(
            boarder = boarder,
            sessions = sessions,
            records = myRecords,
            onBack = { screen = "main" }
        )
        return
    }

    if (screen == "qr") {
        val activeS = if (activeSession == "morning") morningSession else eveningSession
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "My Attendance QR", onBack = { screen = "main" })
            ScrollBody(
                modifier = Modifier.weight(1f),
                padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.primaryLt)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (activeSession == "morning") "☀️ Morning Worship Check-in"
                        else "🌙 Evening Worship Check-in",
                        color = C.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        listOfNotNull(
                            activeS?.let { "${it.windowStart} – ${it.windowEnd}" },
                            today
                        ).joinToString(" · "),
                        color = C.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (activeS != null) {
                        val qrEndMillis = windowMillis(activeS.date, activeS.windowEnd)
                        val qrLive = qrEndMillis?.let { now < it } ?: true
                        Text(
                            if (qrLive) "● Check-in open · closes at ${activeS.windowEnd}"
                            else "✕ Check-in closed at ${activeS.windowEnd}",
                            color = if (qrLive) C.greenText else C.danger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, C.border, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        QRCodeDisplay(seed = "boarder-${boarder.id}", size = 218)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            boarder.name,
                            color = C.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Room ${boarder.room} · ID: ${boarder.id}",
                            color = C.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

            if (sessionReminders.value) {
                val anyOpen = isMorningOpen || isEveningOpen
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (anyOpen) C.primaryLt else C.bg)
                        .border(
                            1.dp,
                            if (anyOpen) C.primary else C.border,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        if (anyOpen) "🔔 Worship check-in is open" else "🔕 No check-in window open today",
                        color = if (anyOpen) C.primary else C.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (anyOpen) {
                            listOfNotNull(
                                morningSession?.let { "Morning ${it.windowStart}–${it.windowEnd}" },
                                eveningSession?.let { "Evening ${it.windowStart}–${it.windowEnd}" }
                            ).joinToString("  ·  ")
                        } else "You will be reminded when a session opens",
                        color = if (anyOpen) C.primary else C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            PCard {
                    Text(
                        "How to use",
                        color = C.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "1. Show this QR code to the house guardian.",
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "2. The guardian will scan it with their device to mark your attendance.",
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        "3. Your check-in will be recorded immediately.",
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Text(
                    "This QR is unique to your account. Do not share it with other boarders.",
                    color = C.muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(
            title = "Attendance",
            right = {
                Text(
                    "📈 History",
                    color = C.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { screen = "history" }
                )
            }
        )
        ScrollBody(modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, C.primary, RoundedCornerShape(14.dp))
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(C.primaryLt)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "My QR Code",
                        color = C.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your guardian scans this to mark you present",
                        color = C.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().background(C.card).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .border(1.dp, C.border, RoundedCornerShape(10.dp))
                    ) {
                        QRCodeDisplay(seed = "boarder-${boarder.id}", size = 80)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            boarder.name,
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Room ${boarder.room} · ID: ${boarder.id}",
                            color = C.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Box(
                            Modifier.fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.primary)
                                .clickable { openQR("morning") }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📱 Show Full QR",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(title = "Today — $today")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (todayMorning?.status) {
                                    AttendanceStatus.PRESENT -> C.sageLt
                                    AttendanceStatus.ABSENT -> C.dangerLt
                                    null -> C.card
                                }
                            )
                            .border(
                                1.dp,
                                if (todayMorning?.status == AttendanceStatus.ABSENT) C.danger else C.border,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            "☀️ Morning",
                            color = C.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            morningSession?.let { "${it.windowStart} – ${it.windowEnd}" }
                                ?: "Morning session",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (todayMorning != null && todayMorning.status == AttendanceStatus.PRESENT) {
                            Text(
                                "✅ ${todayMorning.checkInTime}",
                                color = C.greenText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (todayMorning != null && todayMorning.status == AttendanceStatus.ABSENT) {
                            Text(
                                "❌ Absent",
                                color = C.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Not checked in by ${morningSession?.windowEnd ?: "window end"}",
                                color = C.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else if (isMorningOpen) {
                            Box(
                                Modifier.padding(top = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(C.primary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable { openQR("morning") }
                            ) {
                                Text(
                                    "Show My QR",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                if (morningSession == null) "Session not scheduled today"
                                else "Check-in closed",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (todayEvening?.status) {
                                    AttendanceStatus.PRESENT -> C.sageLt
                                    AttendanceStatus.ABSENT -> C.dangerLt
                                    null -> C.card
                                }
                            )
                            .border(
                                1.dp,
                                if (todayEvening?.status == AttendanceStatus.ABSENT) C.danger else C.border,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            "🌙 Evening",
                            color = C.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            eveningSession?.let { "${it.windowStart} – ${it.windowEnd}" }
                                ?: "Evening session",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (todayEvening != null && todayEvening.status == AttendanceStatus.PRESENT) {
                            Text(
                                "✅ ${todayEvening.checkInTime}",
                                color = C.greenText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (todayEvening != null && todayEvening.status == AttendanceStatus.ABSENT) {
                            Text(
                                "❌ Absent",
                                color = C.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "Not checked in by ${eveningSession?.windowEnd ?: "window end"}",
                                color = C.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        } else if (isEveningOpen) {
                            Box(
                                Modifier.padding(top = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(C.primary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable { openQR("evening") }
                            ) {
                                Text(
                                    "Show My QR",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                if (eveningSession == null) "Session not scheduled today"
                                else "Check-in closed",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Present",
                    value = present.size,
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Absent",
                    value = absent.size,
                    bg = C.dangerLt,
                    textColor = C.danger,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Streak 🔥",
                    value = present.size,
                    bg = C.primaryLt,
                    textColor = C.primary,
                    modifier = Modifier.weight(1f)
                )
            }

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
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
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

            if (absenceAlertsOn.value && absenceState.isOverThreshold) {
                val notified = absenceState.lastAlertedAt > 0
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (notified) C.warnLt else C.dangerLt)
                        .border(
                            1.dp,
                            if (notified) C.warn.copy(alpha = 0.5f) else C.danger,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠ Absence Threshold Reached",
                        color = C.warn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "You have missed ${absenceState.absenceCount} worship sessions. The limit is $threshold.",
                        color = C.warn,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        if (notified)
                            "Your guardian was notified at ${absenceState.lastAlertedAt} absences" +
                                if (absenceState.absencesSinceAlert > 0)
                                    " and has ${absenceState.absencesSinceAlert} newer miss${if (absenceState.absencesSinceAlert == 1) "" else "es"} to review."
                                else " and has not been re-notified since."
                        else
                            "Your guardian has not been notified yet about these absences.",
                        color = C.warn,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BAttn_History(
    boarder: Boarder,
    sessions: List<com.example.mbhm.model.AttendanceSession>,
    records: List<AttendanceRecord>,
    onBack: () -> Unit
) {
    var fromIso by remember { mutableStateOf("") }
    var toIso by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("all") }

    val sessionById = sessions.associateBy { it.id }
    val filtered = records.filter { r ->
        val session = sessionById[r.sessionId]
        val typeOk = typeFilter == "all" || session?.type?.name?.lowercase() == typeFilter
        typeOk && session != null && withinDateRange(session.date, fromIso, toIso)
    }
    val presentCount = filtered.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = filtered.count { it.status == AttendanceStatus.ABSENT }
    val rate = if (presentCount + absentCount > 0) {
        (presentCount * 100) / (presentCount + absentCount)
    } else 0

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "My Attendance History", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Records",
                    value = filtered.size,
                    sub = "in range",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Attendance Rate",
                    value = "$rate%",
                    sub = "$presentCount present",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Present",
                    value = presentCount,
                    sub = "sessions",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Absent",
                    value = absentCount,
                    sub = "sessions",
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
                value = typeFilter,
                onChange = { typeFilter = it }
            )

            SectionHeader(title = "${filtered.size} record${if (filtered.size == 1) "" else "s"}")

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = "📋",
                    title = "No attendance records",
                    sub = "Try another date range or session filter"
                )
            }

            filtered
                .sortedByDescending { r ->
                    sessionById[r.sessionId]?.let { parseDisplayDate(it.date) } ?: 0L
                }
                .forEach { r ->
                    val session = sessionById[r.sessionId]
                    val isPresent = r.status == AttendanceStatus.PRESENT
                    PCard(padding = PaddingValues(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPresent) C.sageLt else C.dangerLt),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isPresent) "✓" else "✗",
                                    color = if (isPresent) C.greenText else C.danger,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    session?.date ?: "Unknown date",
                                    color = C.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    listOfNotNull(
                                        session?.let {
                                            if (it.type == com.example.mbhm.model.SessionType.MORNING) "☀️ Morning"
                                            else "🌙 Evening"
                                        },
                                        r.checkInTime?.let { "In $it" },
                                        if (r.method == com.example.mbhm.model.AttendanceMethod.QR) "📱 QR Scan"
                                        else "✏️ Manual Check"
                                    ).joinToString(" · "),
                                    color = C.muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                r.overrideReason?.let {
                                    Text(
                                        it,
                                        color = C.muted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(if (isPresent) C.sageLt else C.dangerLt)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (isPresent) "Present" else "Absent",
                                    color = if (isPresent) C.greenText else C.danger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
        }
    }
}
