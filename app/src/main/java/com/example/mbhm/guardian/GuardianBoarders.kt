package com.example.mbhm.guardian

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.HouseJob
import com.example.mbhm.model.HouseRoom
import com.example.mbhm.model.MonthAccount
import com.example.mbhm.model.NewBoarderInput
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.currentPeriod
import com.example.mbhm.model.monthAccount
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Option
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Select
import com.example.mbhm.ui.components.StatCard
import com.example.mbhm.ui.components.TableCard
import com.example.mbhm.ui.components.TableColumn
import com.example.mbhm.ui.components.TableRow
import com.example.mbhm.ui.components.money
import kotlinx.coroutines.launch
import java.util.Locale

private val boarderTableColumns = listOf(
    TableColumn("Boarder", 1.7f),
    TableColumn("ID", 0.85f),
    TableColumn("Rent", 0.85f, end = true),
    TableColumn("Manage", 0.7f, end = true)
)

/**
 * Guardian-only directory and management page for every boarder. It combines the
 * roster overview with the create/edit/password actions that used to live under
 * Security & Emergency.
 */
@Composable
fun GBoarders(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    setBoarders: (List<Boarder>) -> Unit,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    onCreateBoarder: suspend (NewBoarderInput) -> String?,
    onSetPassword: suspend (String, String) -> Boolean,
    onViewPayments: (Boarder) -> Unit,
    onViewAttendance: (Boarder) -> Unit,
    showToast: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val period = remember { currentPeriod() }
    val accounts = remember(boarders, payments, period) {
        boarders.associate { it.id to monthAccount(it, payments, period) }
    }
    val occupiedRooms = remember(boarders) {
        boarders.map { it.room.trim() }.filter { it.isNotEmpty() }.distinct().size
    }
    val unassigned = remember(boarders) { boarders.count { it.room.isBlank() } }

    fun occupants(roomName: String) = boarders.count { it.room == roomName }

    fun saveEdit(
        boarder: Boarder,
        newRoom: String,
        newFloor: String,
        newJob: String,
        newPassword: String
    ) {
        val roomName = newRoom.trim()
        if (roomName.isBlank()) {
            showToast("Please select a room")
            return
        }
        if (roomName != boarder.room) {
            val room = rooms.find { it.name == roomName }
            if (room != null && occupants(roomName) >= room.maxCapacity) {
                showToast("Room $roomName is full (${room.maxCapacity}/${room.maxCapacity})")
                return
            }
        }
        val pwd = newPassword.trim()
        if (pwd.isNotEmpty()) {
            passwordError(pwd)?.let { showToast(it); return }
        }
        setBoarders(
            boarders.map {
                if (it.id == boarder.id) {
                    it.copy(room = roomName, floor = newFloor.trim(), job = newJob.trim())
                } else {
                    it
                }
            }
        )
        editingId = null
        if (pwd.isNotEmpty()) {
            val idNumber = boarder.idNumber.ifBlank { boarder.id }
            scope.launch {
                val ok = onSetPassword(idNumber, pwd)
                showToast(
                    if (ok) "Room updated and password changed for ${boarder.name}"
                    else "Room updated, but the password could not be changed"
                )
            }
        } else {
            showToast("Room assignment updated for ${boarder.name}")
        }
    }

    if (adding) {
        AddBoarderScreen(
            rooms = rooms,
            jobs = jobs,
            occupants = { occupants(it) },
            onBack = { adding = false },
            onCreate = onCreateBoarder,
            onCreated = { adding = false },
            showToast = showToast
        )
        return
    }

    BackHandler(enabled = editingId != null) { editingId = null }

    val visibleBoarders = remember(boarders, query) {
        val needle = query.trim().lowercase(Locale.ENGLISH)
        boarders
            .filter { b ->
                needle.isEmpty() || listOf(
                    b.name,
                    b.room,
                    b.floor,
                    b.idNumber,
                    b.job
                ).any { it.lowercase(Locale.ENGLISH).contains(needle) }
            }
            .sortedWith(
                compareBy(
                    { it.room.ifBlank { "\uFFFF" }.lowercase(Locale.ENGLISH) },
                    { it.name.lowercase(Locale.ENGLISH) }
                )
            )
    }

    Screen(title = "Boarder Directory") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Boarders",
                value = boarders.size,
                sub = "registered",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Rooms",
                value = occupiedRooms,
                sub = when {
                    boarders.isEmpty() -> "no boarder data"
                    unassigned > 0 -> "$unassigned unassigned"
                    else -> "all assigned"
                },
                modifier = Modifier.weight(1f)
            )
        }

        PrimaryButton(
            text = "+ Add Boarder",
            onClick = {
                editingId = null
                adding = true
            },
            modifier = Modifier.fillMaxWidth()
        )

        Input(
            value = query,
            onChange = { query = it },
            label = "Find a boarder",
            placeholder = "Search name, room, ID, or job"
        )

        if (visibleBoarders.isEmpty()) {
            EmptyState(
                icon = "👥",
                title = if (boarders.isEmpty()) "No boarders yet" else "No matches",
                sub = if (boarders.isEmpty()) {
                    "Tap Add Boarder to create the first account."
                } else {
                    "Try a different name, room, or ID number."
                }
            )
        } else {
            SectionHeader(
                title = if (query.isBlank()) "All boarders" else "Search results",
                action = {
                    Text(
                        "${visibleBoarders.size} shown",
                        color = C.muted,
                        fontSize = 11.sp
                    )
                }
            )
            TableCard(boarderTableColumns) {
                visibleBoarders.forEachIndexed { index, boarder ->
                    val account = accounts[boarder.id]
                    val expanded = editingId == boarder.id
                    TableRow(
                        columns = boarderTableColumns,
                        divider = index > 0,
                        onClick = {
                            editingId = if (expanded) null else boarder.id
                        },
                        cells = listOf(
                            {
                                Column {
                                    Text(
                                        boarder.name,
                                        color = C.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        listOf(boarderLocation(boarder), boarder.job.trim())
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        color = C.primary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            },
                            {
                                Text(
                                    boarder.idNumber.ifBlank { "—" },
                                    color = C.text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            {
                                BoarderRentLabel(account)
                            },
                            {
                                Text(
                                    if (expanded) "Close" else "Manage ›",
                                    color = C.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    )
                    if (expanded) {
                        BoarderExpandedPanel(
                            boarder = boarder,
                            account = account,
                            rooms = rooms,
                            jobs = jobs,
                            occupants = { occupants(it) },
                            onPay = { onViewPayments(boarder) },
                            onAttendance = { onViewAttendance(boarder) },
                            onClose = { editingId = null },
                            onSave = { room, floor, job, password ->
                                saveEdit(boarder, room, floor, job, password)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun boarderLocation(boarder: Boarder): String {
    val room = boarder.room.trim()
    val floor = boarder.floor.trim().takeUnless { it == "—" || it == "-" }.orEmpty()
    return when {
        room.isEmpty() && floor.isEmpty() -> "Room unassigned"
        room.isEmpty() -> "Room unassigned · $floor"
        floor.isEmpty() -> "Room $room"
        else -> "Room $room · $floor"
    }
}

private fun boarderGuardianLabel(boarder: Boarder): String =
    listOf(boarder.guardianContact.name, boarder.guardianContact.phone)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
        .ifBlank { "—" }

@Composable
private fun BoarderRentLabel(account: MonthAccount?) {
    val (label, color) = when {
        account == null -> "—" to C.muted
        account.due == 0 -> "No rent" to C.muted
        account.status == com.example.mbhm.model.PayStatus.PAID -> "Paid" to C.greenText
        account.status == com.example.mbhm.model.PayStatus.PARTIAL -> "Partial" to C.primary
        account.status == com.example.mbhm.model.PayStatus.OVERDUE -> "Overdue" to C.danger
        else -> "Pending" to C.warn
    }
    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BoarderExpandedPanel(
    boarder: Boarder,
    account: MonthAccount?,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    occupants: (String) -> Int,
    onPay: () -> Unit,
    onAttendance: () -> Unit,
    onClose: () -> Unit,
    onSave: (room: String, floor: String, job: String, password: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.bg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DirectoryField(
                label = "Phone",
                value = boarder.phone.ifBlank { "—" },
                modifier = Modifier.weight(1f)
            )
            DirectoryField(
                label = "Guardian",
                value = boarderGuardianLabel(boarder),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DirectoryField(
                label = "Job",
                value = boarder.job.ifBlank { "—" },
                modifier = Modifier.weight(1f)
            )
            DirectoryField(
                label = "Outstanding",
                value = when {
                    account == null -> "—"
                    account.due == 0 -> "No rent set"
                    account.balance == 0 -> "None"
                    else -> money(account.balance)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RosterActionPill("💳", "Pay", C.primaryLt, C.primary, onPay)
                RosterActionPill("📅", "Attendance", C.primaryLt, C.primary, onAttendance)
            }
            RosterActionPill(
                icon = "✕",
                label = "Close",
                bg = C.card,
                fg = C.text,
                onClick = onClose
            )
        }

        BoarderEditPanel(
            boarder = boarder,
            rooms = rooms,
            jobs = jobs,
            occupants = occupants,
            onCancel = onClose,
            onSave = onSave
        )
    }
}

@Composable
private fun BoarderEditPanel(
    boarder: Boarder,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    occupants: (String) -> Int,
    onCancel: () -> Unit,
    onSave: (room: String, floor: String, job: String, password: String) -> Unit
) {
    var room by remember(boarder.id) { mutableStateOf(boarder.room) }
    var floor by remember(boarder.id) { mutableStateOf(boarder.floor) }
    var job by remember(boarder.id) { mutableStateOf(boarder.job) }
    var password by remember(boarder.id) { mutableStateOf("") }

    fun roomOptions(): List<Option> = rooms.map { r ->
        Option(r.name, "${r.name} (${occupants(r.name)}/${r.maxCapacity})")
    }

    fun floorOptions(): List<Option> {
        val floors = rooms.map { it.floor }.filter { it.isNotBlank() }.distinct().sorted()
        val all = if (floor.isNotBlank() && floor !in floors && floor != "—") {
            floors + floor
        } else {
            floors
        }
        return all.map { Option(it, it) }
    }

    fun jobOptions(): List<Option> {
        val names = jobs.map { it.name }.distinct().sorted()
        val all = if (job.isNotBlank() && job !in names && job != "—") {
            names + job
        } else {
            names
        }
        return all.map { Option(it, it) }
    }

    PCard(bg = C.card, borderColor = C.border) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                value = room,
                onChange = { value ->
                    room = value
                    rooms.find { it.name == value }?.let { floor = it.floor }
                },
                options = roomOptions()
            )
            Select(
                label = "Floor",
                value = floor,
                onChange = { floor = it },
                options = floorOptions()
            )
            Select(
                label = "Job",
                value = job,
                onChange = { job = it },
                options = jobOptions()
            )
            Input(
                label = "New Password (leave blank to keep the current one)",
                value = password,
                onChange = { password = it }
            )
            ActionPair(
                onCancel = onCancel,
                onConfirm = { onSave(room, floor, job, password) },
                confirmLabel = "Save"
            )
        }
    }
}

@Composable
private fun AddBoarderScreen(
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    occupants: (String) -> Int,
    onBack: () -> Unit,
    onCreate: suspend (NewBoarderInput) -> String?,
    onCreated: () -> Unit,
    showToast: (String) -> Unit
) {
    BackHandler { onBack() }

    var name by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var job by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun roomOptions(): List<Option> = rooms.map { r ->
        Option(r.name, "${r.name} (${occupants(r.name)}/${r.maxCapacity})")
    }

    fun floorOptions(): List<Option> {
        val floors = rooms.map { it.floor }.filter { it.isNotBlank() }.distinct().sorted()
        val all = if (floor.isNotBlank() && floor !in floors && floor != "—") {
            floors + floor
        } else {
            floors
        }
        return all.map { Option(it, it) }
    }

    fun jobOptions(): List<Option> {
        val names = jobs.map { it.name }.distinct().sorted()
        val all = if (job.isNotBlank() && job !in names && job != "—") {
            names + job
        } else {
            names
        }
        return all.map { Option(it, it) }
    }

    fun submit() {
        error = null
        if (name.isBlank()) { error = "Please enter the boarder's full name"; return }
        if (idNumber.isBlank()) { error = "Please enter an ID number"; return }
        if (room.isBlank()) { error = "Please select a room"; return }
        val monthlyRate = rate.trim().toIntOrNull()
        if (monthlyRate == null || monthlyRate < 0) {
            error = "Please enter a valid monthly payment"
            return
        }
        val selectedRoom = rooms.find { it.name == room }
        if (selectedRoom != null && occupants(room) >= selectedRoom.maxCapacity) {
            error = "Room $room is full (${selectedRoom.maxCapacity}/${selectedRoom.maxCapacity})"
            return
        }
        passwordError(password)?.let { error = it; return }
        if (password != confirm) { error = "Passwords do not match"; return }

        adding = true
        scope.launch {
            val result = onCreate(
                NewBoarderInput(
                    name = name.trim(),
                    idNumber = idNumber.trim(),
                    room = room.trim(),
                    floor = floor.trim(),
                    job = job.trim(),
                    monthlyRate = rate.trim(),
                    password = password,
                    confirm = confirm
                )
            )
            adding = false
            if (result == null) {
                showToast("Boarder account created")
                onCreated()
            } else {
                error = result
            }
        }
    }

    Screen(title = "Add Boarder", onBack = onBack) {
        Text(
            "Only the basic info is needed. You issue the ID number and create the password.",
            color = C.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Input(label = "Full Name", value = name, onChange = { name = it })
        Input(
            label = "ID Number (used to sign in)",
            value = idNumber,
            onChange = { idNumber = it },
            placeholder = "e.g. MBH-0001"
        )
        Select(
            label = "Room",
            value = room,
            onChange = { value ->
                room = value
                rooms.find { it.name == value }?.let { floor = it.floor }
            },
            options = roomOptions()
        )
        Select(
            label = "Floor",
            value = floor,
            onChange = { floor = it },
            options = floorOptions()
        )
        Select(
            label = "Job",
            value = job,
            onChange = { job = it },
            options = jobOptions()
        )
        Input(
            label = "Monthly Payment (₱)",
            value = rate,
            onChange = { value -> rate = value.filter { it.isDigit() } },
            placeholder = "e.g. 3500",
            keyboardType = KeyboardType.Number
        )
        Input(
            label = "Password",
            value = password,
            onChange = { password = it }
        )
        Input(
            label = "Confirm Password",
            value = confirm,
            onChange = { confirm = it }
        )

        error?.let {
            Text(it, color = C.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        PrimaryButton(
            text = if (adding) "Creating…" else "Create Boarder Account",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !adding
        )
    }
}

@Composable
private fun RosterActionPill(
    icon: String,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
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
private fun DirectoryField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label.uppercase(Locale.ENGLISH),
            color = C.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            color = C.text,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun passwordError(password: String): String? = when {
    password.length < 10 -> "Password must be at least 10 characters"
    !password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } ->
        "Password must contain uppercase and lowercase letters"
    !password.any { it.isDigit() } -> "Password must contain at least one number"
    !password.any { !it.isLetterOrDigit() } -> "Password must contain a special character (e.g. !@#)"
    else -> null
}
