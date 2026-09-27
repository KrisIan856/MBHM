package com.example.mbhm.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.BillingSettings
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.PayMethod
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.PenaltyType
import com.example.mbhm.model.ReceiptStatus
import com.example.mbhm.model.RecordedBy
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.Option
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PayChip
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.ReceiptChip
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Select
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.money

private val availableMonths = listOf(
    "May 2026", "Jun 2026", "Jul 2026", "Aug 2026", "Sep 2026", "Oct 2026", "Nov 2026", "Dec 2026"
)

private val payDueDayOptions = (1..28).map { n ->
    val suffix = when (n) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    Option(n.toString(), "$n$suffix of month")
}

private val payGraceOptions = listOf(0, 1, 2, 3, 5, 7).map { d ->
    Option(d.toString(), "$d day" + if (d == 1) "" else "s")
}

@Composable
fun GPayments(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    billing: BillingSettings,
    setBoarders: (List<Boarder>) -> Unit,
    setPayments: (List<PaymentRecord>) -> Unit,
    setBilling: (BillingSettings) -> Unit,
    showToast: (String) -> Unit
) {
    var screen by remember { mutableStateOf("list") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var filter by remember { mutableStateOf("all") }
    var currentMonthIndex by remember { mutableStateOf(4) } // "Sep 2026"
    val month = availableMonths[currentMonthIndex]

    fun nav(s: String, d: Any? = null) {
        screen = s
        screenData = d
    }

    fun back() = nav("list")

    if (screen == "detail") {
        GPay_Detail(
            boarder = screenData as Boarder,
            payments = payments,
            setPayments = setPayments,
            showToast = showToast,
            onBack = { back() },
            onCash = { nav("cash") }
        )
        return
    }
    if (screen == "cash") {
        GPay_Cash(boarders, payments, setPayments, showToast, onBack = { back() })
        return
    }
    if (screen == "settings") {
        GPay_Settings(boarders, setBoarders, billing, setBilling, showToast, onBack = { back() })
        return
    }
    if (screen == "export") {
        GPay_Export(showToast, onBack = { back() })
        return
    }

    val filtered = boarders.filter { filter == "all" || it.paymentStatus.name.lowercase() == filter }

    Screen(
        title = "Payment Management",
        right = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "💵" to "cash",
                    "📊" to "export",
                    "⚙️" to "settings"
                ).forEach { (icon, target) ->
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(C.bg).clickable { nav(target) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 16.sp)
                    }
                }
            }
        }
    ) {
        // Payment Period Navigation (Req 3.A)
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.card)
                .border(1.dp, C.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(if (currentMonthIndex > 0) C.bg else C.card)
                    .clickable(enabled = currentMonthIndex > 0) {
                        if (currentMonthIndex > 0) currentMonthIndex--
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = if (currentMonthIndex > 0) C.text else C.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PAYMENT PERIOD", color = C.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(month, color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(if (currentMonthIndex < availableMonths.lastIndex) C.bg else C.card)
                    .clickable(enabled = currentMonthIndex < availableMonths.lastIndex) {
                        if (currentMonthIndex < availableMonths.lastIndex) currentMonthIndex++
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = if (currentMonthIndex < availableMonths.lastIndex) C.text else C.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(C.primary)
                .clickable { nav("cash") }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💵", fontSize = 24.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    "Record In-Person Payment",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Manually record cash or direct payment for $month",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
            Text("›", color = Color.White, fontSize = 18.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            paySummaryCell(
                Modifier.weight(1f),
                boarders.count { it.paymentStatus == PayStatus.PAID },
                "Paid", C.sageLt, C.greenText
            )
            paySummaryCell(
                Modifier.weight(1f),
                boarders.count { it.paymentStatus == PayStatus.PARTIAL },
                "Partial", C.blueLt, C.blueText
            )
            paySummaryCell(
                Modifier.weight(1f),
                boarders.count { it.paymentStatus == PayStatus.PENDING },
                "Pending", C.warnLt, C.warn
            )
            paySummaryCell(
                Modifier.weight(1f),
                boarders.count { it.paymentStatus == PayStatus.OVERDUE },
                "Overdue", C.dangerLt, C.danger
            )
        }

        ChipBar(
            options = listOf(
                "All" to "all",
                "✓ Paid" to "paid",
                "½ Partial" to "partial",
                "⏳ Pending" to "pending",
                "⚠ Overdue" to "overdue"
            ),
            value = filter,
            onChange = { filter = it }
        )

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filtered.forEach { b ->
                val rec = payments.find { p -> p.boarderId == b.id && p.period == month }
                payBoarderRow(b, rec, onClick = { nav("detail", b) })
            }
            if (filtered.isEmpty()) {
                EmptyState(icon = "🔍", title = "No boarder records found for $month", sub = "Try selecting a different filter or month")
            }
        }
    }
}

@Composable
private fun paySummaryCell(modifier: Modifier, count: Int, label: String, bg: Color, tc: Color) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(count.toString(), color = tc, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = tc, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun payBoarderRow(b: Boarder, rec: PaymentRecord?, onClick: () -> Unit) {
    val borderColor = when {
        rec?.receiptStatus == ReceiptStatus.PENDING_REVIEW -> Color(0xFFF0DCA8)
        b.paymentStatus == PayStatus.PARTIAL -> Color(0xFFBFCFE8)
        else -> C.border
    }
    val hasPending = rec?.receiptStatus == ReceiptStatus.PENDING_REVIEW
    val paidDate = rec?.paidDate
    val amountPaid = rec?.amountPaid

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(initials = b.initials, color = b.avatarColor)
            Column(Modifier.weight(1f)) {
                Text(b.name, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Room ${b.room} · ${money(b.monthlyRate)}/mo",
                    color = C.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PayChip(status = b.paymentStatus)
                if (hasPending) {
                    Text("🧾 Receipt pending", color = C.warn, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (paidDate != null) {
                    Text(paidDate, color = C.muted, fontSize = 10.sp)
                }
            }
        }
        if (b.paymentStatus == PayStatus.PARTIAL && amountPaid != null) {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${money(amountPaid)} paid", color = C.blueText, fontSize = 11.sp)
                    Text(
                        "${money(b.monthlyRate - amountPaid)} balance",
                        color = C.muted,
                        fontSize = 11.sp
                    )
                }
                Box(
                    Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp)
                        .clip(RoundedCornerShape(50)).background(C.bg)
                ) {
                    Box(
                        Modifier.fillMaxWidth(
                            (amountPaid.toFloat() / b.monthlyRate).coerceIn(0f, 1f)
                        ).fillMaxHeight().background(C.blueBar, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun GPay_Detail(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit,
    onCash: () -> Unit
) {
    val bPay = payments.filter { p -> p.boarderId == boarder.id }
    val sepRec = bPay.find { p -> p.period == "Sep 2026" }
    var editRate by remember { mutableStateOf(false) }
    var newRate by remember { mutableStateOf(boarder.monthlyRate.toString()) }
    var rejReason by remember { mutableStateOf("") }
    var rejModal by remember { mutableStateOf(false) }

    fun verify(id: String, status: ReceiptStatus, reason: String? = null) {
        setPayments(
            payments.map { p ->
                if (p.id == id) {
                    p.copy(
                        receiptStatus = status,
                        rejectionReason = reason,
                        verifiedAt = "Sep 7 · now",
                        verifiedBy = "Ate Sandra"
                    )
                } else p
            }
        )
        showToast(if (status == ReceiptStatus.VERIFIED) "✓ Receipt verified" else "Receipt rejected")
        rejModal = false
    }

    Box(Modifier.fillMaxSize()) {
        Screen(
            title = boarder.name,
            onBack = onBack,
            right = {
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(C.primaryLt)
                        .clickable { onCash() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Record Payment", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.card)
                    .border(1.dp, C.border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(initials = boarder.initials, color = boarder.avatarColor, size = 52)
                Column(Modifier.weight(1f)) {
                    Text(boarder.name, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Room ${boarder.room} · ${boarder.job}",
                        color = C.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(boarder.phone, color = C.muted, fontSize = 12.sp)
                }
                PayChip(status = boarder.paymentStatus)
            }

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Billing — Sep 2026", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (editRate) "Cancel" else "✏ Edit",
                            color = C.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { editRate = !editRate }
                        )
                    }
                    if (editRate) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Input(
                                label = "Monthly Rate (₱)",
                                value = newRate,
                                onChange = { newRate = it },
                                keyboardType = KeyboardType.Number
                            )
                            PrimaryButton(
                                text = "Save Changes",
                                onClick = {
                                    editRate = false
                                    showToast("Billing rate updated")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                height = 45,
                                radius = 10,
                                fontSize = 14
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            payBillRow("Monthly Rate", money(boarder.monthlyRate))
                            payBillRow("Due Date", "Every ${boarder.dueDay}th of month")
                            payBillRow("Grace Period", "${boarder.gracePeriodDays} days")
                            payBillRow(
                                "Late Penalty",
                                if (boarder.latePenaltyType == PenaltyType.FLAT) "₱${boarder.latePenaltyAmount} flat fee"
                                else "${boarder.latePenaltyAmount}% of rent"
                            )
                        }
                    }
                }
            }

            val recFileName = sepRec?.receiptFileName
            if (sepRec != null && recFileName != null) {
                val isPending = sepRec.receiptStatus == ReceiptStatus.PENDING_REVIEW
                PCard(borderColor = if (isPending) Color(0xFFF0DCA8) else C.border) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Proof of Payment — Sep 2026", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            sepRec.receiptStatus?.let { ReceiptChip(status = it) }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.bg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧾", fontSize = 20.sp)
                            Text(recFileName, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("View", color = C.muted, fontSize = 12.sp)
                        }
                        if (isPending) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    Modifier.weight(1f).height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(C.dangerLt)
                                        .clickable { rejModal = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕ Reject", color = C.danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    Modifier.weight(1f).height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(C.sage)
                                        .clickable { verify(sepRec.id, ReceiptStatus.VERIFIED) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓ Verify", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        if (sepRec.receiptStatus == ReceiptStatus.REJECTED && sepRec.rejectionReason != null) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.dangerLt)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text("Rejection reason:", color = C.danger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    sepRec.rejectionReason,
                                    color = C.danger,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(title = "Payment History & Audit Trail")
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bPay.forEach { p -> payHistoryCard(p) }
                    if (bPay.isEmpty()) {
                        EmptyState(icon = "📋", title = "No payment records found")
                    }
                }
            }
        }

        Modal(open = rejModal, onClose = { rejModal = false }, title = "Reject Receipt") {
            Textarea(
                value = rejReason,
                onChange = { rejReason = it },
                label = "Reason for rejection",
                placeholder = "e.g. Amount mismatch, blurry image...",
                rows = 3
            )
            ActionPair(
                onCancel = { rejModal = false },
                onConfirm = {
                    val rec = sepRec
                    if (rec != null) verify(rec.id, ReceiptStatus.REJECTED, rejReason)
                },
                confirmLabel = "Reject Receipt",
                confirmDanger = true
            )
        }
    }
}

@Composable
private fun payBillRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = C.muted, fontSize = 14.sp)
        Text(value, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun payInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = C.muted, fontSize = 12.sp)
        Text(value, color = C.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun payHistoryCard(p: PaymentRecord) {
    PCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(p.period, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                PayChip(status = p.status)
            }
            p.paidDate?.let { payInfoRow("Paid on", it) }
            payInfoRow("Method", p.method.label)
            payInfoRow("Recorded by", if (p.recordedBy == RecordedBy.GUARDIAN) "Guardian" else "Boarder")
            p.verifiedAt?.let { payInfoRow("Verified", it) }
            p.notes?.let {
                Text(
                    it,
                    color = C.muted,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                HorizontalDivider(thickness = 1.dp, color = C.border, modifier = Modifier.padding(bottom = 8.dp))
                if (p.status == PayStatus.PARTIAL && p.amountPaid != null) {
                    payInfoRow("Amount Due", money(p.amount))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Paid So Far", color = C.blueText, fontSize = 12.sp)
                        Text(
                            money(p.amountPaid),
                            color = C.blueText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Balance Remaining", color = C.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            money(p.amount - p.amountPaid),
                            color = C.warn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp)
                            .clip(RoundedCornerShape(50)).background(C.bg)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(
                                (p.amountPaid.toFloat() / p.amount).coerceIn(0f, 1f)
                            ).fillMaxHeight().background(C.blueBar, RoundedCornerShape(50))
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount", color = C.muted, fontSize = 12.sp)
                        Text(money(p.amount), color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Record In-Person Payment Dashboard (Req 4.A, 4.B, 4.C)
@Composable
private fun GPay_Cash(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var boarderId by remember { mutableStateOf(boarders[0].id) }
    var period by remember { mutableStateOf("Sep 2026") }
    var payDate by remember { mutableStateOf("Sep 07, 2026") }
    var amountDueInput by remember { mutableStateOf(boarders[0].monthlyRate.toString()) }
    var amountPaidInput by remember { mutableStateOf(boarders[0].monthlyRate.toString()) }
    var method by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val selectedBoarder = boarders.firstOrNull { it.id == boarderId } ?: boarders[0]
    val amountDueVal = amountDueInput.toIntOrNull() ?: 0
    val amountPaidVal = amountPaidInput.toIntOrNull() ?: 0
    val remainingBalance = (amountDueVal - amountPaidVal).coerceAtLeast(0)

    val canSubmit = amountDueVal > 0 && amountPaidVal > 0 && amountPaidVal <= amountDueVal

    fun executeSave() {
        val isPartial = remainingBalance > 0
        val finalNotes = notes.ifBlank {
            if (isPartial) "In-person payment — balance ${money(remainingBalance)} remaining" else "Paid in person"
        }

        val newRec = PaymentRecord(
            id = "p" + System.currentTimeMillis(),
            boarderId = boarderId,
            period = period,
            amount = amountDueVal,
            amountPaid = if (isPartial) amountPaidVal else null,
            paidDate = payDate,
            method = when (method) {
                "cash" -> PayMethod.CASH
                "gcash" -> PayMethod.GCASH
                "bank_transfer" -> PayMethod.BANK_TRANSFER
                else -> PayMethod.ONLINE
            },
            status = if (isPartial) PayStatus.PARTIAL else PayStatus.PAID,
            recordedBy = RecordedBy.GUARDIAN,
            verifiedBy = "Ate Sandra",
            verifiedAt = "$payDate · recorded",
            notes = finalNotes
        )

        setPayments(payments + newRec)
        showToast("Payment of ${money(amountPaidVal)} successfully recorded")
        onBack()
    }

    Screen(title = "Record In-Person Payment", onBack = onBack) {
        Select(
            label = "Select Boarder",
            value = boarderId,
            onChange = { id ->
                boarderId = id
                val b = boarders.firstOrNull { it.id == id }
                if (b != null) {
                    amountDueInput = b.monthlyRate.toString()
                    amountPaidInput = b.monthlyRate.toString()
                }
            },
            options = boarders.map { Option(it.id, "${it.name} — Room ${it.room}") }
        )

        // Period & Calendar Selector (Req 4.A)
        Select(
            label = "Billing Period",
            value = period,
            onChange = { period = it },
            options = availableMonths.map { Option(it, it) }
        )

        Input(
            label = "Payment Date",
            value = payDate,
            onChange = { payDate = it },
            placeholder = "e.g. Sep 07, 2026"
        )

        // Amount Due (Editable - Req 4.B)
        Input(
            label = "Amount Due (₱)",
            value = amountDueInput,
            onChange = { amountDueInput = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 5000"
        )

        Input(
            label = "Amount Paid Now (₱)",
            value = amountPaidInput,
            onChange = { amountPaidInput = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 5000"
        )

        // Balance Summary Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (remainingBalance == 0) C.sageLt else C.blueLt)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Due:", color = C.muted, fontSize = 13.sp)
                Text(money(amountDueVal), color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Paid:", color = C.greenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(money(amountPaidVal), color = C.greenText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remaining Balance:", color = if (remainingBalance > 0) C.warn else C.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(money(remainingBalance), color = if (remainingBalance > 0) C.warn else C.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Select(
            label = "Payment Method",
            value = method,
            onChange = { method = it },
            options = listOf(
                Option("cash", "💵 Cash (In Person)"),
                Option("gcash", "GCash Direct"),
                Option("bank_transfer", "Bank Transfer Direct"),
                Option("online", "Online Transfer")
            )
        )

        Input(
            label = "Notes or Remarks (optional)",
            value = notes,
            onChange = { notes = it },
            placeholder = "e.g. Received cash in person at front desk"
        )

        PrimaryButton(
            text = "Record Payment",
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        )

        // Confirmation Dialog before permanently saving payment record (Req 4.C)
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Payment Record", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to record a payment of ${money(amountPaidVal)} for ${selectedBoarder.name} for $period?",
                        color = C.text,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            executeSave()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = C.primary)
                    ) {
                        Text("Save Payment", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = C.muted)
                    }
                }
            )
        }
    }
}

@Composable
private fun GPay_Settings(
    boarders: List<Boarder>,
    setBoarders: (List<Boarder>) -> Unit,
    billing: BillingSettings,
    setBilling: (BillingSettings) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var s by remember { mutableStateOf(billing) }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Billing Terms & Settings", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 20) {
            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Default Terms", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Select(
                        label = "Due Day (each month)",
                        value = s.dueDay.toString(),
                        onChange = { v -> s = s.copy(dueDay = v.toIntOrNull() ?: s.dueDay) },
                        options = payDueDayOptions
                    )
                    Select(
                        label = "Grace Period",
                        value = s.gracePeriodDays.toString(),
                        onChange = { v -> s = s.copy(gracePeriodDays = v.toIntOrNull() ?: s.gracePeriodDays) },
                        options = payGraceOptions
                    )
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Late Payment Penalty", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Select(
                        label = "Penalty Type",
                        value = if (s.penaltyType == PenaltyType.FLAT) "flat" else "percentage",
                        onChange = { v ->
                            s = s.copy(penaltyType = if (v == "flat") PenaltyType.FLAT else PenaltyType.PERCENTAGE)
                        },
                        options = listOf(
                            Option("flat", "Flat Fee (₱)"),
                            Option("percentage", "Percentage of Rent (%)")
                        )
                    )
                    Input(
                        label = if (s.penaltyType == PenaltyType.FLAT) "Penalty Amount (₱)" else "Penalty Percentage (%)",
                        value = s.penaltyAmount.toString(),
                        onChange = { v -> s = s.copy(penaltyAmount = v.toIntOrNull() ?: s.penaltyAmount) },
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            PrimaryButton(
                text = "Save Settings",
                onClick = {
                    setBilling(s)
                    showToast("Billing settings saved")
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GPay_Export(showToast: (String) -> Unit, onBack: () -> Unit) {
    var from by remember { mutableStateOf("2026-07-01") }
    var to by remember { mutableStateOf("2026-09-07") }

    Screen(title = "Export Payment Records", onBack = onBack) {
        Input(label = "From", value = from, onChange = { from = it })
        Input(label = "To", value = to, onChange = { to = it })

        PrimaryButton(
            text = "Export Report",
            onClick = {
                showToast("Payment records report exported")
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
