package com.example.mbhm.guardian

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Announcement
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.Priority
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.DateRangeFilter
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.withinDateRange
import com.example.mbhm.ui.components.isoToday
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GAnnouncements(
    boarders: List<Boarder>,
    announcements: List<Announcement>,
    setAnnouncements: (List<Announcement>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var view by remember { mutableStateOf("list") }
    var selected by remember { mutableStateOf<Announcement?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    var recipients by remember { mutableStateOf("all") }
    var fromIso by remember { mutableStateOf(isoToday()) }
    var toIso by remember { mutableStateOf(isoToday()) }
    var priorityFilter by remember { mutableStateOf("all") }
    val ann = selected

    fun send() {
        val newAnn = Announcement(
            id = "ann" + System.currentTimeMillis(),
            title = title,
            body = body,
            priority = if (priority == "critical") Priority.CRITICAL else Priority.NORMAL,
            recipients = null,
            sentAt = SimpleDateFormat("MMM d · h:mm a", Locale.ENGLISH).format(Date()),
            sentBy = "Guardian",
            readBy = emptyList(),
            acknowledgedBy = emptyList()
        )
        setAnnouncements(listOf(newAnn) + announcements)
        showToast("Announcement sent")
        view = "list"
        title = ""
        body = ""
        priority = "normal"
    }

    if (view == "detail" && ann != null) {
        AnnDetail(
            ann = ann,
            boarders = boarders,
            onBack = { view = "list" }
        )
        return
    }

    if (view == "compose") {
        Screen(title = "New Announcement", onBack = { view = "list" }) {
            Input(
                label = "Title",
                value = title,
                onChange = { title = it },
                placeholder = "Brief, clear subject..."
            )
            Textarea(
                label = "Message",
                value = body,
                onChange = { body = it },
                placeholder = "Write your message here...",
                rows = 5
            )
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Priority",
                    color = C.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("normal" to "📢 Normal", "critical" to "🔴 Critical").forEach { (p, label) ->
                        val selectedP = priority == p
                        Box(
                            Modifier.weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedP) (if (p == "critical") C.danger else C.primary)
                                    else C.card
                                )
                                .then(
                                    if (selectedP) Modifier
                                    else Modifier.border(1.dp, C.border, RoundedCornerShape(12.dp))
                                )
                                .clickable { priority = p },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selectedP) Color.White else C.muted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Recipients",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        Modifier.fillMaxWidth().clickable { recipients = "all" },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(16.dp)
                                .clip(RoundedCornerShape(50))
                                .border(
                                    2.dp,
                                    if (recipients == "all") C.primary else C.border,
                                    RoundedCornerShape(50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (recipients == "all") {
                                Box(
                                    Modifier.size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(C.primary)
                                )
                            }
                        }
                        Text("All boarders", color = C.text, fontSize = 14.sp)
                    }
                }
            }

            PrimaryButton(
                text = "Send Announcement",
                onClick = { send() },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && body.isNotBlank()
            )
        }
        return
    }

    val filteredAnnouncements = announcements.filter { a ->
        val matchesPriority = priorityFilter == "all" ||
            (priorityFilter == "critical" && a.priority == Priority.CRITICAL) ||
            (priorityFilter == "normal" && a.priority == Priority.NORMAL)
        matchesPriority && withinDateRange(a.sentAt, fromIso, toIso)
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(
            title = "Announcement",
            onBack = onBack,
            right = {
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(C.primaryLt)
                        .clickable { view = "compose" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "+ New",
                        color = C.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            SectionHeader(title = "Filter by date")
            DateRangeFilter(from = fromIso, to = toIso, onChange = { f, t -> fromIso = f; toIso = t })

            ChipBar(
                options = listOf(
                    "All" to "all",
                    "🔴 Critical" to "critical",
                    "📢 Normal" to "normal"
                ),
                value = priorityFilter,
                onChange = { priorityFilter = it }
            )

            Text(
                "${filteredAnnouncements.size} of ${announcements.size} announcement",
                color = C.muted,
                fontSize = 12.sp
            )

            filteredAnnouncements.forEach { a ->
                PCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selected = a
                            view = "detail"
                        },
                    padding = PaddingValues(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            Text(
                                a.sentAt,
                                color = C.muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            a.title,
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            a.body,
                            color = C.muted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            annProgress(
                                Modifier.weight(1f),
                                if (boarders.isEmpty()) 0f else a.readBy.size.toFloat() / boarders.size,
                                6
                            )
                            Text(
                                "${a.readBy.size}/${boarders.size} read",
                                color = C.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            if (filteredAnnouncements.isEmpty()) {
                EmptyState(
                    icon = "📢",
                    title = if (announcements.isEmpty()) "No announcement yet" else "No announcement matches",
                    sub = if (announcements.isEmpty()) "Tap + New to create one" else "Adjust the date or priority filter"
                )
            }
        }
    }
}

@Composable
private fun AnnDetail(
    ann: Announcement,
    boarders: List<Boarder>,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Announcement", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 16) {
            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (ann.priority == Priority.CRITICAL) "🔴" else "📢",
                            fontSize = 16.sp
                        )
                        if (ann.priority == Priority.CRITICAL) {
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
                        ann.title,
                        color = C.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(ann.body, color = C.text, fontSize = 14.sp)
                    Text(
                        "Sent ${ann.sentAt} by ${ann.sentBy}",
                        color = C.muted,
                        fontSize = 12.sp
                    )
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Read by ${ann.readBy.size}/${boarders.size} boarders",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    annProgress(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        if (boarders.isEmpty()) 0f else ann.readBy.size.toFloat() / boarders.size,
                        8
                    )
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        boarders.forEach { b ->
                            val read = ann.readBy.contains(b.id)
                            val acked = ann.acknowledgedBy.contains(b.id)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(initials = b.initials, color = b.avatarColor, size = 32)
                                Text(
                                    b.name,
                                    color = if (read) C.text else C.muted,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (read) {
                                    Text("✓ Read", color = C.sage, fontSize = 12.sp)
                                } else {
                                    Text("Unread", color = C.muted, fontSize = 12.sp)
                                }
                                if (ann.priority == Priority.CRITICAL && acked) {
                                    Text("✓ Ack", color = C.greenText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun annProgress(modifier: Modifier, fraction: Float, heightDp: Int) {
    Box(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(50))
            .background(C.bg)
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(C.sage, RoundedCornerShape(50))
        )
    }
}
