package com.example.mbhm.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Abbreviated month names used to build the `"MMM yyyy"` billing-period keys. */
private val PERIOD_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** The billing period for [millis], e.g. `"Oct 2026"`. */
fun periodOf(millis: Long): String = SimpleDateFormat("MMM yyyy", Locale.US).format(millis)

/** The billing period for right now. */
fun currentPeriod(): String = periodOf(System.currentTimeMillis())

/**
 * `"Oct 2026"` as a sortable `year * 12 + monthIndex` value, or null when the key
 * cannot be parsed. Sorting on this keeps months in real calendar order instead of
 * alphabetical order.
 */
fun periodSortKey(period: String): Int? {
    val parts = period.trim().split(Regex("\\s+"))
    if (parts.size != 2) return null
    val month = PERIOD_MONTHS.indexOfFirst { it.equals(parts[0], ignoreCase = true) }
    val year = parts[1].toIntOrNull()
    if (month < 0 || year == null) return null
    return year * 12 + month
}

/** Shifts a `"MMM yyyy"` key by [delta] months, handling year boundaries. */
fun shiftPeriod(period: String, delta: Int): String {
    val key = periodSortKey(period) ?: return period
    val total = key + delta
    return PERIOD_MONTHS[total % 12] + " " + (total / 12)
}

/**
 * Every month a payment can be paged through: a window around today plus any month that
 * already has records. Chronologically ordered so callers can safely index into it.
 */
fun paymentPeriods(
    payments: List<PaymentRecord>,
    monthsBack: Int = 3,
    monthsForward: Int = 1
): List<String> {
    val now = currentPeriod()
    val seen = LinkedHashSet<String>()
    for (offset in -monthsBack..monthsForward) seen += shiftPeriod(now, offset)
    payments.forEach { if (it.period.isNotBlank()) seen += it.period.trim() }
    return seen.sortedBy { periodSortKey(it) ?: Int.MAX_VALUE }
}

/**
 * Cash actually collected on a record. A PAID record may leave [PaymentRecord.amountPaid]
 * null, in which case the full amount was paid.
 */
fun PaymentRecord.paidAmount(): Int = when (status) {
    PayStatus.PAID -> amountPaid ?: amount
    else -> amountPaid ?: 0
}

/** [PaymentRecord.paidAmount] for a possibly absent record. */
fun paidAmountOf(record: PaymentRecord?): Int = record?.paidAmount() ?: 0

/**
 * Epoch millis encoded in a generated record id (`"p" + System.currentTimeMillis()`),
 * or null when the id was not generated this way (e.g. seeded or server-assigned ids).
 */
private fun idTimestampMillis(id: String): Long? =
    id.removePrefix("p").toLongOrNull()

/**
 * When the payment actually happened, for ordering history newest-first.
 *
 * Falls back through progressively weaker signals because a boarder-submitted payment
 * is created with a null [PaymentRecord.paidDate]: the verified stamp, then the id's
 * embedded creation time, and only then the start of the billing period. Without this,
 * every boarder upload sorts to the first of its month and appears older than a
 * guardian-recorded payment made later the same month.
 */
fun PaymentRecord.effectivePaidMillis(): Long {
    parseDisplayDateCompat(paidDate ?: "")?.let { return it }
    parseDisplayDateCompat(verifiedAt ?: "")?.let { return it }
    idTimestampMillis(id)?.let { return it }
    return periodSortKey(period)?.let { key ->
        Calendar.getInstance().apply {
            clear()
            set(key / 12, key % 12, 1)
        }.timeInMillis
    } ?: 0L
}

/** Local alias so the model layer does not import the UI component layer. */
private fun parseDisplayDateCompat(raw: String): Long? = runCatching {
    if (raw.isBlank()) return null
    val parts = raw.trim().split(Regex("\\s+"))
    val monthName = parts.firstOrNull { it.firstOrNull()?.isLetter() == true } ?: return null
    val month = PERIOD_MONTHS.indexOfFirst { it.equals(monthName.take(3), ignoreCase = true) }
    if (month < 0) return null
    val year = parts.lastOrNull { it.firstOrNull()?.isDigit() == true && it.length >= 4 }?.toIntOrNull()
        ?: return null
    val day = parts.firstOrNull { it.firstOrNull()?.isDigit() == true && it.length <= 2 }
        ?.toIntOrNull()
        ?: 1
    java.util.Calendar.getInstance().apply {
        clear()
        set(year, month, day.coerceIn(1, 31))
    }.timeInMillis
}.getOrNull()

/**
 * One month of a boarder's account: what was billed, what was actually collected and
 * what is left. [due] prefers the amount stored on the record over the boarder's current
 * rate so a rate change never rewrites history.
 */
data class MonthAccount(
    val period: String,
    val due: Int,
    val paid: Int,
    val record: PaymentRecord?
) {
    val balance: Int get() = (due - paid).coerceAtLeast(0)

    /** True only when there is a real bill and it has been settled in full. */
    val isFullyPaid: Boolean get() = due > 0 && paid >= due

    val hasRecord: Boolean get() = record != null

    val status: PayStatus
        get() = when {
            isFullyPaid -> PayStatus.PAID
            paid > 0 -> PayStatus.PARTIAL
            record?.status == PayStatus.OVERDUE -> PayStatus.OVERDUE
            else -> PayStatus.PENDING
        }
}

/** The account [boarder] has for [period], whether or not a record exists yet. */
fun monthAccount(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    period: String
): MonthAccount {
    val record = payments.find { it.boarderId == boarder.id && it.period == period }
    return MonthAccount(
        period = period,
        due = record?.amount ?: boarder.monthlyRate,
        paid = paidAmountOf(record),
        record = record
    )
}

/** Month-by-month accounts for [boarder] across [periods], oldest first. */
fun boarderAccounts(
    boarder: Boarder,
    payments: List<PaymentRecord>,
    periods: List<String>
): List<MonthAccount> = periods.map { monthAccount(boarder, payments, it) }