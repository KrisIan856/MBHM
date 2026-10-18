package com.example.mbhm.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.mbhm.data.converter.Converters
import com.example.mbhm.model.LeaveNotice
import com.example.mbhm.model.LeaveNoticeStatus

@Entity(tableName = "leave_notices")
@TypeConverters(Converters::class)
data class LeaveNoticeEntity(
    @PrimaryKey val id: String,
    val boarderId: String,
    val leaveDate: String,
    val expectedDepartureTime: String,
    val expectedReturnDate: String,
    val expectedReturnTime: String,
    val reason: String,
    val destination: String,
    val additionalNotes: String? = null,
    val status: LeaveNoticeStatus = LeaveNoticeStatus.PENDING,
    val submittedAt: String,
    val verifiedBy: String? = null,
    val verifiedAt: String? = null,
    val departedAt: String? = null,
    val arrivedAt: String? = null
) {
    fun toDomain(): LeaveNotice = LeaveNotice(
        id = id,
        boarderId = boarderId,
        leaveDate = leaveDate,
        expectedDepartureTime = expectedDepartureTime,
        expectedReturnDate = expectedReturnDate,
        expectedReturnTime = expectedReturnTime,
        reason = reason,
        destination = destination,
        additionalNotes = additionalNotes,
        status = status,
        submittedAt = submittedAt,
        verifiedBy = verifiedBy,
        verifiedAt = verifiedAt,
        departedAt = departedAt,
        arrivedAt = arrivedAt
    )

    companion object {
        fun fromDomain(notice: LeaveNotice): LeaveNoticeEntity = LeaveNoticeEntity(
            id = notice.id,
            boarderId = notice.boarderId,
            leaveDate = notice.leaveDate,
            expectedDepartureTime = notice.expectedDepartureTime,
            expectedReturnDate = notice.expectedReturnDate,
            expectedReturnTime = notice.expectedReturnTime,
            reason = notice.reason,
            destination = notice.destination,
            additionalNotes = notice.additionalNotes,
            status = notice.status,
            submittedAt = notice.submittedAt,
            verifiedBy = notice.verifiedBy,
            verifiedAt = notice.verifiedAt,
            departedAt = notice.departedAt,
            arrivedAt = notice.arrivedAt
        )
    }
}
