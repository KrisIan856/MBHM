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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.*
import com.example.mbhm.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GSecurity(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    incidents: List<Incident>,
    setIncidents: (List<Incident>) -> Unit,
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    guardianProfile: GuardianProfile?,
    onCreateBoarder: suspend (NewBoarderInput) -> String?,
    onSetPassword: suspend (String, String) -> Boolean,
    deepLink: String?,
    onDeepLinkConsumed: () -> Unit,
    onViewPayments: (Boarder) -> Unit,
    onViewAttendance: (Boarder) -> Unit,
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
        GSec_BoardersList(
            boarders, setBoarders, showToast,
            rooms = rooms,
            jobs = jobs,
            guardianProfile = guardianProfile,
            onCreateBoarder = onCreateBoarder,
            onSetPassword = onSetPassword,
            onViewPayments = onViewPayments,
            onViewAttendance = onViewAttendance,
            onBack = { nav("overview") }
        )
        return
    }
    if (screen == "incidents") {
        GSec_Incidents(boarders, incidents, setIncidents, maintenance, setMaintenance, showToast, onBack = { nav("overview") })
        return
    }

    val activeSOS = incidents.filter { it.type == IncidentType.SOS && !it.resolved }
    val context = LocalContext.current

    Screen(title = "Security & Emergency") {
        if (activeSOS.isNotEmpty() &&
            rememberPrefBoolean("g_notif_sos", true).value
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.dangerLt)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val phone = b?.phone
                            if (!phone.isNullOrBlank()) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(50))
                                        .background(C.dangerLt)
                                        .clickable { dialPhone(context, phone) }
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
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                secNavCard(
                    Modifier.weight(1f),
                    "📞",
                    "Emergency Contacts",
                    "Boarder contact directory",
                    false
                ) { nav("contacts") }
                secNavCard(
                    Modifier.weight(1f),
                    "📋",
                    "Incident Log",
                    "${incidents.count { !it.resolved }} unresolved",
                    false
                ) { nav("incidents") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                secNavCard(
                    Modifier.weight(1f),
                    "👥",
                    "Boarders",
                    "${boarders.size} registered",
                    false
                ) { nav("boarders") }
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
            .border(1.dp, if (alert) C.warn.copy(alpha = 0.5f) else C.border, RoundedCornerShape(14.dp))
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

/** Column layouts shared by every Security table so they read the same way. */
private val contactColumns = listOf(
    TableColumn("Boarder", 1.15f),
    TableColumn("Room", 0.45f, end = true),
    TableColumn("Emergency", 1.1f, end = true),
    TableColumn("", 0.7f, end = true)
)

private val securityBoarderColumns = listOf(
    TableColumn("Boarder", 1.15f),
    TableColumn("Room", 0.45f, end = true),
    TableColumn("", 1.45f, end = true)
)

private val incidentColumns = listOf(
    TableColumn("Incident", 1.5f),
    TableColumn("When", 0.85f, end = true),
    TableColumn("", 0.9f, end = true)
)

private val maintenanceColumns = listOf(
    TableColumn("Request", 1.35f),
    TableColumn("Room", 0.45f, end = true),
    TableColumn("Status", 0.8f, end = true),
    TableColumn("", 0.85f, end = true)
)

private data class GContactForm(val name: String, val phone: String)

@Composable
private fun GSec_Contacts(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<String?>(null) }
    var openId by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf(GContactForm("", "")) }
    val context = LocalContext.current
    val showContacts = rememberPrefBoolean("g_privacy_show_contacts", true).value

    fun startEdit(b: Boarder) {
        editing = b.id
        form = GContactForm(
            b.emergencyContact.name,
            b.emergencyContact.phone
        )
    }

    fun saveEdit() {
        setBoarders(
            boarders.map { b ->
                if (b.id == editing) {
                    b.copy(
                        emergencyContact = EmergencyContact(form.name, form.phone)
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
            boarders.forEachIndexed { index, b ->
                TableRow(
                    columns = contactColumns,
                    divider = index > 0,
                    onClick = { openId = if (openId == b.id) null else b.id },
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
                                    if (showContacts) b.phone else b.email,
                                    color = C.muted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        },
                        {
                            Text(b.room, color = C.muted, fontSize = 12.sp)
                        },
                        {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    b.emergencyContact.name,
                                    color = C.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (showContacts)
                                        b.emergencyContact.phone
                                    else "Number on file",
                                    color = C.muted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        },
                        {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showContacts) {
                                    Text(
                                        "📞",
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable { dialPhone(context, b.phone) }
                                    )
                                }
                                Text(
                                    "Edit",
                                    color = C.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { startEdit(b) }
                                )
                            }
                        }
                    )
                )

                if (editing == b.id || openId == b.id) {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.bg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "BOARDER",
                            color = C.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            b.name,
                            color = C.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(b.email, color = C.muted, fontSize = 12.sp)
                        if (showContacts) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b.phone, color = C.muted, fontSize = 12.sp)
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

                        HorizontalDivider(thickness = 1.dp, color = C.border)

                        Text(
                            "EMERGENCY CONTACT",
                            color = C.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        if (editing == b.id) {
                            Input(
                                label = "Contact Name",
                                value = form.name,
                                onChange = { v -> form = form.copy(name = v) }
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
                        } else {
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
                                        if (showContacts)
                                            b.emergencyContact.phone
                                        else "Number on file",
                                        color = C.muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (showContacts) {
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
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    guardianProfile: GuardianProfile?,
    onCreateBoarder: suspend (NewBoarderInput) -> String?,
    onSetPassword: suspend (String, String) -> Boolean,
    onViewPayments: (Boarder) -> Unit,
    onViewAttendance: (Boarder) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("list") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editRoom by remember { mutableStateOf("") }
    var editFloor by remember { mutableStateOf("") }
    var editJob by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }

    var addName by remember { mutableStateOf("") }
    var addId by remember { mutableStateOf("") }
    var addRoom by remember { mutableStateOf("") }
    var addFloor by remember { mutableStateOf("") }
    var addJob by remember { mutableStateOf("") }
    var addRate by remember { mutableStateOf("") }
    var addPassword by remember { mutableStateOf("") }
    var addConfirm by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }

    fun occupants(roomName: String) = boarders.count { it.room == roomName }

    fun roomOptions(): List<Option> = rooms.map { r ->
        Option(r.name, "${r.name} (${occupants(r.name)}/${r.maxCapacity})")
    }

    fun floorOptions(current: String): List<Option> {
        val floors = rooms.map { it.floor }.filter { it.isNotBlank() }.distinct().sorted()
        val all = if (current.isNotBlank() && current !in floors && current != "—") {
            floors + current
        } else floors
        return all.map { Option(it, it) }
    }

    fun jobOptions(current: String): List<Option> {
        val names = jobs.map { it.name }.distinct().sorted()
        val all = if (current.isNotBlank() && current !in names && current != "—") {
            names + current
        } else names
        return all.map { Option(it, it) }
    }

    fun startEdit(b: Boarder) {
        editingId = b.id
        editRoom = b.room
        editFloor = b.floor
        editJob = b.job
        editPassword = ""
    }

    fun validPassword(pwd: String): String? = when {
        pwd.length < 6 -> "Password must be at least 6 characters"
        !pwd.any { it.isUpperCase() } || !pwd.any { it.isLowerCase() } ->
            "Password must contain uppercase and lowercase letters"
        !pwd.any { it.isDigit() } -> "Password must contain at least one number"
        !pwd.any { !it.isLetterOrDigit() } -> "Password must contain a special character (e.g. !@#)"
        else -> null
    }

    fun saveEdit() {
        val b = boarders.find { it.id == editingId } ?: return
        val newRoom = editRoom.trim()
        if (newRoom.isBlank()) {
            showToast("Please select a room")
            return
        }
        if (newRoom != b.room) {
            val room = rooms.find { it.name == newRoom }
            if (room != null && occupants(newRoom) >= room.maxCapacity) {
                showToast("Room $newRoom is full (${room.maxCapacity}/${room.maxCapacity})")
                return
            }
        }
        val pwd = editPassword.trim()
        if (pwd.isNotEmpty()) {
            validPassword(pwd)?.let { showToast(it); return }
        }
        setBoarders(
            boarders.map {
                if (it.id == editingId) {
                    it.copy(room = newRoom, floor = editFloor.trim(), job = editJob.trim())
                } else it
            }
        )
        editingId = null
        if (pwd.isNotEmpty()) {
            val idNumber = b.idNumber.ifBlank { b.id }
            scope.launch {
                val ok = onSetPassword(idNumber, pwd)
                showToast(
                    if (ok) "Room updated and password changed for ${b.name}"
                    else "Room updated, but the password could not be changed"
                )
            }
        } else {
            showToast("Room assignment updated for ${b.name}")
        }
    }

    fun submitAdd() {
        addError = null
        if (addName.isBlank()) { addError = "Please enter the boarder's full name"; return }
        if (addId.isBlank()) { addError = "Please enter an ID number"; return }
        if (addRoom.isBlank()) { addError = "Please select a room"; return }
        val rate = addRate.trim().toIntOrNull()
        if (rate == null || rate < 0) { addError = "Please enter a valid monthly payment"; return }
        val room = rooms.find { it.name == addRoom }
        if (room != null && occupants(addRoom) >= room.maxCapacity) {
            addError = "Room $addRoom is full (${room.maxCapacity}/${room.maxCapacity})"
            return
        }
        val pwdErr = validPassword(addPassword)
        if (pwdErr != null) { addError = pwdErr; return }
        if (addPassword != addConfirm) { addError = "Passwords do not match"; return }

        adding = true
        scope.launch {
            val err = onCreateBoarder(
                NewBoarderInput(
                    name = addName.trim(),
                    idNumber = addId.trim(),
                    room = addRoom.trim(),
                    floor = addFloor.trim(),
                    job = addJob.trim(),
                    monthlyRate = addRate.trim(),
                    password = addPassword,
                    confirm = addConfirm
                )
            )
            adding = false
            if (err == null) {
                mode = "list"
                addName = ""; addId = ""; addRoom = ""; addFloor = ""; addJob = ""
                addRate = ""
                addPassword = ""; addConfirm = ""; addError = null
                showToast("Boarder account created")
            } else {
                addError = err
            }
        }
    }

    if (mode == "add") {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "Add Boarder", onBack = { mode = "list"; addError = null })
            ScrollBody(modifier = Modifier.weight(1f), spacing = 14) {
                Text(
                    "Only the basic info is needed. You issue the ID number and create the password.",
                    color = C.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Input(label = "Full Name", value = addName, onChange = { addName = it })
                Input(
                    label = "ID Number (used to sign in)",
                    value = addId,
                    onChange = { addId = it },
                    placeholder = "e.g. MBH-0001"
                )
                Select(
                    label = "Room",
                    value = addRoom,
                    onChange = { v ->
                        addRoom = v
                        rooms.find { it.name == v }?.let { addFloor = it.floor }
                    },
                    options = roomOptions()
                )
                Select(
                    label = "Floor",
                    value = addFloor,
                    onChange = { addFloor = it },
                    options = floorOptions(addFloor)
                )
                Select(
                    label = "Job",
                    value = addJob,
                    onChange = { addJob = it },
                    options = jobOptions(addJob)
                )
                Input(
                    label = "Monthly Payment (₱)",
                    value = addRate,
                    onChange = { v -> addRate = v.filter { it.isDigit() } },
                    placeholder = "e.g. 3500",
                    keyboardType = KeyboardType.Number
                )
                Input(
                    label = "Password",
                    value = addPassword,
                    onChange = { addPassword = it }
                )
                Input(
                    label = "Confirm Password",
                    value = addConfirm,
                    onChange = { addConfirm = it }
                )

                addError?.let {
                    Text(it, color = C.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                PrimaryButton(
                    text = if (adding) "Creating…" else "Create Boarder Account",
                    onClick = { submitAdd() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !adding
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Boarders List", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.primary)
                    .clickable {
                        editingId = null
                        mode = "add"
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Add Boarder", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "${boarders.size} boarders registered",
                color = C.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            boarders.forEachIndexed { index, b ->
                TableRow(
                    columns = securityBoarderColumns,
                    divider = index > 0,
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
                                    "ID ${b.idNumber.ifBlank { "—" }} · ${b.job}",
                                    color = C.muted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        },
                        {
                            Text(b.room, color = C.muted, fontSize = 12.sp)
                        },
                        {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SecActionPill("💳", "Pay", C.primaryLt, C.primary) { onViewPayments(b) }
                                SecActionPill("📅", "Attn", C.primaryLt, C.primary) { onViewAttendance(b) }
                                Text(
                                    if (editingId == b.id) "✓" else "✏",
                                    color = C.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        if (editingId == b.id) {
                                            saveEdit()
                                        } else {
                                            startEdit(b)
                                        }
                                    }
                                )
                            }
                        }
                    )
                )

                if (editingId == b.id) {
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.bg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "ROOM ASSIGNMENT & ACCOUNT",
                            color = C.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Select(
                            label = "Room Number",
                            value = editRoom,
                            onChange = { v ->
                                editRoom = v
                                rooms.find { it.name == v }?.let { editFloor = it.floor }
                            },
                            options = roomOptions()
                        )
                        Select(
                            label = "Floor",
                            value = editFloor,
                            onChange = { editFloor = it },
                            options = floorOptions(editFloor)
                        )
                        Select(
                            label = "Job",
                            value = editJob,
                            onChange = { editJob = it },
                            options = jobOptions(editJob)
                        )
                        Input(
                            label = "New Password (leave blank to keep the current one)",
                            value = editPassword,
                            onChange = { editPassword = it }
                        )
                        ActionPair(
                            onCancel = { editingId = null; editPassword = "" },
                            onConfirm = { saveEdit() },
                            confirmLabel = "Save"
                        )
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
private fun SecActionPill(
    icon: String,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 11.sp)
        Text(
            label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )
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
private fun GSec_Incidents(
    boarders: List<Boarder>,
    incidents: List<Incident>,
    setIncidents: (List<Incident>) -> Unit,
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var filter by remember { mutableStateOf("all") }
    val filtered = if (filter == "all") incidents else incidents.filter { it.type.name.lowercase() == filter }

    fun resolve(id: String) {
        setIncidents(incidents.map { i -> if (i.id == id) i.copy(resolved = true) else i })
        showToast("Incident resolved")
    }

    fun advanceMaintenance(id: String) {
        val now = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
        setMaintenance(
            maintenance.map { m ->
                if (m.id == id) {
                    val next = when (m.status) {
                        MaintStatus.OPEN -> MaintStatus.IN_PROGRESS
                        MaintStatus.IN_PROGRESS -> MaintStatus.RESOLVED
                        MaintStatus.RESOLVED -> MaintStatus.OPEN
                    }
                    m.copy(
                        status = next,
                        resolvedAt = if (next == MaintStatus.RESOLVED) now else null
                    )
                } else m
            }
        )
        showToast("Maintenance status updated")
    }

    Screen(title = "Incident Log", onBack = onBack) {
        ChipBar(
            options = listOf(
                "All" to "all",
                "🆘 SOS" to "sos",
                "📅 Worship" to "missed_worship",
                "🔧 Maint." to "maintenance"
            ),
            value = filter,
            onChange = { filter = it }
        )

        if (filtered.isEmpty()) {
            EmptyState(icon = "✅", title = "No incidents", sub = "All clear!")
        } else {
            TableCard(incidentColumns) {
                filtered.forEachIndexed { index, i ->
                    val b = boarders.find { it.id == i.boarderId }
                    TableRow(
                        columns = incidentColumns,
                        divider = index > 0,
                        cells = listOf(
                            {
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            i.title,
                                            color = C.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
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
                                        i.description,
                                        color = C.muted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Row(
                                        Modifier.padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IncidentBadge(type = i.type)
                                        if (b != null) {
                                            Avatar(
                                                initials = b.initials,
                                                color = b.avatarColor,
                                                size = 16
                                            )
                                            Text(
                                                b.name,
                                                color = C.muted,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            },
                            {
                                Text(
                                    i.timestamp,
                                    color = C.muted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            {
                                if (i.resolved) {
                                    Text(
                                        "✓ Resolved",
                                        color = C.greenText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(C.sage)
                                            .clickable { resolve(i.id) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            "Resolve",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }

        if (filter == "all" || filter == "maintenance") {
            val visibleMaintenance = if (filter == "maintenance") maintenance else maintenance.filter { it.status != MaintStatus.RESOLVED }
            if (visibleMaintenance.isNotEmpty()) {
                SectionHeader("Maintenance Requests")
                TableCard(maintenanceColumns) {
                    visibleMaintenance.forEachIndexed { index, m ->
                        val b = boarders.find { it.id == m.boarderId }
                        TableRow(
                            columns = maintenanceColumns,
                            divider = index > 0,
                            cells = listOf(
                                {
                                    Column {
                                        Text(
                                            m.category.label,
                                            color = C.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            m.description,
                                            color = C.muted,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (b != null) {
                                            Row(
                                                Modifier.padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Avatar(
                                                    initials = b.initials,
                                                    color = b.avatarColor,
                                                    size = 16
                                                )
                                                Text(
                                                    b.name,
                                                    color = C.muted,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                },
                                {
                                    Text(m.room, color = C.muted, fontSize = 12.sp)
                                },
                                {
                                    Text(
                                        when (m.status) {
                                            MaintStatus.OPEN -> "Open"
                                            MaintStatus.IN_PROGRESS -> "Working"
                                            MaintStatus.RESOLVED -> "Resolved"
                                        },
                                        color = when (m.status) {
                                            MaintStatus.OPEN -> C.danger
                                            MaintStatus.IN_PROGRESS -> C.warn
                                            MaintStatus.RESOLVED -> C.greenText
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(if (m.status == MaintStatus.RESOLVED) C.sage else C.primary)
                                            .clickable { advanceMaintenance(m.id) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            when (m.status) {
                                                MaintStatus.OPEN -> "Start"
                                                MaintStatus.IN_PROGRESS -> "Resolve"
                                                MaintStatus.RESOLVED -> "Reopen"
                                            },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            )
                        )
                    }
                }
            } else if (filter == "maintenance") {
                EmptyState(icon = "🔧", title = "No maintenance requests")
            }
        }
    }
}
