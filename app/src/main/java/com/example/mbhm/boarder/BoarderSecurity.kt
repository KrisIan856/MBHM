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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Announcement
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.CurfewRecord
import com.example.mbhm.model.CurfewStatus
import com.example.mbhm.model.MaintCategory
import com.example.mbhm.model.MaintStatus
import com.example.mbhm.model.MaintenanceReport
import com.example.mbhm.model.Priority
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.Option
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.RowItem
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Select
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.dialPhone

@Composable
fun BSecurity(
    boarder: Boarder,
    announcements: List<Announcement>,
    setAnnouncements: (List<Announcement>) -> Unit,
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    curfew: List<CurfewRecord>,
    setCurfew: (List<CurfewRecord>) -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember { mutableStateOf("overview") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var sosModal by remember { mutableStateOf(false) }

    fun nav(s: String, d: Any? = null) {
        screen = s
        screenData = d
    }

    if (screen == "ann-detail") {
        BAnn_Detail(
            announcement = screenData as Announcement,
            boarder = boarder,
            announcements = announcements,
            setAnnouncements = setAnnouncements,
            onBack = { nav("overview") }
        )
        return
    }
    if (screen == "maintenance") {
        B_Maintenance(
            boarder = boarder,
            maintenance = maintenance,
            setMaintenance = setMaintenance,
            showToast = showToast,
            onBack = { nav("overview") }
        )
        return
    }
    if (screen == "curfew") {
        B_Curfew(
            boarder = boarder,
            curfew = curfew,
            setCurfew = setCurfew,
            showToast = showToast,
            onBack = { nav("overview") }
        )
        return
    }
    if (screen == "emergency-contact") {
        B_EmergencyContact(boarder = boarder, showToast = showToast, onBack = { nav("overview") })
        return
    }

    val myAnn = announcements.filter { a -> a.isFor(boarder.id) }
    val unread = myAnn.count { a -> !a.readBy.contains(boarder.id) }
    val myCurfew = curfew.find { c -> c.boarderId == boarder.id }

    var annRight: (@Composable () -> Unit)? = null
    if (unread > 0) {
        annRight = {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(C.danger),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    unread.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "Security & Emergency")
            ScrollBody(modifier = Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.dangerLt)
                        .border(2.dp, Color(0xFFF5C4C4), RoundedCornerShape(14.dp))
                        .clickable { sosModal = true }
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(56.dp)
                            .clip(CircleShape)
                            .background(C.danger),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🆘", fontSize = 30.sp)
                    }
                    Column {
                        Text(
                            "Emergency SOS",
                            color = C.danger,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap to send instant alert to guardian",
                            color = C.danger,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.card)
                        .border(1.dp, C.border, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "🌙 Curfew — 10:00 PM",
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Sep 7, 2026",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (myCurfew != null) {
                            val chipBg = when (myCurfew.status) {
                                CurfewStatus.COMPLIANT -> C.sageLt
                                CurfewStatus.PENDING -> C.primaryLt
                                else -> C.dangerLt
                            }
                            val chipFg = when (myCurfew.status) {
                                CurfewStatus.COMPLIANT -> C.greenText
                                CurfewStatus.PENDING -> C.primary
                                else -> C.danger
                            }
                            val chipText = when (myCurfew.status) {
                                CurfewStatus.COMPLIANT -> "✓ In"
                                CurfewStatus.PENDING -> "⏳ Pending"
                                else -> "⚠ Late"
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(chipBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    chipText,
                                    color = chipFg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    val checkedInAt = myCurfew?.checkedInAt
                    if (checkedInAt != null) {
                        Text(
                            "Checked in at $checkedInAt",
                            color = C.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (myCurfew?.status == CurfewStatus.PENDING) {
                        Box(
                            Modifier.fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.primary)
                                .clickable { nav("curfew") }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Check In Now",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RowItem(
                        icon = "📞",
                        label = "Emergency Contact",
                        sub = "${boarder.emergencyContact.name} · ${boarder.emergencyContact.relationship}",
                        onClick = { nav("emergency-contact") }
                    )
                    RowItem(
                        icon = "📢",
                        label = "Announcements",
                        sub = if (unread > 0) "$unread unread" else "All read",
                        onClick = { nav("ann-list") },
                        right = annRight
                    )
                }

                if (screen == "overview") {
                    Column(Modifier.fillMaxWidth()) {
                        SectionHeader(title = "Announcements")
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            myAnn.forEach { a ->
                                val read = a.readBy.contains(boarder.id)
                                val acked = a.acknowledgedBy.contains(boarder.id)
                                Column(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(C.card)
                                        .border(
                                            1.dp,
                                            if (!read && a.priority == Priority.CRITICAL) Color(0xFFF5C4C4)
                                            else C.border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { nav("ann-detail", a) }
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
                                                if (!read) {
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
                                                a.sentAt,
                                                color = C.muted,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                        if (a.priority == Priority.CRITICAL && !acked && read) {
                                            Box(
                                                Modifier.clip(RoundedCornerShape(50))
                                                    .background(C.warnLt)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Ack req.",
                                                    color = C.warn,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Modal(open = sosModal, onClose = { sosModal = false }, title = "Send Emergency SOS?") {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🆘", fontSize = 60.sp)
                Text(
                    buildAnnotatedString {
                        append("This will immediately alert ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = C.text)) {
                            append("Ate Sandra")
                        }
                        append(" of an emergency. Only use in genuine emergencies.")
                    },
                    color = C.muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            ActionPair(
                onCancel = { sosModal = false },
                onConfirm = {
                    sosModal = false
                    showToast("🆘 SOS alert sent! Guardian has been notified.")
                },
                confirmLabel = "Send SOS Alert",
                confirmDanger = true
            )
        }
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

    Screen(title = "Announcement", onBack = onBack) {
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

@Composable
private fun B_EmergencyContact(
    boarder: Boarder,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var formName by remember { mutableStateOf(boarder.emergencyContact.name) }
    var formRelationship by remember { mutableStateOf(boarder.emergencyContact.relationship) }
    var formPhone by remember { mutableStateOf(boarder.emergencyContact.phone) }
    val context = LocalContext.current

    Screen(
        title = "Emergency Contact",
        onBack = onBack,
        right = {
            Text(
                if (editing) "Cancel" else "✏ Edit",
                color = C.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { editing = !editing }
            )
        }
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.dangerLt)
                .border(1.dp, Color(0xFFF5C4C4), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(
                "⚠ Emergency Contact Info",
                color = C.danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "This person will be contacted in case of emergency. Keep it updated.",
                color = C.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (editing) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Input(
                    label = "Full Name",
                    value = formName,
                    onChange = { formName = it }
                )
                Input(
                    label = "Relationship",
                    value = formRelationship,
                    onChange = { formRelationship = it }
                )
                Input(
                    label = "Phone Number",
                    value = formPhone,
                    onChange = { formPhone = it }
                )
                PrimaryButton(
                    text = "Save Contact",
                    onClick = {
                        editing = false
                        showToast("Emergency contact updated")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.card)
                    .border(1.dp, C.border, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(C.dangerLt),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 24.sp)
                }
                Text(
                    formName,
                    color = C.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(formRelationship, color = C.muted, fontSize = 14.sp)
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.sage)
                        .clickable { dialPhone(context, formPhone) }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📞 $formPhone",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun B_Maintenance(
    boarder: Boarder,
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var screen by remember { mutableStateOf("list") }
    var category by remember { mutableStateOf(MaintCategory.PLUMBING) }
    var description by remember { mutableStateOf("") }

    val myReports = maintenance.filter { m -> m.boarderId == boarder.id }

    val catEmoji = mapOf(
        MaintCategory.PLUMBING to "🚿",
        MaintCategory.ELECTRICAL to "💡",
        MaintCategory.FURNITURE to "🛋️",
        MaintCategory.APPLIANCE to "❄️",
        MaintCategory.OTHER to "🔧"
    )

    fun submit() {
        setMaintenance(
            maintenance + MaintenanceReport(
                id = "m" + System.currentTimeMillis(),
                boarderId = boarder.id,
                room = boarder.room,
                category = category,
                description = description,
                status = MaintStatus.OPEN,
                submittedAt = "Sep 7, now"
            )
        )
        showToast("Maintenance request submitted")
        description = ""
        screen = "list"
    }

    if (screen == "new") {
        Screen(title = "New Request", onBack = { screen = "list" }) {
            Select(
                label = "Category",
                value = category.name.lowercase(),
                onChange = { v ->
                    category = MaintCategory.values().first { it.name.lowercase() == v }
                },
                options = MaintCategory.values().map { c ->
                    Option(c.name.lowercase(), "${catEmoji[c]} ${c.label}")
                }
            )
            Textarea(
                label = "Describe the issue",
                value = description,
                onChange = { description = it },
                placeholder = "Describe what's broken or needs fixing...",
                rows = 5
            )
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.warnLt)
                    .border(1.dp, Color(0xFFF0DCA8), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Room ${boarder.room} · Photo attachment coming soon",
                    color = C.warn,
                    fontSize = 12.sp
                )
            }
            PrimaryButton(
                text = "Submit Request",
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank(),
                height = 56
            )
        }
        return
    }

    Screen(
        title = "Maintenance",
        onBack = onBack,
        right = {
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(C.primaryLt)
                    .clickable { screen = "new" }
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
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            myReports.forEach { m ->
                PCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(catEmoji[m.category] ?: "🔧", fontSize = 20.sp)
                            Text(
                                m.category.label,
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val statusBg = when (m.status) {
                            MaintStatus.OPEN -> C.dangerLt
                            MaintStatus.IN_PROGRESS -> C.warnLt
                            MaintStatus.RESOLVED -> C.sageLt
                        }
                        val statusFg = when (m.status) {
                            MaintStatus.OPEN -> C.danger
                            MaintStatus.IN_PROGRESS -> C.warn
                            MaintStatus.RESOLVED -> C.greenText
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                m.status.name.lowercase().replace('_', ' '),
                                color = statusFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        m.description,
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Submitted ${m.submittedAt}",
                        color = C.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (m.resolvedAt != null) {
                        Text(
                            "Resolved ${m.resolvedAt}",
                            color = C.greenText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            if (myReports.isEmpty()) {
                EmptyState(
                    icon = "🔧",
                    title = "No reports yet",
                    sub = "Tap + New to report an issue"
                )
            }
        }
    }
}

@Composable
private fun B_Curfew(
    boarder: Boarder,
    curfew: List<CurfewRecord>,
    setCurfew: (List<CurfewRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    val my = curfew.find { c -> c.boarderId == boarder.id }
    val isIn = my?.status == CurfewStatus.COMPLIANT

    fun checkIn() {
        setCurfew(curfew.map { c ->
            if (c.boarderId == boarder.id) {
                c.copy(status = CurfewStatus.COMPLIANT, checkedInAt = "Just now")
            } else c
        })
        showToast("✅ Curfew check-in recorded")
    }

    fun checkOut() {
        setCurfew(curfew.map { c ->
            if (c.boarderId == boarder.id) {
                c.copy(status = CurfewStatus.PENDING, checkedInAt = null)
            } else c
        })
        showToast("Checked out. Please be back by 10:00 PM.")
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Curfew Check-in", onBack = onBack)
        Column(
            Modifier.fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isIn) "🏠" else "🌙",
                    fontSize = 60.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    if (isIn) "You are checked in" else "Curfew: 10:00 PM",
                    color = C.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sep 7, 2026" + (my?.checkedInAt?.let { " · Checked in: $it" } ?: ""),
                    color = C.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isIn) C.sageLt else C.warnLt)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isIn) "You are inside and accounted for" else "Please check in before 10:00 PM",
                    color = if (isIn) C.greenText else C.warn,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            if (!isIn) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.sage)
                        .clickable { checkIn() }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✅ Check In Now",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.dangerLt)
                        .clickable { checkOut() }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Going Out",
                        color = C.danger,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
