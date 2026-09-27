package com.example.mbhm.boarder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.data.SampleData
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.EmergencyContact
import com.example.mbhm.model.GuardianContact
import com.example.mbhm.model.LeaveNotice
import com.example.mbhm.model.LeaveNoticeStatus
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.RowItem
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.Toggle
import com.example.mbhm.ui.components.money

@Composable
fun BMore(
    boarder: Boarder,
    onLogout: () -> Unit,
    showToast: (String) -> Unit
) {
    var currentBoarder by remember { mutableStateOf(boarder) }
    var screen by remember { mutableStateOf("main") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Emergency & Guardian Edit State
    var showEditContactsModal by remember { mutableStateOf(false) }
    var editEmName by remember { mutableStateOf(currentBoarder.emergencyContact.name) }
    var editEmRel by remember { mutableStateOf(currentBoarder.emergencyContact.relationship) }
    var editEmPhone by remember { mutableStateOf(currentBoarder.emergencyContact.phone) }
    var editEmSecPhone by remember { mutableStateOf(currentBoarder.emergencyContact.secondaryPhone ?: "") }
    var editEmAddress by remember { mutableStateOf(currentBoarder.emergencyContact.address ?: "") }

    var editGName by remember { mutableStateOf(currentBoarder.guardianContact.name) }
    var editGRel by remember { mutableStateOf(currentBoarder.guardianContact.relationship) }
    var editGPhone by remember { mutableStateOf(currentBoarder.guardianContact.phone) }
    var editGAltPhone by remember { mutableStateOf(currentBoarder.guardianContact.altPhone ?: "") }
    var editGAddress by remember { mutableStateOf(currentBoarder.guardianContact.address ?: "") }
    var contactsError by remember { mutableStateOf<String?>(null) }

    // Leave Notice State
    var leaveNoticeList by remember { mutableStateOf(SampleData.leaveNotices.filter { it.boarderId == currentBoarder.id }) }
    var showLeaveModal by remember { mutableStateOf(false) }
    var leaveStep by remember { mutableStateOf("form") } // "form" or "review"
    var lLeaveDate by remember { mutableStateOf("Sep 20, 2026") }
    var lDepTime by remember { mutableStateOf("08:00 AM") }
    var lRetDate by remember { mutableStateOf("Sep 22, 2026") }
    var lRetTime by remember { mutableStateOf("06:00 PM") }
    var lReason by remember { mutableStateOf("") }
    var lDest by remember { mutableStateOf("") }
    var lNotes by remember { mutableStateOf("") }
    var leaveFormError by remember { mutableStateOf<String?>(null) }

    if (screen == "leave_history") {
        Screen(title = "Leave Notices", onBack = { screen = "main" }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.primary)
                    .clickable {
                        lReason = ""
                        lDest = ""
                        lNotes = ""
                        leaveFormError = null
                        leaveStep = "form"
                        showLeaveModal = true
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ File New Leave Notice",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                leaveNoticeList.forEach { notice ->
                    LeaveNoticeCard(notice)
                }
                if (leaveNoticeList.isEmpty()) {
                    EmptyState(icon = "📅", title = "No leave notices filed yet")
                }
            }
        }
        return
    }

    if (screen == "profile") {
        Screen(title = "My Profile", onBack = { screen = "main" }) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Avatar(initials = currentBoarder.initials, color = currentBoarder.avatarColor, size = 72)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currentBoarder.name,
                        color = C.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        currentBoarder.job,
                        color = C.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            PCard {
                Text(
                    "Room & Tenancy",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(
                    "Room" to "Room ${currentBoarder.room}",
                    "Floor" to currentBoarder.floor,
                    "Move-in Date" to currentBoarder.joinDate,
                    "Monthly Rate" to money(currentBoarder.monthlyRate),
                    "Due Day" to "Every ${currentBoarder.dueDay}th of month",
                    "Grace Period" to "${currentBoarder.gracePeriodDays} days"
                ).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = C.muted, fontSize = 13.sp)
                        Text(
                            value,
                            color = C.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            PCard {
                Text(
                    "Contact Information",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(
                    "Phone" to currentBoarder.phone,
                    "Email" to currentBoarder.email
                ).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = C.muted, fontSize = 13.sp)
                        Text(
                            value,
                            color = C.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Emergency Contact Section
            PCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Emergency Contact",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "✏ Edit",
                        color = C.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            editEmName = currentBoarder.emergencyContact.name
                            editEmRel = currentBoarder.emergencyContact.relationship
                            editEmPhone = currentBoarder.emergencyContact.phone
                            editEmSecPhone = currentBoarder.emergencyContact.secondaryPhone ?: ""
                            editEmAddress = currentBoarder.emergencyContact.address ?: ""

                            editGName = currentBoarder.guardianContact.name
                            editGRel = currentBoarder.guardianContact.relationship
                            editGPhone = currentBoarder.guardianContact.phone
                            editGAltPhone = currentBoarder.guardianContact.altPhone ?: ""
                            editGAddress = currentBoarder.guardianContact.address ?: ""
                            contactsError = null
                            showEditContactsModal = true
                        }
                    )
                }

                val em = currentBoarder.emergencyContact
                listOf(
                    "Name" to em.name,
                    "Relationship" to em.relationship,
                    "Contact Number" to em.phone,
                    "Secondary Number" to (em.secondaryPhone ?: "N/A"),
                    "Address" to (em.address ?: "N/A")
                ).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = C.muted, fontSize = 13.sp)
                        Text(
                            value,
                            color = C.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Guardian Contact Section
            PCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Guardian Contact",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val gc = currentBoarder.guardianContact
                listOf(
                    "Guardian Name" to gc.name,
                    "Relationship" to gc.relationship,
                    "Contact Number" to gc.phone,
                    "Alternative Number" to (gc.altPhone ?: "N/A"),
                    "Guardian Address" to (gc.address ?: "N/A")
                ).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = C.muted, fontSize = 13.sp)
                        Text(
                            value,
                            color = C.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        return
    }

    if (screen == "settings") {
        Screen(title = "Settings", onBack = { screen = "main" }) {
            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Notifications",
                        color = C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Toggle(
                        label = "Payment Reminders",
                        sub = "In-app reminder before rent is due",
                        checked = true,
                        onChange = { showToast("Notification settings updated") }
                    )
                    Toggle(
                        label = "Attendance Reminders",
                        sub = "Alert before worship session starts",
                        checked = true,
                        onChange = { showToast("Notification settings updated") }
                    )
                    Toggle(
                        label = "Announcement Alerts",
                        sub = "Notify me of new announcements",
                        checked = true,
                        onChange = { showToast("Notification settings updated") }
                    )
                }
            }

            PCard {
                Text(
                    "App Info",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    "Version" to "1.0.0",
                    "Build" to "2026.02"
                ).forEach { (label, value) ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = C.muted, fontSize = 12.sp)
                        Text(
                            value,
                            color = C.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        return
    }

    Screen(title = "More") {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
                .clickable { screen = "profile" }
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(initials = currentBoarder.initials, color = currentBoarder.avatarColor, size = 60)
            Column(Modifier.weight(1f)) {
                Text(
                    currentBoarder.name,
                    color = C.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Room ${currentBoarder.room} · ${currentBoarder.job}",
                    color = C.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    currentBoarder.phone,
                    color = C.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text("View →", color = C.primary, fontSize = 14.sp)
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RowItem(
                icon = "📅",
                label = "Leave Notice",
                sub = "Submit leave notice & view statuses",
                onClick = { screen = "leave_history" }
            )
            RowItem(
                icon = "👤",
                label = "Profile & Contacts",
                sub = "Emergency & Guardian contact info",
                onClick = { screen = "profile" }
            )
            RowItem(
                icon = "⚙️",
                label = "Settings",
                sub = "App preferences & notifications",
                onClick = { screen = "settings" }
            )
        }

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.dangerLt)
                .clickable { showLogoutDialog = true }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sign Out",
                color = C.danger,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                text = { Text("Are you sure you want to sign out?", color = C.text, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) },
                confirmButton = {
                    Button(
                        onClick = { onLogout(); showLogoutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = C.danger)
                    ) {
                        Text("Yes", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = C.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Edit Emergency & Guardian Contacts Modal
        Modal(
            open = showEditContactsModal,
            onClose = { showEditContactsModal = false },
            title = "Edit Contact Information"
        ) {
            Text("EMERGENCY CONTACT", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Input(label = "Emergency Contact Name", value = editEmName, onChange = { editEmName = it })
            Input(label = "Relationship", value = editEmRel, onChange = { editEmRel = it })
            Input(label = "Contact Number", value = editEmPhone, onChange = { editEmPhone = it }, keyboardType = KeyboardType.Phone)
            Input(label = "Secondary Number (optional)", value = editEmSecPhone, onChange = { editEmSecPhone = it }, keyboardType = KeyboardType.Phone)
            Input(label = "Address (optional)", value = editEmAddress, onChange = { editEmAddress = it })

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = C.border)
            Spacer(Modifier.height(8.dp))

            Text("GUARDIAN CONTACT", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Input(label = "Guardian Full Name", value = editGName, onChange = { editGName = it })
            Input(label = "Relationship to Boarder", value = editGRel, onChange = { editGRel = it })
            Input(label = "Guardian Contact Number", value = editGPhone, onChange = { editGPhone = it }, keyboardType = KeyboardType.Phone)
            Input(label = "Alternative Number (optional)", value = editGAltPhone, onChange = { editGAltPhone = it }, keyboardType = KeyboardType.Phone)
            Input(label = "Guardian Address (optional)", value = editGAddress, onChange = { editGAddress = it })

            contactsError?.let { err ->
                Text(err, color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            ActionPair(
                onCancel = { showEditContactsModal = false },
                onConfirm = {
                    if (editEmName.isBlank() || editEmPhone.isBlank()) {
                        contactsError = "Please enter Emergency Contact Name and Number"
                        return@ActionPair
                    }
                    if (editGName.isBlank() || editGPhone.isBlank()) {
                        contactsError = "Please enter Guardian Name and Number"
                        return@ActionPair
                    }
                    contactsError = null
                    currentBoarder = currentBoarder.copy(
                        emergencyContact = EmergencyContact(
                            name = editEmName.trim(),
                            relationship = editEmRel.trim(),
                            phone = editEmPhone.trim(),
                            secondaryPhone = editEmSecPhone.trim().ifEmpty { null },
                            address = editEmAddress.trim().ifEmpty { null }
                        ),
                        guardianContact = GuardianContact(
                            name = editGName.trim(),
                            relationship = editGRel.trim(),
                            phone = editGPhone.trim(),
                            altPhone = editGAltPhone.trim().ifEmpty { null },
                            address = editGAddress.trim().ifEmpty { null }
                        )
                    )
                    showEditContactsModal = false
                    showToast("Emergency & Guardian contact updated")
                },
                confirmLabel = "Save Contacts"
            )
        }

        // Leave Notice Modal Form (Requirement 1.C)
        Modal(
            open = showLeaveModal,
            onClose = { showLeaveModal = false },
            title = if (leaveStep == "form") "Leave Notice Form" else "Review Leave Notice"
        ) {
            if (leaveStep == "form") {
                Input(label = "Leave Date", value = lLeaveDate, onChange = { lLeaveDate = it })
                Input(label = "Expected Departure Time", value = lDepTime, onChange = { lDepTime = it })
                Input(label = "Expected Return Date", value = lRetDate, onChange = { lRetDate = it })
                Input(label = "Expected Return Time", value = lRetTime, onChange = { lRetTime = it })
                Input(label = "Reason for Leaving", value = lReason, onChange = { lReason = it }, placeholder = "e.g. Family gathering, Medical visit")
                Input(label = "Destination", value = lDest, onChange = { lDest = it }, placeholder = "e.g. Davao City")
                Textarea(value = lNotes, onChange = { lNotes = it }, label = "Additional Notes (optional)", rows = 2)

                leaveFormError?.let { err ->
                    Text(err, color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                ActionPair(
                    onCancel = { showLeaveModal = false },
                    onConfirm = {
                        if (lReason.isBlank() || lDest.isBlank()) {
                            leaveFormError = "Please enter reason for leaving and destination"
                            return@ActionPair
                        }
                        leaveFormError = null
                        leaveStep = "review"
                    },
                    confirmLabel = "Review Notice"
                )
            } else {
                // Review step
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.bg)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PLEASE REVIEW YOUR LEAVE NOTICE", color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    leaveReviewRow("Leave Date", "$lLeaveDate at $lDepTime")
                    leaveReviewRow("Expected Return", "$lRetDate at $lRetTime")
                    leaveReviewRow("Reason", lReason)
                    leaveReviewRow("Destination", lDest)
                    if (lNotes.isNotBlank()) {
                        leaveReviewRow("Notes", lNotes)
                    }
                }

                ActionPair(
                    onCancel = { leaveStep = "form" },
                    onConfirm = {
                        val newNotice = LeaveNotice(
                            id = "ln" + System.currentTimeMillis(),
                            boarderId = currentBoarder.id,
                            leaveDate = lLeaveDate,
                            expectedDepartureTime = lDepTime,
                            expectedReturnDate = lRetDate,
                            expectedReturnTime = lRetTime,
                            reason = lReason,
                            destination = lDest,
                            additionalNotes = lNotes.ifBlank { null },
                            status = LeaveNoticeStatus.PENDING,
                            submittedAt = "Just now"
                        )
                        leaveNoticeList = listOf(newNotice) + leaveNoticeList
                        showLeaveModal = false
                        showToast("Leave notice successfully submitted")
                    },
                    confirmLabel = "Submit Leave Notice"
                )
            }
        }
    }
}

@Composable
private fun leaveReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = C.muted, fontSize = 13.sp)
        Text(value, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LeaveNoticeCard(notice: LeaveNotice) {
    val (statusLabel, statusBg, statusTc) = when (notice.status) {
        LeaveNoticeStatus.PENDING -> Triple("Pending", C.warnLt, C.warn)
        LeaveNoticeStatus.APPROVED -> Triple("Approved", C.sageLt, C.greenText)
        LeaveNoticeStatus.REJECTED -> Triple("Rejected", C.dangerLt, C.danger)
        LeaveNoticeStatus.COMPLETED -> Triple("Completed", C.blueLt, C.blueText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, C.border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                notice.reason,
                color = C.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    statusLabel,
                    color = statusTc,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Destination: ${notice.destination}",
            color = C.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Departure", color = C.muted, fontSize = 11.sp)
                Text("${notice.leaveDate} · ${notice.expectedDepartureTime}", color = C.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Expected Return", color = C.muted, fontSize = 11.sp)
                Text("${notice.expectedReturnDate} · ${notice.expectedReturnTime}", color = C.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        notice.additionalNotes?.let { notes ->
            Text("Notes: $notes", color = C.muted, fontSize = 12.sp)
        }

        Text(
            "Submitted: ${notice.submittedAt}",
            color = C.muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
