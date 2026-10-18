package com.example.mbhm.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.HouseJob
import com.example.mbhm.model.HouseRoom
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.SectionHeader

/**
 * Guardian-only screen for managing the room list (with the maximum boarder
 * capacity of each room), the floors and the job options used by the
 * boarder form dropdowns.
 */
@Composable
fun GRooms(
    boarders: List<Boarder>,
    rooms: List<HouseRoom>,
    jobs: List<HouseJob>,
    onSaveRoom: (HouseRoom) -> Unit,
    onDeleteRoom: (String) -> Unit,
    onSaveJob: (HouseJob) -> Unit,
    onDeleteJob: (String) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editingName by remember { mutableStateOf<String?>(null) }
    var editRoomName by remember { mutableStateOf("") }
    var editFloor by remember { mutableStateOf("") }
    var editCapacity by remember { mutableStateOf("") }

    var newName by remember { mutableStateOf("") }
    var newFloor by remember { mutableStateOf("") }
    var newCapacity by remember { mutableStateOf("") }
    var roomError by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    var newJob by remember { mutableStateOf("") }
    var jobError by remember { mutableStateOf<String?>(null) }

    fun occupants(name: String) = boarders.count { it.room == name }

    Screen(title = "Rooms & Capacity", onBack = onBack) {
        Text(
            "Rooms, floors and jobs feed the dropdowns in the boarder form. " +
                "A boarder cannot be assigned to a room that is already full.",
            color = C.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SectionHeader("Rooms")

        if (rooms.isEmpty()) {
            PCard {
                Text(
                    "No rooms yet — add your first room below.",
                    color = C.muted,
                    fontSize = 13.sp
                )
            }
        }

        rooms.forEach { room ->
            val occupied = occupants(room.name)
            val full = occupied >= room.maxCapacity
            PCard {
                if (editingName == room.name) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "EDIT ROOM",
                            color = C.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Input(
                            label = "Room Name / Number",
                            value = editRoomName,
                            onChange = { editRoomName = it }
                        )
                        Input(
                            label = "Floor",
                            value = editFloor,
                            onChange = { editFloor = it }
                        )
                        Input(
                            label = "Max Capacity (boarders)",
                            value = editCapacity,
                            onChange = { editCapacity = it },
                            keyboardType = KeyboardType.Number
                        )
                        ActionPair(
                            onCancel = { editingName = null; editError = null },
                            onConfirm = {
                                val cap = editCapacity.trim().toIntOrNull()
                                if (editRoomName.isBlank()) {
                                    editError = "Room name is required"
                                    return@ActionPair
                                }
                                if (cap == null || cap < 1) {
                                    editError = "Max capacity must be at least 1"
                                    return@ActionPair
                                }
                                val renamed = editRoomName.trim() != room.name
                                if (renamed && rooms.any { it.name == editRoomName.trim() }) {
                                    editError = "A room with that name already exists"
                                    return@ActionPair
                                }
                                editError = null
                                onSaveRoom(HouseRoom(editRoomName.trim(), editFloor.trim(), cap))
                                editingName = null
                                showToast("Room updated")
                            },
                            confirmLabel = "Save"
                        )
                        editError?.let {
                            Text(it, color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Room ${room.name}",
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${room.floor} · $occupied of ${room.maxCapacity} occupied",
                                color = if (full) C.danger else C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Edit",
                                color = C.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    editingName = room.name
                                    editRoomName = room.name
                                    editFloor = room.floor
                                    editCapacity = room.maxCapacity.toString()
                                    roomError = null
                                }
                            )
                            Text(
                                "Delete",
                                color = C.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    if (occupied > 0) {
                                        showToast("Move the $occupied boarder(s) out of room ${room.name} first")
                                    } else {
                                        onDeleteRoom(room.name)
                                        showToast("Room ${room.name} deleted")
                                    }
                                }
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(C.bg)
                    ) {
                        val fraction = if (room.maxCapacity > 0) {
                            occupied.toFloat() / room.maxCapacity
                        } else 0f
                        Box(
                            Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (full) C.danger else C.sage)
                        )
                    }
                }
            }
        }

        PCard {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ADD ROOM",
                    color = C.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Input(
                    label = "Room Name / Number",
                    value = newName,
                    onChange = { newName = it },
                    placeholder = "e.g. 105"
                )
                Input(
                    label = "Floor",
                    value = newFloor,
                    onChange = { newFloor = it },
                    placeholder = "e.g. 1st Floor"
                )
                Input(
                    label = "Max Capacity (boarders)",
                    value = newCapacity,
                    onChange = { newCapacity = it },
                    placeholder = "e.g. 4",
                    keyboardType = KeyboardType.Number
                )
                roomError?.let {
                    Text(it, color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Add Room",
                    onClick = {
                        val cap = newCapacity.trim().toIntOrNull()
                        if (newName.isBlank()) {
                            roomError = "Room name is required"
                            return@PrimaryButton
                        }
                        if (rooms.any { it.name.equals(newName.trim(), ignoreCase = true) }) {
                            roomError = "A room with that name already exists"
                            return@PrimaryButton
                        }
                        if (newFloor.isBlank()) {
                            roomError = "Please enter the floor"
                            return@PrimaryButton
                        }
                        if (cap == null || cap < 1) {
                            roomError = "Max capacity must be at least 1"
                            return@PrimaryButton
                        }
                        roomError = null
                        onSaveRoom(HouseRoom(newName.trim(), newFloor.trim(), cap))
                        newName = ""; newFloor = ""; newCapacity = ""
                        showToast("Room added")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader("Job Options")

        if (jobs.isEmpty()) {
            PCard {
                Text("No job options yet — add one below.", color = C.muted, fontSize = 13.sp)
            }
        }

        jobs.forEach { job ->
            PCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(job.name, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Delete",
                        color = C.danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            onDeleteJob(job.name)
                            showToast("Job removed")
                        }
                    )
                }
            }
        }

        PCard {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ADD JOB",
                    color = C.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Input(
                    label = "Job / Occupation",
                    value = newJob,
                    onChange = { newJob = it },
                    placeholder = "e.g. Student"
                )
                jobError?.let {
                    Text(it, color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Add Job",
                    onClick = {
                        if (newJob.isBlank()) {
                            jobError = "Please enter a job title"
                            return@PrimaryButton
                        }
                        if (jobs.any { it.name.equals(newJob.trim(), ignoreCase = true) }) {
                            jobError = "That job already exists"
                            return@PrimaryButton
                        }
                        jobError = null
                        onSaveJob(HouseJob(newJob.trim()))
                        newJob = ""
                        showToast("Job added")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
