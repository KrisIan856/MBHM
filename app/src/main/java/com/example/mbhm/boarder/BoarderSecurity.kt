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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.MaintCategory
import com.example.mbhm.model.MaintStatus
import com.example.mbhm.model.MaintenanceReport
import com.example.mbhm.data.repository.AppRepository
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
import com.example.mbhm.ui.components.Select
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.dialPhone

@Composable
fun BSecurity(
    boarder: Boarder,
    startScreen: String = "overview",
    maintenance: List<MaintenanceReport>,
    setMaintenance: (List<MaintenanceReport>) -> Unit,
    onBoarderUpdate: (Boarder) -> Unit,
    onSendSos: () -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember(startScreen) { mutableStateOf(startScreen) }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var sosModal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appRepository = remember(context) { AppRepository.getInstance(context) }
    val guardianProfile by appRepository.observeGuardianProfile().collectAsState(initial = null)

    fun nav(s: String, d: Any? = null) {
        screen = s
        screenData = d
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
    if (screen == "emergency-contact") {
        B_EmergencyContact(boarder = boarder, onBoarderUpdate = onBoarderUpdate, showToast = showToast, onBack = { nav("overview") })
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(C.bg)) {
            Header(title = "Security & Emergency")
            ScrollBody(modifier = Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.dangerLt)
                        .border(2.dp, C.danger.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RowItem(
                        icon = "📞",
                        label = "Emergency Contact",
                        sub = "${boarder.emergencyContact.name} · ${boarder.emergencyContact.phone}",
                        onClick = { nav("emergency-contact") }
                    )
                    RowItem(
                        icon = "🏠",
                        label = "House Guardian",
                        sub = guardianProfile?.phone?.ifBlank { null }
                            ?: boarder.guardianContact.phone,
                        onClick = {
                            val phone = guardianProfile?.phone?.ifBlank { null }
                                ?: boarder.guardianContact.phone
                            if (phone.isNotBlank()) dialPhone(context, phone)
                            else showToast("No guardian contact number saved yet")
                        }
                    )
                    RowItem(
                        icon = "🔧",
                        label = "Maintenance",
                        sub = "${maintenance.count { m -> m.boarderId == boarder.id }} request${if (maintenance.count { m -> m.boarderId == boarder.id } == 1) "" else "s"}",
                        onClick = { nav("maintenance") }
                    )
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
                            append("the guardian")
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
                    onSendSos()
                    showToast("🆘 SOS alert sent! Guardian has been notified.")
                },
                confirmLabel = "Send SOS Alert",
                confirmDanger = true
            )
        }
    }
}

@Composable
private fun B_EmergencyContact(
    boarder: Boarder,
    onBoarderUpdate: (Boarder) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var formName by remember { mutableStateOf(boarder.emergencyContact.name) }
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
                .border(1.dp, C.danger.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
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
                    label = "Phone Number",
                    value = formPhone,
                    onChange = { formPhone = it }
                )
                PrimaryButton(
                    text = "Save Contact",
                    onClick = {
                        if (formName.isBlank() || formPhone.isBlank()) {
                            showToast("Please enter contact name and phone number")
                            return@PrimaryButton
                        }
                        onBoarderUpdate(
                            boarder.copy(
                                emergencyContact = boarder.emergencyContact.copy(
                                    name = formName.trim(),
                                    phone = formPhone.trim()
                                )
                            )
                        )
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
                submittedAt = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
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
                    .border(1.dp, C.warn.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
