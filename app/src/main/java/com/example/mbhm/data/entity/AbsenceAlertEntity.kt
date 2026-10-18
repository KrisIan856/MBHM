package com.example.mbhm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mbhm.model.AbsenceAlertRecord

@Entity(tableName = "absence_alerts")
data class AbsenceAlertEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val absenceCountAtSend: Int,
    val thresholdAtSend: Int,
    val sentOn: String,
    val sentAt: String? = null
) {
    fun toDomain(): AbsenceAlertRecord = AbsenceAlertRecord(
        id = id,
        boarderId = boarderId,
        absenceCountAtSend = absenceCountAtSend,
        thresholdAtSend = thresholdAtSend,
        sentOn = sentOn,
        sentAt = sentAt
    )

    companion object {
        fun fromDomain(record: AbsenceAlertRecord): AbsenceAlertEntity = AbsenceAlertEntity(
            id = record.id,
            boarderId = record.boarderId,
            absenceCountAtSend = record.absenceCountAtSend,
            thresholdAtSend = record.thresholdAtSend,
            sentOn = record.sentOn,
            sentAt = record.sentAt
        )
    }
}