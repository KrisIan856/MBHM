package com.example.mbhm.model

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Absence-alert bookkeeping.
 *
 * The rule the guardian screen enforces: an alert is owed only when a boarder's absence
 * total is at or above the threshold **and** has risen since the last alert was sent.
 * Once they have been alerted for their current total, the banner stops asking, so the
 * same absence is never nagged about daily. Missing again raises the total, which makes
 * a fresh alert due again.
 */

/** Total absences recorded for each boarder. */
fun absenceCounts(records: List<AttendanceRecord>): Map<String, Int> =
    records
        .filter { it.status == AttendanceStatus.ABSENT }
        .groupingBy { it.boarderId }
        .eachCount()

/** Highest absence total already alerted for each boarder, 0 when never alerted. */
fun lastAlertedAbsenceCounts(alerts: List<AbsenceAlertRecord>): Map<String, Int> =
    alerts.groupBy { it.boarderId }
        .mapValues { (_, rows) -> rows.maxOf { it.absenceCountAtSend } }

/** One boarder's absence standing, as shown in the threshold banner. */
data class AbsenceAlertState(
    val boarder: Boarder,
    val absenceCount: Int,
    val threshold: Int,
    val lastAlertedAt: Int
) {
    val isOverThreshold: Boolean get() = absenceCount >= threshold

    /**
     * True when the guardian still owes this boarder a notice: they are at or over the
     * threshold and have not already been alerted at this exact total.
     */
    val needsAlert: Boolean get() = isOverThreshold && absenceCount > lastAlertedAt

    val hasBeenAlerted: Boolean get() = lastAlertedAt > 0

    /** How many further absences have happened since the last notice went out. */
    val absencesSinceAlert: Int get() = (absenceCount - lastAlertedAt).coerceAtLeast(0)
}

/**
 * Works out who is over the absence threshold and who still needs an alert, ordered by
 * the most absences first so the worst case leads the list.
 */
fun absenceAlertStates(
    boarders: List<Boarder>,
    records: List<AttendanceRecord>,
    alerts: List<AbsenceAlertRecord>,
    threshold: Int
): List<AbsenceAlertState> {
    val counts = absenceCounts(records)
    val alerted = lastAlertedAbsenceCounts(alerts)
    return boarders
        .map { b ->
            AbsenceAlertState(
                boarder = b,
                absenceCount = counts[b.id] ?: 0,
                threshold = threshold,
                lastAlertedAt = alerted[b.id] ?: 0
            )
        }
        .filter { it.isOverThreshold }
        .sortedByDescending { it.absenceCount }
}

/** Builds the alert rows to persist when the guardian taps Send Alerts. */
fun buildAbsenceAlerts(
    states: List<AbsenceAlertState>,
    todayLabel: String,
    stamp: String = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.ENGLISH).format(java.util.Date())
): List<AbsenceAlertRecord> = states
    .filter { it.needsAlert }
    .map { s ->
        AbsenceAlertRecord(
            id = "aa-${s.boarder.id}-${s.absenceCount}-${todayLabel.replace(" ", "")}",
            boarderId = s.boarder.id,
            absenceCountAtSend = s.absenceCount,
            thresholdAtSend = s.threshold,
            sentOn = todayLabel,
            sentAt = stamp
        )
    }