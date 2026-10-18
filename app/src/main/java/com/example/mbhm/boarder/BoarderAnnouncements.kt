package com.example.mbhm.boarder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Announcement
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.Priority
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.DateRangeFilter
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.withinDateRange
import com.example.mbhm.ui.components.isoToday

@Composable
fun BAnnouncements(
    boarder: Boarder,
    announcements: List<Announcement>,
    setAnnouncements: (List<Announcement>) -> Unit,
    onBack: () -> Unit
) {
    var detail by remember { mutableStateOf<Announcement?>(null) }
    var fromIso by remember { mutableStateOf(isoToday()) }
    var toIso by remember { mutableStateOf(isoToday()) }
    var priorityFilter by remember { mutableStateOf("all") }
    var readFilter by remember { mutableStateOf("all") }

    val selected = detail
    if (selected != null) {
        BAnn_Detail(
            announcement = selected,
            boarder = boarder,
            announcements = announcements,
            setAnnouncements = setAnnouncements,
            onBack = { detail = null }
        )
        return
    }

    val mine = announcements.filter { a -> a.isFor(boarder.id) }
    val filtered = mine.filter { a ->
        val typeOk = priorityFilter == "all" ||
            (priorityFilter == "critical") == (a.priority == Priority.CRITICAL)
        val isRead = a.readBy.contains(boarder.id)
        val readOk = readFilter == "all" ||
            (readFilter == "unread") != isRead
        typeOk && readOk && withinDateRange(a.sentAt, fromIso, toIso)
    }
    val unread = mine.count { a -> !a.readBy.contains(boarder.id) }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Announcement", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell(
                    label = "Total",
                    value = mine.size,
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "Unread",
                    value = unread,
                    bg = C.primaryLt,
                    fg = C.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "Critical",
                    value = mine.count { a -> a.priority == Priority.CRITICAL },
                    bg = C.dangerLt,
                    fg = C.danger,
                    modifier = Modifier.weight(1f)
                )
            }

            SectionHeader(title = "Filter by date")
            DateRangeFilter(from = fromIso, to = toIso, onChange = { f, t -> fromIso = f; toIso = t })

            ChipBar(
                options = listOf(
                    "All" to "all",
                    "🔴 Critical" to "critical",
                    "📢 Info" to "normal"
                ),
                value = priorityFilter,
                onChange = { priorityFilter = it }
            )
            ChipBar(
                options = listOf(
                    "All" to "all",
                    "Unread" to "unread",
                    "Read" to "read"
                ),
                value = readFilter,
                onChange = { readFilter = it }
            )

            SectionHeader(title = "${filtered.size} of ${mine.size} announcement")

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = "📢",
                    title = if (mine.isEmpty()) "No announcement yet" else "No announcement matches",
                    sub = if (mine.isEmpty()) "Your guardian will post updates here" else "Try another date range or filter"
                )
            }

            filtered.forEach { a ->
                val isRead = a.readBy.contains(boarder.id)
                val acked = a.acknowledgedBy.contains(boarder.id)
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.card)
                        .border(
                            1.dp,
                            if (!isRead && a.priority == Priority.CRITICAL) C.danger.copy(alpha = 0.35f)
                            else C.border,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { detail = a }
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            if (a.priority == Priority.CRITICAL) "🔴" else "📢",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isRead) {
                                    Box(
                                        Modifier.size(8.dp)
                                            .clip(CircleShape)
                                            .background(C.primary)
                                    )
                                }
                                Text(
                                    a.title,
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
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
                                "${a.sentBy} · ${a.sentAt}",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        if (a.priority == Priority.CRITICAL && acked) {
                            Text("✓ Ack", color = C.greenText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        } else if (isRead) {
                            Text("✓ Read", color = C.greenText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("New", color = C.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    bg: Color = C.card,
    fg: Color = C.text
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (bg == C.card) Modifier.border(1.dp, C.border, RoundedCornerShape(14.dp)) else Modifier)
            .padding(14.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            value.toString(),
            color = fg,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun BAnn_Detail(
    announcement: Announcement,
    boarder: Boarder,
    announcements: List<Announcement>,
    setAnnouncements: (List<Announcement>) -> Unit,
    onBack: () -> Unit
) {
    fun markRead() {
        if (!announcement.readBy.contains(boarder.id)) {
            setAnnouncements(announcements.map { a ->
                if (a.id == announcement.id) a.copy(readBy = a.readBy + boarder.id) else a
            })
        }
    }

    fun acknowledge() {
        setAnnouncements(announcements.map { a ->
            if (a.id == announcement.id) {
                a.copy(
                    acknowledgedBy = a.acknowledgedBy + boarder.id,
                    readBy = if (a.readBy.contains(boarder.id)) a.readBy else a.readBy + boarder.id
                )
            } else a
        })
    }

    LaunchedEffect(Unit) {
        markRead()
    }

    val a = announcements.find { x -> x.id == announcement.id } ?: announcement
    val isRead = a.readBy.contains(boarder.id)
    val isAcked = a.acknowledgedBy.contains(boarder.id)

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Announcement", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.card)
                    .border(1.dp, C.border, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (a.priority == Priority.CRITICAL) "🔴" else "📢", fontSize = 24.sp)
                    if (a.priority == Priority.CRITICAL) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(C.dangerLt)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Critical",
                                color = C.danger,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    a.title,
                    color = C.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(a.body, color = C.text, fontSize = 14.sp, lineHeight = 20.sp)
                Text(
                    "Posted by ${a.sentBy} · ${a.sentAt}",
                    color = C.muted,
                    fontSize = 12.sp
                )
                if (isRead) {
                    Text("✓ Marked as read", color = C.greenText, fontSize = 12.sp)
                }
            }
            if (a.priority == Priority.CRITICAL && !isAcked) {
                PrimaryButton(
                    text = "✓ Acknowledge",
                    onClick = { acknowledge() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (a.priority == Priority.CRITICAL && isAcked) {
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.sageLt)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "✓ You have acknowledged this announcement",
                        color = C.greenText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
