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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*

@Composable
fun GSecurity(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    announcements: List<Announcement>,
    setAnnouncements: (List<Announcement>) -> Unit,
    incidents: List<Incident>,
    setIncidents: (List<Incident>) -> Unit,
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    curfew: List<CurfewRecord>,
    setCurfew: (List<CurfewRecord>) -> Unit,
    deepLink: String?,
    onDeepLinkConsumed: () -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember { mutableStateOf(deepLink ?: "overview") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    fun nav(s: String, d: Any? = null) {
        screen = s
        screenData = d
    }

    LaunchedEffect(deepLink) {
        if (deepLink != null) onDeepLinkConsumed()
    }

    if (screen == "contacts") {
        GSec_Contacts(boarders, setBoarders, showToast, onBack = { nav("overview") })
        return
    }
    if (screen == "boarders") {
        GSec_BoardersList(boarders, setBoarders, showToast, onBack = { nav("overview") })
        return
    }
    if (screen == "announcements") {
        GSec_Announcements(boarders, announcements, setAnnouncements, showToast, onBack = { nav("overview") })
        return
    }
    if (screen == "curfew") {
        GSec_Curfew(boarders, curfew, setCurfew, showToast, onBack = { nav("overview") })
        return
    }
    if (screen == "incidents") {
        GSec_Incidents(boarders, incidents, setIncidents, maintenance, showToast, onBack = { nav("overview") })
        return
    }

    val activeSOS = incidents.filter { it.type == IncidentType.SOS && !it.resolved }
    val curfewPending = curfew.count { it.status == CurfewStatus.PENDING }
    val curfewLate = curfew.count { it.status == CurfewStatus.LATE }

    Screen(title = "Security & Emergency") {
        if (activeSOS.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFE4E4))
                    .border(2.dp, C.danger, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🆘 Active SOS Alert", color = C.danger, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                activeSOS.forEach { i ->
                    val b = boarders.find { it.id == i.boarderId }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                b?.name ?: "",
                                color = C.danger,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(i.description, color = C.danger, fontSize = 12.sp)
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(C.danger)
                                .clickable {
                                    setIncidents(
                                        incidents.map { inc ->
                                            if (inc.id == i.id) inc.copy(resolved = true) else inc
                                        }
                                    )
                                    showToast("SOS resolved")
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Resolve",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                secNavCard(
                    Modifier.weight(1f),
                    "📞",
                    "Emergency Contacts",
                    "${boarders.size} boarders",
                    false
                ) { nav("contacts") }
                secNavCard(
                    Modifier.weight(1f),
                    "📢",
                    "Announcements",
                    "${announcements.size} sent",
                    false
                ) { nav("announcements") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                secNavCard(
                    Modifier.weight(1f),
                    "🌙",
                    "Curfew Compliance",
                    "$curfewLate late · $curfewPending pending",
                    curfewLate > 0
                ) { nav("curfew") }
                secNavCard(
                    Modifier.weight(1f),
                    "📋",
                    "Incident Log",
                    "${incidents.count { !it.resolved }} unresolved",
                    false
                ) { nav("incidents") }
            }
        }

        Column(Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "Incident Log",
                action = {
                    Text(
                        "View all",
                        color = C.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { nav("incidents") }
                    )
                }
            )
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                incidents.take(3).forEach { i ->
                    PCard(padding = PaddingValues(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IncidentBadge(type = i.type)
                                    if (i.resolved) {
                                        Box(
                                            Modifier.clip(RoundedCornerShape(50))
                                                .background(C.sageLt)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "Resolved",
                                                color = C.greenText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                Text(
                                    i.title,
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    i.timestamp,
                                    color = C.muted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun secNavCard(
    modifier: Modifier,
    icon: String,
    label: String,
    sub: String,
    alert: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, if (alert) Color(0xFFF0DCA8) else C.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Text(
            label,
            color = C.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            sub,
            color = if (alert) C.warn else C.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private data class GContactForm(val name: String, val relationship: String, val phone: String)

@Composable
private fun GSec_Contacts(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf(GContactForm("", "", "")) }
    val context = LocalContext.current

    fun startEdit(b: Boarder) {
        editing = b.id
        form = GContactForm(
            b.emergencyContact.name,
            b.emergencyContact.relationship,
            b.emergencyContact.phone
        )
    }

    fun saveEdit() {
        setBoarders(
            boarders.map { b ->
                if (b.id == editing) {
                    b.copy(
                        emergencyContact = EmergencyContact(form.name, form.relationship, form.phone)
                    )
                } else b
            }
        )
        editing = null
        showToast("Contact updated")
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Emergency Contacts", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            boarders.forEach { b ->
                PCard {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(initials = b.initials, color = b.avatarColor)
                        Column(Modifier.weight(1f)) {
                            Text(
                                b.name,
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Room ${b.room}", color = C.muted, fontSize = 12.sp)
                        }
                        Text(
                            "Edit",
                            color = C.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { startEdit(b) }
                        )
                    }

                    Column(Modifier.fillMaxWidth()) {
                        HorizontalDivider(thickness = 1.dp, color = C.border)
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                            Text(
                                "BOARDER DIRECT",
                                color = C.muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        b.name,
                                        color = C.text,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        b.phone,
                                        color = C.muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(b.email, color = C.muted, fontSize = 12.sp)
                                }
                                Box(
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(C.sageLt)
                                        .clickable { dialPhone(context, b.phone) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "📞 Call",
                                        color = C.greenText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (editing == b.id) {
                        Column(Modifier.fillMaxWidth()) {
                            HorizontalDivider(thickness = 1.dp, color = C.border)
                            Column(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "EMERGENCY CONTACT",
                                    color = C.muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Input(
                                    label = "Contact Name",
                                    value = form.name,
                                    onChange = { v -> form = form.copy(name = v) }
                                )
                                Input(
                                    label = "Relationship",
                                    value = form.relationship,
                                    onChange = { v -> form = form.copy(relationship = v) }
                                )
                                Input(
                                    label = "Phone",
                                    value = form.phone,
                                    onChange = { v -> form = form.copy(phone = v) }
                                )
                                ActionPair(
                                    onCancel = { editing = null },
                                    onConfirm = { saveEdit() },
                                    confirmLabel = "Save"
                                )
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth()) {
                            HorizontalDivider(thickness = 1.dp, color = C.border)
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                                Text(
                                    "EMERGENCY CONTACT",
                                    color = C.muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f, fill = false)) {
                                        Text(
                                            b.emergencyContact.name,
                                            color = C.text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${b.emergencyContact.relationship} · ${b.emergencyContact.phone}",
                                            color = C.muted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Box(
                                        Modifier.padding(start = 8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(C.dangerLt)
                                            .clickable { dialPhone(context, b.emergencyContact.phone) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "📞 Call",
                                            color = C.danger,
                                            fontSize = 12.sp,
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

@Composable
private fun GSec_BoardersList(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var editRoom by remember { mutableStateOf("") }
    var editFloor by remember { mutableStateOf("") }

    fun startEdit(b: Boarder) {
        editingId = b.id
        editRoom = b.room
        editFloor = b.floor
    }

    fun saveEdit() {
        val b = boarders.find { it.id == editingId }
        if (b != null) {
            setBoarders(
                boarders.map {
                    if (it.id == editingId) {
                        it.copy(room = editRoom.trim(), floor = editFloor.trim())
                    } else it
                }
            )
            editingId = null
            showToast("Room assignment updated for ${b.name}")
        }
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Boarders List", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Text(
                "${boarders.size} boarders registered",
                color = C.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            boarders.forEach { b ->
                PCard {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(initials = b.initials, color = b.avatarColor)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    b.name,
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Room ${b.room} · ${b.job}", color = C.muted, fontSize = 12.sp)
                            }
                            Text(
                                if (editingId == b.id) "Save" else "✏ Edit",
                                color = C.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    if (editingId == b.id) {
                                        if (editRoom.isNotBlank()) {
                                            saveEdit()
                                        } else {
                                            showToast("Room number is required")
                                        }
                                    } else {
                                        startEdit(b)
                                    }
                                }
                            )
                        }

                        if (editingId == b.id) {
                            HorizontalDivider(thickness = 1.dp, color = C.border)
                            Column(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Input(
                                    label = "Room Number",
                                    value = editRoom,
                                    onChange = { editRoom = it }
                                )
                                Input(
                                    label = "Floor",
                                    value = editFloor,
                                    onChange = { editFloor = it }
                                )
                            }
                        }
                    }
                }
            }

            if (boarders.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No boarders registered yet", color = C.muted, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun GSec_Announcements(
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
    val ann = selected

    fun send() {
        val newAnn = Announcement(
            id = "ann" + System.currentTimeMillis(),
            title = title,
            body = body,
            priority = if (priority == "critical") Priority.CRITICAL else Priority.NORMAL,
            recipients = null,
            sentAt = "Sep 7 · now",
            sentBy = "Ate Sandra",
            readBy = emptyList(),
            acknowledgedBy = emptyList()
        )
        setAnnouncements(listOf(newAnn) + announcements)
        showToast("Announcement sent")
        view = "list"
        title = ""
        body = ""
    }

    if (view == "detail" && ann != null) {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "Announcement", onBack = { view = "list" })
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
                        secProgress(
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

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(
            title = "Announcements",
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
            announcements.forEach { a ->
                PCard(modifier = Modifier.clickable {
                    selected = a
                    view = "detail"
                }) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
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
                                    Text(a.sentAt, color = C.muted, fontSize = 12.sp)
                                }
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
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            secProgress(
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
            if (announcements.isEmpty()) {
                EmptyState(
                    icon = "📢",
                    title = "No announcements yet",
                    sub = "Tap + New to create one"
                )
            }
        }
    }
}

@Composable
private fun secProgress(modifier: Modifier, fraction: Float, heightDp: Int) {
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

@Composable
private fun GSec_Curfew(
    boarders: List<Boarder>,
    curfew: List<CurfewRecord>,
    setCurfew: (List<CurfewRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf("all") }

    val filtered = if (filter == "all") curfew else curfew.filter { it.status.name.lowercase() == filter }
    val counts = mapOf(
        "compliant" to curfew.count { it.status == CurfewStatus.COMPLIANT },
        "late" to curfew.count { it.status == CurfewStatus.LATE },
        "absent" to curfew.count { it.status == CurfewStatus.ABSENT },
        "pending" to curfew.count { it.status == CurfewStatus.PENDING }
    )

    fun flag(id: String) {
        setCurfew(curfew.map { c -> if (c.id == id) c.copy(status = CurfewStatus.ABSENT) else c })
        showToast("Boarder flagged as absent")
    }

    Screen(title = "Curfew Compliance", onBack = onBack) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sep 7, 2026", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(C.dangerLt)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Curfew: 10:00 PM",
                    color = C.danger,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("In", counts.getValue("compliant"), C.sageLt) to C.greenText,
                Triple("Late", counts.getValue("late"), C.dangerLt) to C.danger,
                Triple("Absent", counts.getValue("absent"), Color(0xFFF5E4E4)) to Color(0xFF9B0000),
                Triple("Pending", counts.getValue("pending"), C.primaryLt) to C.primary
            ).forEach { (stat, tc) ->
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(stat.third)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stat.second.toString(),
                        color = tc,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stat.first,
                        color = tc,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        ChipBar(
            options = listOf(
                "All" to "all",
                "✓ In" to "compliant",
                "⚠ Late" to "late",
                "✕ Absent" to "absent",
                "⏳ Pending" to "pending"
            ),
            value = filter,
            onChange = { filter = it }
        )

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.forEach { c ->
                val b = boarders.find { it.id == c.boarderId } ?: return@forEach
                PCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(initials = b.initials, color = b.avatarColor)
                        Column(Modifier.weight(1f)) {
                            Text(
                                b.name,
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Room ${b.room}" + (c.checkedInAt?.let { " · Checked in: $it" } ?: ""),
                                color = C.muted,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurfewChip(status = c.status)
                            if (c.status == CurfewStatus.PENDING) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(C.dangerLt)
                                        .clickable { flag(c.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Flag",
                                        color = C.danger,
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

@Composable
private fun GSec_Incidents(
    boarders: List<Boarder>,
    incidents: List<Incident>,
    setIncidents: (List<Incident>) -> Unit,
    maintenance: List<MaintenanceReport>,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    val filtered = if (filter == "all") incidents else incidents.filter { it.type.name.lowercase() == filter }

    fun resolve(id: String) {
        setIncidents(incidents.map { i -> if (i.id == id) i.copy(resolved = true) else i })
        showToast("Incident resolved")
    }

    Screen(title = "Incident Log", onBack = onBack) {
        ChipBar(
            options = listOf(
                "All" to "all",
                "🌙 Curfew" to "missed_curfew",
                "🆘 SOS" to "sos",
                "📅 Worship" to "missed_worship",
                "🔧 Maint." to "maintenance"
            ),
            value = filter,
            onChange = { filter = it }
        )

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.forEach { i ->
                val b = boarders.find { it.id == i.boarderId }
                PCard(borderColor = if (i.resolved) C.border else C.dangerLt) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IncidentBadge(type = i.type)
                                if (i.resolved) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(50))
                                            .background(C.sageLt)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Resolved",
                                            color = C.greenText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Text(
                                i.title,
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                i.description,
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (b != null) {
                                Row(
                                    Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(initials = b.initials, color = b.avatarColor, size = 20)
                                    Text(b.name, color = C.muted, fontSize = 12.sp)
                                }
                            }
                            Text(
                                i.timestamp,
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        if (!i.resolved) {
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(C.sage)
                                    .clickable { resolve(i.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Resolve",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                EmptyState(icon = "✅", title = "No incidents", sub = "All clear!")
            }
        }
    }
}
