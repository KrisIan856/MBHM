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
import com.example.mbhm.model.AttendanceStatus
import com.example.mbhm.model.Boarder
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.QRCodeDisplay
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.StatCard

@Composable
fun BAttendance(
    boarder: Boarder,
    attnRecords: List<AttendanceRecord>,
    setAttnRecords: (List<AttendanceRecord>) -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember { mutableStateOf("main") }
    var activeSession by remember { mutableStateOf("morning") }

    val myRecords = attnRecords.filter { r -> r.boarderId == boarder.id }
    val present = myRecords.filter { r -> r.status == AttendanceStatus.PRESENT }
    val absent = myRecords.filter { r -> r.status == AttendanceStatus.ABSENT }
    val todayMorning = myRecords.find { r -> r.sessionId == "s1" }

    val isMorningOpen = true
    val isEveningOpen = false

    val presentDays = listOf(1, 2, 3, 4, 5, 7)
    val absentDays = listOf(6)
    val firstDayOffset = 2

    fun openQR(sess: String) {
        activeSession = sess
        screen = "qr"
    }

    if (screen == "qr") {
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
                        if (activeSession == "morning") "6:00 AM – 8:00 AM · Sep 7, 2026"
                        else "8:00 PM – 10:00 PM · Sep 7, 2026",
                        color = C.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.shadow(8.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, C.border, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        QRCodeDisplay(seed = "boarder-${boarder.id}", size = 220)
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
        Header(title = "Attendance")
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
                SectionHeader(title = "Today — Sep 7, 2026")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (todayMorning?.status == AttendanceStatus.PRESENT) C.sageLt
                                else C.card
                            )
                            .border(1.dp, C.border, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "☀️ Morning",
                            color = C.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "6:00–8:00 AM",
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
                                "Window closed",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Column(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.card)
                            .border(1.dp, C.border, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "🌙 Evening",
                            color = C.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "8:00–10:00 PM",
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (isEveningOpen) {
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
                                "Opens at 8:00 PM",
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
                    "September 2026",
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
                    List<Int?>(firstDayOffset) { null } + (1..30).toList() + List<Int?>(3) { null }
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
                                    val isPresent = day in presentDays
                                    val isAbsentDay = day in absentDays
                                    val isToday = day == 7
                                    val isFuture = day > 7
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
                                        isFuture -> Color(0xFFC4BFB9)
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
                                        Text(
                                            day.toString(),
                                            color = cellFg,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
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

            if (absent.size >= 3) {
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "You have ${absent.size} missed worship sessions. The guardian has been notified.",
                        color = C.warn,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
