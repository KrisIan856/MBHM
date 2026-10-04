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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.ReceiptChip
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.fileNameOf
import com.example.mbhm.ui.components.money

@Composable
fun BPayments(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit
) {
    var selectedMonth by remember { mutableStateOf("Sep 2026") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showPreviewModal by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val myPay = payments.filter { p -> p.boarderId == boarder.id }
    val currentRec = myPay.find { p -> p.period == selectedMonth }
    val history = myPay.filter { p -> p.period != selectedMonth }

    val amountDue = currentRec?.amount ?: boarder.monthlyRate
    val amountPaid = currentRec?.amountPaid ?: if (currentRec?.status == PayStatus.PAID) amountDue else 0
    val remainingBalance = (amountDue - amountPaid).coerceAtLeast(0)

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
                        rejectionReason = null,
                        paidDate = "Sep 7"
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
                    amountPaid = if (boarder.paymentStatus == PayStatus.PARTIAL) 2500 else null,
                    paidDate = "Sep 7",
                    method = PayMethod.GCASH,
                    status = if (boarder.paymentStatus == PayStatus.PARTIAL) PayStatus.PARTIAL else PayStatus.PENDING,
                    receiptStatus = ReceiptStatus.PENDING_REVIEW,
                    receiptFileName = fileName,
                    recordedBy = RecordedBy.BOARDER
                )
            )
        }
        showToast("Payment proof uploaded — waiting for guardian verification")
    }

    Screen(title = "My Payments") {
        // Payment Status Banner (Req 1.A)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        currentRec?.receiptStatus == ReceiptStatus.PENDING_REVIEW -> C.primary
                        boarder.paymentStatus == PayStatus.PAID -> C.sage
                        boarder.paymentStatus == PayStatus.PARTIAL -> Color(0xFF3B6ACB)
                        boarder.paymentStatus == PayStatus.OVERDUE -> C.danger
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
                    boarder.paymentStatus == PayStatus.PAID -> "✓ FULLY PAID" to Color.White.copy(alpha = 0.25f)
                    boarder.paymentStatus == PayStatus.PARTIAL -> "½ PARTIALLY PAID" to Color.White.copy(alpha = 0.25f)
                    boarder.paymentStatus == PayStatus.OVERDUE -> "⚠ OVERDUE" to Color.White.copy(alpha = 0.25f)
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

        // Bill Breakdown Card
        PCard {
            Text(
                "Bill Details for $selectedMonth",
                color = C.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                "Monthly Rent" to (boarder.monthlyRate - 500),
                "Water Charge" to 300,
                "Electricity Charge" to 200
            ).forEach { (label, amount) ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = C.muted, fontSize = 13.sp)
                    Text(
                        money(amount),
                        color = C.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = C.border,
                modifier = Modifier.padding(top = 10.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Amount Due", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(money(amountDue), color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

        // Payment History Section
        Column(Modifier.fillMaxWidth()) {
            SectionHeader(title = "Payment History")
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                history.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(C.card)
                            .border(1.dp, C.border, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(C.sageLt),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅", fontSize = 18.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.period,
                                color = C.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Paid ${p.paidDate ?: ""} · ${p.method.label}",
                                color = C.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            p.receiptStatus?.let {
                                ReceiptChip(status = it, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Text(
                            money(p.amount),
                            color = C.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (history.isEmpty()) {
                    EmptyState(icon = "📋", title = "No payment history yet")
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
    }
}
