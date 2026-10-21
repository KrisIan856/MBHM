package com.example.mbhm.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentVerificationTest {
    private fun payment(
        status: PayStatus,
        receiptStatus: ReceiptStatus?,
        recordedBy: RecordedBy,
        amountPaid: Int? = null
    ) = PaymentRecord(
        id = "p1",
        boarderId = "b1",
        period = "Oct 2026",
        amount = 5_000,
        method = PayMethod.GCASH,
        status = status,
        receiptStatus = receiptStatus,
        amountPaid = amountPaid,
        recordedBy = recordedBy
    )

    @Test
    fun `boarder payment awaiting review is not counted`() {
        val record = payment(PayStatus.PAID, ReceiptStatus.PENDING_REVIEW, RecordedBy.BOARDER)
        assertEquals(0, record.paidAmount())
    }

    @Test
    fun `rejected boarder payment is not counted`() {
        val record = payment(
            PayStatus.PARTIAL,
            ReceiptStatus.REJECTED,
            RecordedBy.BOARDER,
            amountPaid = 2_000
        )
        assertEquals(0, record.paidAmount())
    }

    @Test
    fun `verified boarder partial payment is counted`() {
        val record = payment(
            PayStatus.PARTIAL,
            ReceiptStatus.VERIFIED,
            RecordedBy.BOARDER,
            amountPaid = 2_000
        )
        assertEquals(2_000, record.paidAmount())
    }

    @Test
    fun `guardian-recorded payment is counted without a receipt review`() {
        val record = payment(
            PayStatus.PARTIAL,
            receiptStatus = null,
            recordedBy = RecordedBy.GUARDIAN,
            amountPaid = 3_000
        )
        assertEquals(3_000, record.paidAmount())
    }

    @Test
    fun `paid record with no amount stored counts the full amount`() {
        val guardian = payment(PayStatus.PAID, null, RecordedBy.GUARDIAN)
        val boarder = payment(PayStatus.PAID, ReceiptStatus.VERIFIED, RecordedBy.BOARDER)

        assertEquals(5_000, guardian.paidAmount())
        assertEquals(5_000, boarder.paidAmount())
    }

    @Test
    fun `pending boarder payment does not reduce a month balance`() {
        val boarder = Boarder(
            id = "b1",
            name = "Test Boarder",
            room = "101",
            floor = "1",
            initials = "TB",
            avatarColor = "#000000",
            phone = "",
            email = "",
            joinDate = "Oct 1, 2026",
            monthlyRate = 5_000,
            dueDay = 5,
            gracePeriodDays = 3,
            paymentStatus = PayStatus.PENDING,
            latePenaltyType = PenaltyType.FLAT,
            latePenaltyAmount = 0,
            emergencyContact = EmergencyContact(name = "", phone = ""),
            guardianContact = GuardianContact(name = "", phone = ""),
            job = "Student",
            idNumber = "MBH-1"
        )
        val pending = payment(PayStatus.PAID, ReceiptStatus.PENDING_REVIEW, RecordedBy.BOARDER)

        val account = monthAccount(boarder, listOf(pending), "Oct 2026")

        assertEquals(0, account.paid)
        assertEquals(5_000, account.balance)
    }
}
