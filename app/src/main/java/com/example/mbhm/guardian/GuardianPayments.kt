package com.example.mbhm.guardian

import android.app.DatePickerDialog
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mbhm.model.BillingSettings
import com.example.mbhm.model.Boarder
import com.example.mbhm.model.MonthAccount
import com.example.mbhm.model.PayMethod
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.PaymentRecord
import com.example.mbhm.model.PenaltyType
import com.example.mbhm.model.ReceiptStatus
import com.example.mbhm.model.RecordedBy
import com.example.mbhm.model.boarderAccounts
import com.example.mbhm.model.currentPeriod
import com.example.mbhm.model.effectivePaidMillis
import com.example.mbhm.model.monthAccount
import com.example.mbhm.model.paymentPeriods
import com.example.mbhm.model.periodSortKey
import com.example.mbhm.ui.components.ActionPair
import com.example.mbhm.ui.components.Avatar
import com.example.mbhm.ui.components.C
import com.example.mbhm.ui.components.ChipBar
import com.example.mbhm.ui.components.DateRangeFilter
import com.example.mbhm.ui.components.EmptyState
import com.example.mbhm.ui.components.Header
import com.example.mbhm.ui.components.Input
import com.example.mbhm.ui.components.Modal
import com.example.mbhm.ui.components.Option
import com.example.mbhm.ui.components.PCard
import com.example.mbhm.ui.components.PayChip
import com.example.mbhm.ui.components.PayPeriodPager
import com.example.mbhm.ui.components.PrimaryButton
import com.example.mbhm.ui.components.ReceiptChip
import com.example.mbhm.ui.components.Screen
import com.example.mbhm.ui.components.ScrollBody
import com.example.mbhm.ui.components.SectionHeader
import com.example.mbhm.ui.components.Select
import com.example.mbhm.ui.components.StatCard
import com.example.mbhm.ui.components.TableCard
import com.example.mbhm.ui.components.TableColumn
import com.example.mbhm.ui.components.TableRow
import com.example.mbhm.ui.components.Textarea
import com.example.mbhm.ui.components.rememberPrefBoolean
import com.example.mbhm.ui.components.money
import com.example.mbhm.ui.components.parseIsoDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun ordinal(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

private val payDueDayOptions = (1..28).map { n ->
    Option(n.toString(), "${ordinal(n)} of month")
}

private val payGraceOptions = listOf(0, 1, 2, 3, 5, 7).map { d ->
    Option(d.toString(), "$d day" + if (d == 1) "" else "s")
}

/** Shared column layouts so every payment table reads the same way. */
private val payTableColumns = listOf(
    TableColumn("Boarder", 1.45f),
    TableColumn("Due", 0.7f, end = true),
    TableColumn("Paid", 0.7f, end = true),
    TableColumn("Status", 1.15f, end = true)
)

private val balanceTableColumns = listOf(
    TableColumn("Boarder", 1.35f),
    TableColumn("Due", 0.7f, end = true),
    TableColumn("Paid", 0.7f, end = true),
    TableColumn("Balance", 0.85f, end = true)
)

private val historyTableColumns = listOf(
    TableColumn("Boarder", 1.3f),
    TableColumn("Period", 0.95f, end = true),
    TableColumn("Amount", 0.8f, end = true),
    TableColumn("Status", 1.1f, end = true)
)

/**
 * Summary table columns. Deliberately three wide columns instead of five narrow ones:
 * the peso amounts need real width or they clip.
 */
private val paySummaryTableColumns = listOf(
    TableColumn("Item", 1.45f),
    TableColumn("Amount", 1.05f, end = true),
    TableColumn("Share", 0.75f, end = true)
)

/**
 * Month-by-month table. Four columns only, so each amount gets enough width to stay
 * readable; status is carried by a coloured dot beside the period plus the balance
 * colour rather than a fifth chip column.
 */
private val payMonthTableColumns = listOf(
    TableColumn("Period", 1.15f),
    TableColumn("Billed", 0.85f, end = true),
    TableColumn("Paid", 0.85f, end = true),
    TableColumn("Balance", 0.9f, end = true)
)

/** Shrinks the font when an amount is long so it never overflows its column. */
private fun moneyFontSize(value: Int): Int {
    val digits = String.format("%,d", value).length
    return when {
        digits <= 7 -> 15
        digits <= 9 -> 13
        else -> 11
    }
}

/**
 * Full-width money line: label on the left, the amount right-aligned on the right.
 * Full width matters here — the amounts are long strings and side-by-side tiles clip
 * them on narrow screens.
 */
@Composable
private fun PayAmountLine(
    label: String,
    value: String,
    valueColor: Color = C.text,
    bg: Color? = null,
    emphasis: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (bg != null) Modifier.clip(RoundedCornerShape(10.dp)).background(bg) else Modifier)
            .padding(horizontal = if (bg != null) 12.dp else 0.dp, vertical = if (bg != null) 10.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (emphasis) C.text else C.muted,
            fontSize = 13.sp,
            fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = valueColor,
            fontSize = if (emphasis) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Small status dot + label used in the Period column of the month table. */
@Composable
private fun PayStatusDot(status: PayStatus) {
    val color = when (status) {
        PayStatus.PAID -> C.greenText
        PayStatus.PARTIAL -> C.primary
        PayStatus.OVERDUE -> C.danger
        PayStatus.PENDING -> C.muted
    }
    val label = when (status) {
        PayStatus.PAID -> "Paid in full"
        PayStatus.PARTIAL -> "Partly paid"
        PayStatus.OVERDUE -> "Overdue"
        PayStatus.PENDING -> "Unpaid"
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** One row of the overall-account table: item, amount, and a proportional share bar. */
@Composable
private fun PaySummaryTableRow(
    label: String,
    value: Int,
    valueColor: Color,
    share: Float,
    barColor: Color,
    divider: Boolean,
    bold: Boolean = false
) {
    TableRow(
        columns = paySummaryTableColumns,
        divider = divider,
        cells = listOf(
            {
                Text(
                    label,
                    color = C.text,
                    fontSize = 13.sp,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2
                )
            },
            {
                Text(
                    money(value),
                    color = valueColor,
                    fontSize = moneyFontSize(value).sp,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
            },
            {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(C.bg)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(share.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(barColor, RoundedCornerShape(50))
                        )
                    }
                    Text(
                        "${(share * 100).toInt()}%",
                        color = C.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    )
}

/** One row of the month-by-month table. */
@Composable
private fun PayMonthTableRow(acc: MonthAccount, highlighted: Boolean, divider: Boolean) {
    TableRow(
        columns = payMonthTableColumns,
        divider = divider,
        modifier = if (highlighted) Modifier.background(C.primaryLt) else Modifier,
        cells = listOf(
            {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        acc.period,
                        color = if (highlighted) C.primary else C.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    PayStatusDot(status = acc.status)
                }
            },
            {
                Text(
                    money(acc.due),
                    color = C.text,
                    fontSize = moneyFontSize(acc.due).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            },
            {
                Text(
                    money(acc.paid),
                    color = if (acc.paid > 0) C.greenText else C.muted,
                    fontSize = moneyFontSize(acc.paid).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            },
            {
                Text(
                    money(acc.balance),
                    color = if (acc.balance > 0) C.danger else C.greenText,
                    fontSize = moneyFontSize(acc.balance).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        )
    )
}

/** Small local carrier so the banner can pick a consistent icon + colour set. */
private data class PayBannerStyle(
    val icon: String,
    val title: String,
    val bg: Color,
    val fg: Color
)

/**
 * Reads the selected boarder's live account for [period] and says plainly whether that
 * month is settled, partly settled or still open.
 */
@Composable
private fun PayPeriodStatusBanner(
    account: MonthAccount,
    boarderName: String
) {
    val style = when {
        account.isFullyPaid -> PayBannerStyle("\u2713", "Already fully paid for ${account.period}", C.sageLt, C.greenText)
        account.paid > 0 -> PayBannerStyle("\u00BD", "Partially paid for ${account.period}", C.primaryLt, C.primary)
        account.hasRecord -> PayBannerStyle("!", "Payment on hold for ${account.period}", C.dangerLt, C.danger)
        else -> PayBannerStyle("\u23F3", "No payment yet for ${account.period}", C.bg, C.muted)
    }
    val fg = style.fg
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(style.bg)
            .border(1.dp, fg.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(style.icon, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {
                Text(style.title, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$boarderName · due ${money(account.due)} · paid ${money(account.paid)}",
                    color = fg.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            PayChip(status = account.status)
        }
        if (account.isFullyPaid) {
            Text(
                "This account is settled for ${account.period}. There is nothing left to collect.",
                color = fg,
                fontSize = 11.sp
            )
        } else if (account.paid > 0) {
            Text(
                "${money(account.balance)} still outstanding. Any payment now is added on top of the ${money(account.paid)} already collected.",
                color = fg,
                fontSize = 11.sp
            )
        }
    }
}

/** Progress bar plus the headline numbers for one month of an account. */
@Composable
private fun PayAccountOverview(
    account: MonthAccount,
    title: String
) {
    PCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = C.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PayChip(status = account.status)
            }
            if (account.due > 0) {
                Box(
                    Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(50)).background(C.bg)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((account.paid.toFloat() / account.due).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                if (account.isFullyPaid) C.greenText else C.primary,
                                RoundedCornerShape(50)
                            )
                    )
                }
                Text(
                    "${money(account.paid)} of ${money(account.due)} collected",
                    color = C.muted,
                    fontSize = 11.sp
                )
            }
            HorizontalDivider(thickness = 1.dp, color = C.border)
            PayAmountLine("Amount Billed", money(account.due))
            PayAmountLine("Amount Paid", money(account.paid), valueColor = C.greenText)
            PayAmountLine(
                label = if (account.isFullyPaid) "Balance (Settled)" else "Balance Remaining",
                value = money(account.balance),
                valueColor = if (account.isFullyPaid) C.greenText else C.danger,
                bg = if (account.isFullyPaid) C.sageLt else C.dangerLt,
                emphasis = true
            )
            account.record?.let { rec ->
                HorizontalDivider(thickness = 1.dp, color = C.border)
                payInfoRow("Paid on", rec.paidDate ?: "—")
                payInfoRow("Method", rec.method.label)
                payInfoRow("Recorded by", if (rec.recordedBy == RecordedBy.GUARDIAN) "Guardian" else "Boarder")
                rec.verifiedAt?.let { payInfoRow("Verified", it) }
                rec.notes?.let {
                    Text(it, color = C.muted, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

/**
 * Inclusive From/To range check against an already-resolved timestamp. Mirrors
 * withinDateRange but takes the real payment instant rather than a display string, so
 * records with no paidDate are still filtered by when they actually happened.
 */
private fun withinRange(millis: Long, fromIso: String, toIso: String): Boolean {
    if (fromIso.isEmpty() && toIso.isEmpty()) return true
    val from = if (fromIso.isEmpty()) Long.MIN_VALUE else (parseIsoDate(fromIso) ?: Long.MIN_VALUE)
    val endOfDay = if (toIso.isEmpty()) {
        Long.MAX_VALUE
    } else {
        (parseIsoDate(toIso) ?: Long.MAX_VALUE) + 24L * 60 * 60 * 1000 - 1
    }
    return millis in from..endOfDay
}

/**
 * What to show as the date under a boarder's name in history. Records with no
 * paidDate are boarder uploads awaiting verification, so label them from their
 * billing period instead of claiming they are unpaid.
 */
private fun historyDateLabel(p: PaymentRecord): String =
    p.paidDate ?: "Uploaded · ${p.period}"

@Composable
fun GPayments(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    billing: BillingSettings,
    setBoarders: (List<Boarder>) -> Unit,
    setPayments: (List<PaymentRecord>) -> Unit,
    setBilling: (BillingSettings) -> Unit,
    showToast: (String) -> Unit,
    deepLinkBoarder: Boarder? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    var screen by remember { mutableStateOf("list") }
    var screenData by remember { mutableStateOf<Any?>(null) }
    var filter by remember { mutableStateOf("all") }
    val periods = remember(payments) { paymentPeriods(payments) }
    var month by remember { mutableStateOf(currentPeriod()) }
    LaunchedEffect(periods) {
        if (periods.none { it == month }) {
            month = periods.lastOrNull() ?: currentPeriod()
        }
    }

    fun nav(s: String, d: Any? = null) {
        screen = s
        screenData = d
    }

    fun back() = nav("list")

    LaunchedEffect(deepLinkBoarder) {
        val b = deepLinkBoarder ?: return@LaunchedEffect
        nav("detail", b)
        onDeepLinkConsumed()
    }

    if (screen == "detail") {
        GPay_Detail(
            boarder = (screenData as Boarder).let { b -> boarders.find { it.id == b.id } ?: b },
            month = month,
            periods = periods,
            payments = payments,
            setBoarders = setBoarders,
            setPayments = setPayments,
            showToast = showToast,
            onBack = { back() },
            onCash = { nav("cash", screenData as Boarder) }
        )
        return
    }
    if (screen == "cash") {
        GPay_Cash(
            boarders = boarders,
            payments = payments,
            setBoarders = setBoarders,
            setPayments = setPayments,
            showToast = showToast,
            onBack = { back() },
            selectedBoarder = (screenData as? Boarder),
            initialPeriod = month
        )
        return
    }
    if (screen == "balances") {
        GPay_Balances(boarders, payments, showToast, onBack = { back() })
        return
    }
    if (screen == "history") {
        GPay_History(boarders, payments, showToast, onBack = { back() })
        return
    }
    if (screen == "export") {
        GPay_Export(showToast, onBack = { back() })
        return
    }

    val accounts = remember(boarders, payments, month) {
        boarders.associate { b -> b.id to monthAccount(b, payments, month) }
    }
    val filtered = boarders.filter { b ->
        filter == "all" || (accounts[b.id] ?: monthAccount(b, payments, month)).status.name.lowercase() == filter
    }

    Screen(
        title = "Payment Management",
        right = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "💵" to "cash",
                    "📊" to "export"
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
        PayPeriodPager(periods = periods, selected = month, onSelect = { month = it })

        val pendingReview = payments.filter { it.receiptStatus == ReceiptStatus.PENDING_REVIEW }
        if (pendingReview.isNotEmpty() &&
            rememberPrefBoolean("g_payment_receipt_alerts", true).value
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.warnLt)
                    .border(1.dp, C.warn.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(enabled = true) {
                        val first = pendingReview.first()
                        boarders.find { it.id == first.boarderId }?.let { nav("detail", it) }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧾", fontSize = 18.sp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "${pendingReview.size} receipt${if (pendingReview.size == 1) "" else "s"} waiting for review",
                        color = C.warn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Open a boarder to verify or reject · tap to view",
                        color = C.warn,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text("›", color = C.warn, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
            payQuickTile(
                Modifier.weight(1f),
                "💰",
                "View Balances",
                "Due, paid & balance"
            ) { nav("balances") }
            payQuickTile(
                Modifier.weight(1f),
                "📜",
                "Payment History",
                "All recorded payments"
            ) { nav("history") }
        }

        val monthAccounts = boarders.map { b -> accounts[b.id] ?: monthAccount(b, payments, month) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            paySummaryCell(
                Modifier.weight(1f),
                monthAccounts.count { it.status == PayStatus.PAID },
                "Paid", C.sageLt, C.greenText
            )
            paySummaryCell(
                Modifier.weight(1f),
                monthAccounts.count { it.status == PayStatus.PARTIAL },
                "Partial", C.primaryLt, C.primary
            )
            paySummaryCell(
                Modifier.weight(1f),
                monthAccounts.count { it.status == PayStatus.PENDING },
                "Pending", C.bg, C.muted
            )
            paySummaryCell(
                Modifier.weight(1f),
                monthAccounts.count { it.status == PayStatus.OVERDUE },
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

        if (filtered.isEmpty()) {
            EmptyState(icon = "🔍", title = "No boarder records found for $month", sub = "Try selecting a different filter or month")
        } else {
            TableCard(payTableColumns) {
                filtered.forEachIndexed { index, b ->
                    val acc = accounts[b.id] ?: monthAccount(b, payments, month)
                    val rec = acc.record
                    TableRow(
                        columns = payTableColumns,
                        divider = index > 0,
                        onClick = { nav("detail", b) },
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
                                        "Room ${b.room} · ${money(b.monthlyRate)}/mo",
                                        color = C.muted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (rec?.receiptStatus == ReceiptStatus.PENDING_REVIEW) {
                                        Text(
                                            "🧾 Receipt pending",
                                            color = C.warn,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            },
                            {
                                Text(
                                    money(acc.due),
                                    color = C.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            {
                                Text(
                                    money(acc.paid),
                                    color = if (acc.paid > 0) C.greenText else C.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            { PayChip(status = acc.status) }
                        )
                    )
                }
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
private fun GPay_Detail(
    boarder: Boarder,
    month: String,
    periods: List<String>,
    payments: List<PaymentRecord>,
    setBoarders: (List<Boarder>) -> Unit,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit,
    onCash: () -> Unit
) {
    val bPay = payments.filter { p -> p.boarderId == boarder.id }
    val sepRec = bPay.find { p -> p.period == month }
    val sepAccount = monthAccount(boarder, payments, month)
    val accountRows = remember(boarder, payments, periods) { boarderAccounts(boarder, payments, periods) }
    val billedTotal = accountRows.sumOf { it.due }
    val paidTotal = accountRows.sumOf { it.paid }
    val outstandingTotal = (billedTotal - paidTotal).coerceAtLeast(0)
    val settledMonths = accountRows.count { it.isFullyPaid }
    var editRate by remember { mutableStateOf(false) }
    var newRate by remember { mutableStateOf(boarder.monthlyRate.toString()) }
    var rejReason by remember { mutableStateOf("") }
    var rejModal by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    val confirmPayments = rememberPrefBoolean("g_privacy_confirm_payments", true)

    fun verify(id: String, status: ReceiptStatus, reason: String? = null) {
        setPayments(
            payments.map { p ->
                if (p.id == id) {
                    p.copy(
                        receiptStatus = status,
                        rejectionReason = reason,
                        verifiedAt = SimpleDateFormat("MMM d · h:mm a", Locale.ENGLISH).format(java.util.Date()),
                        verifiedBy = "Guardian"
                    )
                } else p
            }
        )
        showToast(if (status == ReceiptStatus.VERIFIED) "✓ Receipt verified" else "Receipt rejected")
        rejModal = false
    }

    Box(Modifier.fillMaxSize()) {
        Screen(
            title = "Record Payment",
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
                PayChip(status = sepAccount.status)
            }

            PayAccountOverview(
                account = sepAccount,
                title = "This Month — $month"
            )

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "Overall Account",
                            color = C.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${settledMonths} of ${accountRows.size} months settled",
                            color = if (outstandingTotal > 0) C.danger else C.greenText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TableCard(paySummaryTableColumns) {
                        PaySummaryTableRow(
                            label = "Total Billed",
                            value = billedTotal,
                            valueColor = C.text,
                            share = if (billedTotal > 0) 1f else 0f,
                            barColor = C.primary,
                            divider = false,
                            bold = true
                        )
                        PaySummaryTableRow(
                            label = "Total Paid",
                            value = paidTotal,
                            valueColor = C.greenText,
                            share = if (billedTotal > 0) paidTotal.toFloat() / billedTotal else 0f,
                            barColor = C.greenText,
                            divider = true
                        )
                        PaySummaryTableRow(
                            label = "Outstanding",
                            value = outstandingTotal,
                            valueColor = if (outstandingTotal > 0) C.danger else C.greenText,
                            share = if (billedTotal > 0) outstandingTotal.toFloat() / billedTotal else 0f,
                            barColor = if (outstandingTotal > 0) C.danger else C.greenText,
                            divider = true,
                            bold = true
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = C.border)

                    payBillRow("Monthly Rate", money(boarder.monthlyRate))
                    payBillRow("Due Date", "Every ${ordinal(boarder.dueDay)} of month")
                    payBillRow("Grace Period", "${boarder.gracePeriodDays} days")
                    payBillRow(
                        "Late Penalty",
                        if (boarder.latePenaltyType == PenaltyType.FLAT) "₱${boarder.latePenaltyAmount} flat fee"
                        else "${boarder.latePenaltyAmount}% of rent"
                    )
                    payBillRow("Months Settled", "$settledMonths of ${accountRows.size}")
                }
            }

            PCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Month-by-Month", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (editRate) "Cancel" else "✏ Edit Rate",
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
                                    val rate = newRate.toIntOrNull()
                                    if (rate == null || rate <= 0) {
                                        showToast("Enter a valid monthly rate")
                                        return@PrimaryButton
                                    }
                                    setBoarders(listOf(boarder.copy(monthlyRate = rate)))
                                    editRate = false
                                    showToast("Billing rate updated")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                height = 45,
                                radius = 10,
                                fontSize = 14
                            )
                        }
                    }
                    if (accountRows.isEmpty()) {
                        EmptyState(icon = "📅", title = "No billing months yet")
                    } else {
                        TableCard(payMonthTableColumns) {
                            accountRows.forEachIndexed { index, acc ->
                                PayMonthTableRow(
                                    acc = acc,
                                    highlighted = acc.period == month,
                                    divider = index > 0
                                )
                            }
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(C.bg)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            color = C.muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.weight(1.15f)
                        )
                        Text(
                            money(billedTotal),
                            color = C.text,
                            fontSize = moneyFontSize(billedTotal).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(0.85f)
                        )
                        Text(
                            money(paidTotal),
                            color = C.greenText,
                            fontSize = moneyFontSize(paidTotal).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(0.85f)
                        )
                        Text(
                            money(outstandingTotal),
                            color = if (outstandingTotal > 0) C.danger else C.greenText,
                            fontSize = moneyFontSize(outstandingTotal).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Text(
                        "Totals cover all ${accountRows.size} month${if (accountRows.size == 1) "" else "s"} listed above.",
                        color = C.muted,
                        fontSize = 11.sp
                    )
                }
            }

            val recFileName = sepRec?.receiptFileName
            if (sepRec != null && recFileName != null) {
                val isPending = sepRec.receiptStatus == ReceiptStatus.PENDING_REVIEW
                PCard(borderColor = if (isPending) C.warn.copy(alpha = 0.5f) else C.border) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Proof of Payment — $month", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
                                        .clickable {
                                            if (confirmPayments.value) pendingAction = "verify"
                                            else verify(sepRec.id, ReceiptStatus.VERIFIED)
                                        },
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
                    if (rec != null) {
                        if (confirmPayments.value) {
                            rejModal = false
                            pendingAction = "reject"
                        } else {
                            verify(rec.id, ReceiptStatus.REJECTED, rejReason)
                        }
                    }
                },
                confirmLabel = "Reject Receipt",
                confirmDanger = true
            )
        }

        Modal(
            open = pendingAction != null,
            onClose = { pendingAction = null },
            title = if (pendingAction == "verify") "Verify Receipt" else "Reject Receipt"
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (pendingAction == "verify")
                        "Confirm that ${money(sepRec?.amount ?: 0)} from ${boarder.name} is verified for $month? This records you as the verifier."
                    else
                        "Confirm rejecting this receipt? ${boarder.name} will be asked to upload a new one.",
                    color = C.text,
                    fontSize = 14.sp
                )
                if (pendingAction == "reject") {
                    Text(
                        "Reason: ${rejReason.ifBlank { "(no reason given)" }}",
                        color = C.muted,
                        fontSize = 12.sp
                    )
                }
            }
            ActionPair(
                onCancel = { pendingAction = null },
                onConfirm = {
                    val rec = sepRec
                    val action = pendingAction
                    if (rec != null && action != null) {
                        if (action == "verify") verify(rec.id, ReceiptStatus.VERIFIED)
                        else verify(rec.id, ReceiptStatus.REJECTED, rejReason)
                    }
                    pendingAction = null
                },
                confirmLabel = if (pendingAction == "verify") "Confirm Verify" else "Confirm Reject",
                confirmDanger = pendingAction != "verify"
            )
        }
    }
}

@Composable
private fun payBillRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = C.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = C.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun payInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = C.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = C.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
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
                        Text("Paid So Far", color = C.primary, fontSize = 12.sp)
                        Text(
                            money(p.amountPaid),
                            color = C.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Balance Remaining", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            money(p.amount - p.amountPaid),
                            color = C.primary,
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
                                if (p.amount > 0) (p.amountPaid.toFloat() / p.amount).coerceIn(0f, 1f) else 0f
                            ).fillMaxHeight().background(C.primary, RoundedCornerShape(50))
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
    setBoarders: (List<Boarder>) -> Unit,
    setPayments: (List<PaymentRecord>) -> Unit,
    showToast: (String) -> Unit,
    onBack: () -> Unit,
    selectedBoarder: Boarder? = null,
    initialPeriod: String = currentPeriod()
) {
    var boarderId by remember(boarders) { mutableStateOf(selectedBoarder?.id ?: boarders.firstOrNull()?.id ?: "") }
    var period by remember { mutableStateOf(initialPeriod) }
    var payDate by remember {
        mutableStateOf(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(java.util.Date()))
    }
    var method by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val periodFormat = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

    val boarder = boarders.firstOrNull { it.id == boarderId } ?: boarders.firstOrNull()
    if (boarder == null) {
        EmptyState(icon = "💳", title = "No boarders yet")
        return
    }

    // The live account for the chosen boarder and month drives the whole form.
    val account = remember(boarder, payments, period) { monthAccount(boarder, payments, period) }
    val alreadyPaid = account.paid
    var amountDueInput by remember(boarder.id, period, account.due) { mutableStateOf(account.due.toString()) }
    var amountPaidInput by remember(boarder.id, period, account.due, account.balance) {
        mutableStateOf(account.balance.toString())
    }
    val amountDueVal = amountDueInput.toIntOrNull() ?: 0
    val amountPaidVal = amountPaidInput.toIntOrNull() ?: 0
    val balanceAfter = (amountDueVal - (alreadyPaid + amountPaidVal)).coerceAtLeast(0)

    // The cap tracks the due figure currently typed in, so raising the bill mid-entry
    // immediately opens up room for a larger payment.
    val canSubmit = amountDueVal > 0 &&
        amountPaidVal > 0 &&
        alreadyPaid + amountPaidVal <= amountDueVal

    /** Re-seeds the amount fields from the selected boarder's account for [p]. */
    fun syncToAccount(b: Boarder, p: String) {
        val acc = monthAccount(b, payments, p)
        amountDueInput = acc.due.toString()
        amountPaidInput = acc.balance.toString()
    }

    fun executeSave() {
        // Add onto whatever is already collected for this month rather than replacing it.
        val newPaidTotal = (alreadyPaid + amountPaidVal).coerceAtMost(amountDueVal)
        val stillOwing = (amountDueVal - newPaidTotal).coerceAtLeast(0)
        val isPartial = stillOwing > 0
        val finalNotes = notes.ifBlank {
            if (isPartial) "In-person payment — balance ${money(stillOwing)} remaining" else "Paid in person"
        }

        val existing = payments.firstOrNull { it.boarderId == boarder.id && it.period == period }
        val newRec = PaymentRecord(
            id = existing?.id ?: ("p" + System.currentTimeMillis()),
            boarderId = boarder.id,
            period = period,
            amount = amountDueVal,
            amountPaid = if (isPartial) newPaidTotal else null,
            paidDate = payDate,
            method = when (method) {
                "cash" -> PayMethod.CASH
                "gcash" -> PayMethod.GCASH
                "bank_transfer" -> PayMethod.BANK_TRANSFER
                else -> PayMethod.ONLINE
            },
            status = if (isPartial) PayStatus.PARTIAL else PayStatus.PAID,
            receiptStatus = existing?.receiptStatus,
            receiptFileName = existing?.receiptFileName,
            rejectionReason = existing?.rejectionReason,
            recordedBy = RecordedBy.GUARDIAN,
            verifiedBy = "Guardian",
            verifiedAt = "$payDate · recorded",
            notes = finalNotes
        )

        val updatedPayments = if (existing == null) {
            payments + newRec
        } else {
            payments.map { if (it.id == existing.id) newRec else it }
        }
        setPayments(updatedPayments)
        setBoarders(
            boarders.map { b ->
                if (b.id == boarder.id) b.copy(
                    paymentStatus = if (isPartial) PayStatus.PARTIAL else PayStatus.PAID
                ) else b
            }
        )
        showToast("Payment of ${money(amountPaidVal)} recorded for $period")
        onBack()
    }

    Screen(title = "Record In-Person Payment", onBack = onBack) {
        Select(
            label = "Select Boarder",
            value = boarderId,
            onChange = { id ->
                boarderId = id
                boarders.firstOrNull { it.id == id }?.let { syncToAccount(it, period) }
            },
            options = boarders.map { Option(it.id, "${it.name} — Room ${it.room}") }
        )

        // Calendar selectors keep the billing period and payment date explicit.
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.bg)
                .border(1.dp, C.border, RoundedCornerShape(10.dp))
                .clickable {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            val picked = periodFormat.format(calendar.time)
                            period = picked
                            syncToAccount(boarder, picked)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text("Billing Period (calendar)", color = C.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(period, color = C.text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Automatically reflects whether this boarder is already settled for the chosen month.
        PayPeriodStatusBanner(account = account, boarderName = boarder.name)

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.bg)
                .border(1.dp, C.border, RoundedCornerShape(10.dp))
                .clickable {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(year, month, day)
                            payDate = dateFormat.format(calendar.time)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text("Payment Date (calendar)", color = C.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(payDate, color = C.text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Amount Due (Editable - Req 4.B)
        Input(
            label = "Amount Due (₱)",
            value = amountDueInput,
            onChange = { amountDueInput = it },
            keyboardType = KeyboardType.Number,
            placeholder = "e.g. 5000"
        )

        Input(
            label = if (alreadyPaid > 0) "Amount Paid Now (₱) · ${money(account.balance)} outstanding" else "Amount Paid Now (₱)",
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
                .background(if (balanceAfter == 0) C.sageLt else C.primaryLt)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Due:", color = C.muted, fontSize = 13.sp)
                Text(money(amountDueVal), color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (alreadyPaid > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Already Paid:", color = C.muted, fontSize = 13.sp)
                    Text(money(alreadyPaid), color = C.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Paid:", color = C.greenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(money(alreadyPaid + amountPaidVal), color = C.greenText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remaining Balance:", color = if (balanceAfter > 0) C.primary else C.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(money(balanceAfter), color = if (balanceAfter > 0) C.primary else C.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            text = if (alreadyPaid > 0) "Add Payment" else "Record Payment",
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        )
        if (account.isFullyPaid) {
            Text(
                "Nothing is outstanding for $period.",
                color = C.muted,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Confirmation Dialog before permanently saving payment record (Req 4.C)
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Payment Record", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (alreadyPaid > 0)
                                "Add ${money(amountPaidVal)} to the ${money(alreadyPaid)} already paid by ${boarder.name} for $period?"
                            else
                                "Are you sure you want to record a payment of ${money(amountPaidVal)} for ${boarder.name} for $period?",
                            color = C.text,
                            fontSize = 14.sp
                        )
                        Text(
                            "Total for $period: ${money(amountDueVal)} due · ${money(alreadyPaid + amountPaidVal)} paid · ${money(balanceAfter)} remaining",
                            color = C.muted,
                            fontSize = 12.sp
                        )
                    }
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
fun GPay_Settings(
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
    val iso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var from by remember { mutableStateOf(iso.format(java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))) }
    var to by remember { mutableStateOf(iso.format(java.util.Date())) }

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

@Composable
private fun payQuickTile(
    modifier: Modifier,
    icon: String,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, C.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(
            label,
            color = C.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(sub, color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun GPay_Balances(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    val periods = remember(payments) { paymentPeriods(payments) }
    var period by remember { mutableStateOf(currentPeriod()) }
    LaunchedEffect(periods) {
        if (periods.none { it == period }) {
            period = periods.lastOrNull() ?: currentPeriod()
        }
    }
    var statusFilter by remember { mutableStateOf("all") }

    val rows = boarders.map { b ->
        val acc = monthAccount(b, payments, period)
        Triple(b, acc.record, acc)
    }

    val totalDue = rows.sumOf { it.third.due }
    val totalPaid = rows.sumOf { it.third.paid }
    val totalOutstanding = (totalDue - totalPaid).coerceAtLeast(0)

    val visible = rows.filter { (_, rec, acc) ->
        when (statusFilter) {
            "unpaid" -> !acc.isFullyPaid
            "paid" -> acc.isFullyPaid
            "partial" -> acc.status == PayStatus.PARTIAL
            else -> true
        }
    }

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Balances", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            PayPeriodPager(
                periods = periods,
                selected = period,
                onSelect = { period = it },
                caption = "RENT PERIOD"
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = "Total Due",
                    value = money(totalDue),
                    sub = "${rows.size} boarders",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Collected",
                    value = money(totalPaid),
                    sub = "$totalOutstanding outstanding",
                    bg = C.sageLt,
                    textColor = C.greenText,
                    modifier = Modifier.weight(1f)
                )
            }

            ChipBar(
                options = listOf(
                    "All" to "all",
                    "Unpaid only" to "unpaid",
                    "✓ Paid" to "paid",
                    "½ Partial" to "partial"
                ),
                value = statusFilter,
                onChange = { statusFilter = it }
            )

            Text(
                "Showing ${visible.size} of ${rows.size} boarders for $period",
                color = C.muted,
                fontSize = 12.sp
            )

            if (visible.isEmpty()) {
                EmptyState(
                    icon = "🧾",
                    title = if (statusFilter == "unpaid") "Everyone has paid" else "No matching records",
                    sub = if (statusFilter == "unpaid") "All boarders are fully paid for $period" else "Try another filter"
                )
            } else {
                TableCard(balanceTableColumns) {
                    visible.forEachIndexed { index, row ->
                        val (b, rec, acc) = row
                        val balance = acc.balance
                        TableRow(
                            columns = balanceTableColumns,
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
                                            "Room ${b.room}",
                                            color = C.muted,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (rec != null) {
                                            PayChip(
                                                status = acc.status,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        } else {
                                            Text(
                                                "No record",
                                                color = C.muted,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                },
                                {
Text(
                                    money(acc.due),
                                    color = C.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            {
                                Text(
                                    money(acc.paid),
                                    color = if (acc.paid > 0) C.greenText else C.muted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                                {
                                    Text(
                                        money(balance),
                                        color = if (balance > 0) C.primary else C.greenText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GPay_History(
    boarders: List<Boarder>,
    payments: List<PaymentRecord>,
    showToast: (String) -> Unit,
    onBack: () -> Unit
) {
    var statusFilter by remember { mutableStateOf("all") }
    var periodFilter by remember { mutableStateOf("all") }
    var fromIso by remember { mutableStateOf("") }
    var toIso by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val periods = remember(payments) {
        payments.map { it.period }.distinct().sortedByDescending { periodSortKey(it) ?: Int.MIN_VALUE }
    }
    val periodOptions = listOf(Option("all", "All periods")) + periods.map { Option(it, it) }

    // Order by the real payment instant, not the billing period, so a boarder upload
    // from the 28th ranks above an in-person payment from the 3rd of the same month.
    val filtered = payments.filter { p ->
        val statusOk = statusFilter == "all" || p.status.name.lowercase() == statusFilter
        val periodOk = periodFilter == "all" || p.period == periodFilter
        val dateOk = withinRange(p.effectivePaidMillis(), fromIso, toIso)
        statusOk && periodOk && dateOk
    }.sortedWith(
        compareByDescending<PaymentRecord> { it.effectivePaidMillis() }
            .thenByDescending { periodSortKey(it.period) ?: Int.MIN_VALUE }
    )

    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = "Payment History", onBack = onBack)
        ScrollBody(modifier = Modifier.weight(1f), spacing = 12) {
            SectionHeader(title = "Filter by date")
            DateRangeFilter(from = fromIso, to = toIso, onChange = { f, t -> fromIso = f; toIso = t })

            Select(
                value = periodFilter,
                onChange = { periodFilter = it },
                options = periodOptions,
                label = "Rent period"
            )

            ChipBar(
                options = listOf(
                    "All" to "all",
                    "✓ Paid" to "paid",
                    "½ Partial" to "partial",
                    "⏳ Pending" to "pending",
                    "⚠ Overdue" to "overdue"
                ),
                value = statusFilter,
                onChange = { statusFilter = it }
            )

            Text(
                "${filtered.size} of ${payments.size} payments",
                color = C.muted,
                fontSize = 12.sp
            )

            if (filtered.isEmpty()) {
                EmptyState(icon = "💳", title = "No payments found", sub = "Try a different date range, period, or status")
            } else {
                TableCard(historyTableColumns) {
                    filtered.forEachIndexed { index, p ->
                        val b = boarders.find { it.id == p.boarderId }
                        val expanded = expandedId == p.id

                        TableRow(
                            columns = historyTableColumns,
                            divider = index > 0,
                            onClick = { expandedId = if (expanded) null else p.id },
                            cells = listOf(
                                {
                                    Column {
                                        Text(
                                            b?.name ?: p.boarderId,
                                            color = C.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            historyDateLabel(p),
                                            color = C.muted,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                },
                                {
                                    Text(
                                        p.period,
                                        color = C.text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                {
                                    Text(
                                        money(p.amountPaid ?: p.amount),
                                        color = if (p.status == PayStatus.PAID) C.greenText else C.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                {
                                    Column(horizontalAlignment = Alignment.End) {
                                        PayChip(status = p.status)
                                        Text(
                                            if (expanded) "Less ↑" else "More ↓",
                                            color = C.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            )
                        )

                        if (expanded) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(C.bg)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                payInfoRow("Paid on", p.paidDate ?: "—")
                                payInfoRow("Method", p.method.label)
                                payInfoRow("Recorded by", if (p.recordedBy == RecordedBy.GUARDIAN) "Guardian" else "Boarder")
                                p.verifiedAt?.let { payInfoRow("Verified", it + (p.verifiedBy?.let { v -> " by $v" } ?: "")) }
                                p.notes?.let { payInfoRow("Notes", it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
