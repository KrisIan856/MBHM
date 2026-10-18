package com.example.mbhm.boarder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.ImageView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.PayMethod
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.ReceiptStatus
import com.example.mbhm.model.RecordedBy
import com.example.mbhm.model.currentPeriod
import com.example.mbhm.model.effectivePaidMillis
import com.example.mbhm.model.monthAccount
import com.example.mbhm.model.paidAmountOf
import com.example.mbhm.model.paymentPeriods
import com.example.mbhm.model.periodSortKey
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.DateRangeFilter
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PayChip
import com.example.mbhm.ui.components.PayPeriodPager
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.ReceiptChip
import com.example.mbhm.ui.components.rememberPrefBoolean
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.StatCard
import com.example.mbhm.ui.components.TableCard
import com.example.mbhm.ui.components.TableColumn
import com.example.mbhm.ui.components.TableRow
import com.example.mbhm.ui.components.fileNameOf
import com.example.mbhm.ui.components.money
import com.example.mbhm.ui.components.parseIsoDate

/** Month table columns, mirroring the guardian's month-by-month table. */
private val bPayMonthTableColumns = listOf(
    TableColumn("Period", 1.15f),
    TableColumn("Billed", 0.85f, end = true),
    TableColumn("Paid", 0.85f, end = true),
    TableColumn("Balance", 0.9f, end = true)
)

/** Label left, amount right, with the amount never allowed to wrap or clip. */
@Composable
private fun bPayRow(
    label: String,
    value: String,
    color: Color = C.text,
    bold: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = C.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = color,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun BPayments(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(currentPeriod()) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showPreviewModal by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val myPay = payments.filter { p -> p.boarderId == boarder.id }
    val periods = remember(myPay) { paymentPeriods(myPay) }
    LaunchedEffect(periods) {
        if (periods.none { it == selectedMonth }) {
            selectedMonth = periods.lastOrNull() ?: currentPeriod()
        }
    }
    // Same derived account the guardian screen reads, so both roles always agree.
    val account = remember(boarder, myPay, selectedMonth) {
        monthAccount(boarder, myPay, selectedMonth)
    }
    val currentRec = account.record
    val history = myPay.filter { p -> p.period != selectedMonth }

    var screen by remember { mutableStateOf("main") }
    var newPayOpen by remember { mutableStateOf(false) }
    var newAmount by remember { mutableStateOf(boarder.monthlyRate.toString()) }
    var newMethod by remember { mutableStateOf("gcash") }

    if (screen == "history") {
        BPay_History(
            boarder = boarder,
            payments = myPay,
            onBack = { screen = "main" }
        )
        return
    }

    fun createPayment() {
        val amount = newAmount.trim().toIntOrNull()
        if (amount == null || amount <= 0) {
            showToast("Enter a valid amount to pay")
            return
        }
        if (account.isFullyPaid) {
            showToast("$selectedMonth is already fully paid")
            newPayOpen = false
            return
        }

        val existing = account.record
        // Top-ups add onto the existing record rather than being refused, matching how
        // the guardian's in-person payment accumulates onto a partial month.
        val newPaidTotal = (account.paid + amount).coerceAtMost(account.due)
        val settled = newPaidTotal >= account.due
        val rec = PaymentRecord(
            id = existing?.id ?: ("p" + System.currentTimeMillis()),
            boarderId = boarder.id,
            period = selectedMonth,
            amount = account.due,
            amountPaid = if (settled) null else newPaidTotal,
            method = when (newMethod) {
                "gcash" -> PayMethod.GCASH
                "bank" -> PayMethod.BANK_TRANSFER
                else -> PayMethod.CASH
            },
            status = if (settled) PayStatus.PAID else PayStatus.PARTIAL,
            receiptStatus = existing?.receiptStatus,
            receiptFileName = existing?.receiptFileName,
            rejectionReason = existing?.rejectionReason,
            recordedBy = RecordedBy.BOARDER,
            notes = if (settled) "Paid in full" else "Partial payment — ${money(account.due - newPaidTotal)} remaining"
        )
        setPayments(
            if (existing == null) payments + rec else payments.map { if (it.id == existing.id) rec else it }
        )
        showToast("Payment entry created — upload your proof of payment")
        newPayOpen = false
    }

    val amountDue = account.due
    val amountPaid = account.paid
    val remainingBalance = account.balance

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = context.fileNameOf(uri)
            selectedFileName = fileName
            selectedFileUri = uri
            showPreviewModal = true
        }
    }

    fun submitProof(fileName: String) {
        val rec = currentRec
        if (rec != null) {
            setPayments(payments.map { p ->
                if (p.id == rec.id) {
                    p.copy(
                        receiptStatus = ReceiptStatus.PENDING_REVIEW,
                        receiptFileName = fileName,
                        rejectionReason = null
                    )
                } else p
            })
        } else {
            setPayments(
                payments + PaymentRecord(
                    id = "p" + System.currentTimeMillis(),
                    boarderId = boarder.id,
                    period = selectedMonth,
                    amount = amountDue,
                    amountPaid = null,
                    paidDate = null,
                    method = PayMethod.GCASH,
                    status = PayStatus.PENDING,
                    receiptStatus = ReceiptStatus.PENDING_REVIEW,
                    receiptFileName = fileName,
                    recordedBy = RecordedBy.BOARDER
                )
            )
        }
        showToast("Payment proof uploaded — waiting for guardian verification")
    }

    Screen(
        title = "My Payments",
        right = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (account.isFullyPaid) "＋ Record Top-up" else "＋ New Payment",
                    color = C.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { newPayOpen = true }
                )
                if (rememberPrefBoolean("notif_payment_history", true).value) {
                    Text(
                        "History",
                        color = C.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { screen = "history" }
                    )
                }
            }
        }
    ) {
        // Same pager as the guardian screen so both roles view identical months.
        PayPeriodPager(
            periods = periods,
            selected = selectedMonth,
            onSelect = { selectedMonth = it }
        )

        // Payment Status Banner (Req 1.A)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        currentRec?.receiptStatus == ReceiptStatus.PENDING_REVIEW -> C.primary
                        account.status == PayStatus.PAID -> C.sage
                        account.status == PayStatus.PARTIAL -> C.blueText
                        account.status == PayStatus.OVERDUE -> C.danger
                        else -> C.primary
                    }
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMonth,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                val (badgeText, badgeBg) = when {
                    currentRec?.receiptStatus == ReceiptStatus.PENDING_REVIEW -> "⏳ PENDING VERIFICATION" to Color.White.copy(alpha = 0.25f)
                    account.status == PayStatus.PAID -> "✓ FULLY PAID" to Color.White.copy(alpha = 0.25f)
                    account.status == PayStatus.PARTIAL -> "½ PARTIALLY PAID" to Color.White.copy(alpha = 0.25f)
                    account.status == PayStatus.OVERDUE -> "⚠ OVERDUE" to Color.White.copy(alpha = 0.25f)
                    else -> "⏳ UNPAID" to Color.White.copy(alpha = 0.25f)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        badgeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = money(amountDue),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Paid Date", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(currentRec?.paidDate ?: "Not paid yet", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Amount Paid", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(money(amountPaid), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining Balance", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(money(remainingBalance), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (rememberPrefBoolean("notif_payment_reminders", true).value &&
            !account.isFullyPaid
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.warnLt)
                    .border(1.dp, C.warn.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔", fontSize = 16.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "Payment reminder",
                        color = C.warn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Rent for $selectedMonth is due on day ${boarder.dueDay} · ${money(remainingBalance)} outstanding",
                        color = C.warn,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Account breakdown. Shows the same billed/paid/balance figures the guardian sees
        // rather than inventing utility line items that are not stored anywhere.
        PCard {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Account for $selectedMonth",
                    color = C.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                bPayRow("Monthly Rent", money(amountDue))
                if (amountPaid > 0) {
                    bPayRow("Amount Paid", money(amountPaid), color = C.greenText)
                }
                HorizontalDivider(thickness = 1.dp, color = C.border)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (account.isFullyPaid) "Fully Paid" else "Balance Due",
                        color = if (account.isFullyPaid) C.greenText else C.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        money(remainingBalance),
                        color = if (account.isFullyPaid) C.greenText else C.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Proof of Payment Section
        PCard {
            Text(
                "Proof of Payment",
                color = C.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            val rec = currentRec
            val recStatus = rec?.receiptStatus

            if (rec != null && recStatus != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.bg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧾", fontSize = 20.sp)
                            Text(
                                rec.receiptFileName ?: "Proof uploaded",
                                color = C.text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ReceiptChip(status = recStatus)
                    }

                    if (recStatus == ReceiptStatus.REJECTED) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.dangerLt)
                                .padding(12.dp)
                        ) {
                            Text(
                                "Proof Rejected by Guardian",
                                color = C.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!rec.rejectionReason.isNullOrBlank()) {
                                Text(
                                    "Reason: ${rec.rejectionReason}",
                                    color = C.danger,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(C.danger)
                                    .clickable {
                                        pickerLauncher.launch(arrayOf("image/jpeg", "image/png"))
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Upload New Proof",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (recStatus == ReceiptStatus.VERIFIED && rec.verifiedAt != null) {
                        Text(
                            "Verified ${rec.verifiedAt} by ${rec.verifiedBy ?: "Guardian"}",
                            color = C.muted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(
                        "Upload your GCash, bank transfer, or payment receipt image (JPG, JPEG, PNG).",
                        color = C.muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.primaryLt)
                            .border(1.dp, C.primary, RoundedCornerShape(12.dp))
                            .clickable {
                                pickerLauncher.launch(arrayOf("image/jpeg", "image/png"))
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📎", fontSize = 18.sp)
                            Text(
                                "Upload Proof of Payment",
                                color = C.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // All my months, same figures the guardian reads from their side.
        Column(Modifier.fillMaxWidth()) {
            SectionHeader(title = "All My Months")
            if (history.isEmpty()) {
                EmptyState(icon = "📋", title = "No other months yet")
            } else {
                TableCard(bPayMonthTableColumns) {
                    history
                        .sortedByDescending { periodSortKey(it.period) ?: Int.MIN_VALUE }
                        .forEachIndexed { index, p ->
                            val acc = monthAccount(boarder, myPay, p.period)
                            TableRow(
                                columns = bPayMonthTableColumns,
                                divider = index > 0,
                                onClick = { selectedMonth = p.period },
                                cells = listOf(
                                    {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                p.period,
                                                color = C.text,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            PayChip(status = acc.status)
                                        }
                                    },
                                    {
                                        Text(
                                            money(acc.due),
                                            color = C.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    },
                                    {
                                        Text(
                                            money(acc.paid),
                                            color = if (acc.paid > 0) C.greenText else C.muted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    },
                                    {
                                        Text(
                                            money(acc.balance),
                                            color = if (acc.balance > 0) C.danger else C.greenText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                )
                            )
                        }
                }
            }
        }

        // Image Preview Modal before submitting proof (Req 1.A)
        Modal(
            open = showPreviewModal,
            onClose = { showPreviewModal = false },
            title = "Preview Proof of Payment"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.bg)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.card)
                        .border(1.dp, C.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedFileUri != null) {
                        AndroidView(
                            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                            update = { imageView -> imageView.setImageURI(selectedFileUri) },
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    } else {
                        Text("No image selected", color = C.muted, fontSize = 13.sp)
                    }
                }
                Text(
                    selectedFileName ?: "Selected Image",
                    color = C.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Ready to upload for $selectedMonth",
                    color = C.muted,
                    fontSize = 11.sp
                )
            }

            ActionPair(
                onCancel = {
                    showPreviewModal = false
                    selectedFileName = null
                    selectedFileUri = null
                },
                onConfirm = {
                    val fn = selectedFileName ?: "payment_proof.jpg"
                    submitProof(fn)
                    showPreviewModal = false
                    selectedFileName = null
                    selectedFileUri = null
                },
                confirmLabel = "Submit Proof"
            )
        }

        Modal(open = newPayOpen, onClose = { newPayOpen = false }, title = "New Payment") {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        "Period",
                        color = C.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        selectedMonth,
                        color = C.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Input(
                    label = if (account.paid > 0)
                        "Top-up Amount (₱) · ${money(account.balance)} outstanding"
                    else "Amount (₱)",
                    value = newAmount,
                    onChange = { newAmount = it },
                    placeholder = "0"
                )
                if (account.paid > 0) {
                    Text(
                        "You have already paid ${money(account.paid)} of ${money(account.due)} for $selectedMonth. This adds on top of that.",
                        color = C.muted,
                        fontSize = 12.sp
                    )
                }
                Column {
                    Text(
                        "Payment Method",
                        color = C.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ChipBar(
                        options = listOf(
                            "GCash" to "gcash",
                            "Cash" to "cash",
                            "Bank" to "bank"
                        ),
                        value = newMethod,
                        onChange = { newMethod = it }
                    )
                }
                Text(
                    "The guardian will verify this payment once you upload your proof.",
                    color = C.muted,
                    fontSize = 12.sp
                )
                PrimaryButton(
                    text = "Create Payment",
                    onClick = { createPayment() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BPay_History(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    onBack: () -> Unit
) {
    var fromIso by remember { mutableStateOf("") }
    var toIso by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }

    // Filter on the real payment instant so a top-up always sorts by when it happened.
    val filtered = payments.filter { p ->
        val statusOk = statusFilter == "all" || p.status.name.lowercase() == statusFilter
        val from = if (fromIso.isEmpty()) Long.MIN_VALUE else (parseIsoDate(fromIso) ?: Long.MIN_VALUE)
        val endOfDay = if (toIso.isEmpty()) {
            Long.MAX_VALUE
        } else {
            (parseIsoDate(toIso) ?: Long.MAX_VALUE) + 24L * 60 * 60 * 1000 - 1
        }
        statusOk && p.effectivePaidMillis() in from..endOfDay
    }.sortedWith(
        compareByDescending<PaymentRecord> { it.effectivePaidMillis() }
            .thenByDescending { periodSortKey(it.period) ?: Int.MIN_VALUE }
    )
    val totalBilled = filtered.sumOf { it.amount }
    val totalPaid = filtered.sumOf { paidAmountOf(it) }
    val outstanding = (totalBilled - totalPaid).coerceAtLeast(0)

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Payment History", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Months",
                    value = filtered.size,
                    sub = "in range",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Paid",
                    value = money(totalPaid),
                    sub = "of ${money(totalBilled)}",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
            }
            StatCard(
                label = "Outstanding",
                value = money(outstanding),
                sub = "across listed months",
                bg = if (outstanding > 0) C.warnLt else C.sageLt,
                textColor = if (outstanding > 0) C.warn else C.greenText
            )

            SectionHeader(title = "Filter by date")
            DateRangeFilter(from = fromIso, to = toIso, onChange = { f, t -> fromIso = f; toIso = t })
            ChipBar(
                options = listOf(
                    "All" to "all",
                    "Paid" to "paid",
                    "Pending" to "pending",
                    "Partial" to "partial",
                    "Overdue" to "overdue"
                ),
                value = statusFilter,
                onChange = { statusFilter = it }
            )

            SectionHeader(
                title = "${filtered.size} month${if (filtered.size == 1) "" else "s"}",
                action = { Text("Newest first", color = C.muted, fontSize = 11.sp) }
            )

            if (filtered.isEmpty()) {
                EmptyState(icon = "💳", title = "No payments match", sub = "Try another date range or filter")
            }

            filtered.forEach { p ->
                    val paid = paidAmountOf(p)
                    PCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💳", fontSize = 18.sp)
                                Text(
                                    p.period,
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            PayChip(status = p.status)
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Billed", color = C.muted, fontSize = 11.sp)
                                Text(
                                    money(p.amount),
                                    color = C.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Paid", color = C.muted, fontSize = 11.sp)
                                Text(
                                    money(paid),
                                    color = if (paid > 0) C.greenText else C.muted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Balance", color = C.muted, fontSize = 11.sp)
                                Text(
                                    money((p.amount - paid).coerceAtLeast(0)),
                                    color = if (paid >= p.amount) C.greenText else C.danger,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${p.paidDate ?: "Uploaded · ${p.period}"} · ${p.method.label}",
                                color = C.muted,
                                fontSize = 12.sp
                            )
                            p.receiptStatus?.let { ReceiptChip(status = it) }
                        }
                        if (p.verifiedAt != null) {
                            Text(
                                "Verified ${p.verifiedAt} by ${p.verifiedBy ?: "Guardian"}",
                                color = C.greenText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        if (p.status == PayStatus.PENDING && p.receiptStatus == null) {
                            Text(
                                "No proof uploaded yet",
                                color = C.warn,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
        }
    }
}
