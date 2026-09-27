package com.example.mbhm.data.repository

import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.PaymentRecordEntity
import com.example.mbhm.model.PaymentRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentRepository(private val db: AppDatabase) {
    fun getAllPayments(): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getAll().map { it.map { it.toDomain() } }

    fun getPaymentsByBoarder(boarderId: String): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getByBoarder(boarderId).map { it.map { it.toDomain() } }

    suspend fun getPayment(boarderId: String, period: String): PaymentRecord? =
        db.paymentRecordDao().getByBoarderAndPeriod(boarderId, period)?.toDomain()

    fun getPaymentsByStatus(status: String): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getByStatus(status).map { it.map { it.toDomain() } }

    fun getPaymentsByReceiptStatus(status: String): Flow<List<PaymentRecord>> =
        db.paymentRecordDao().getByReceiptStatus(status).map { it.map { it.toDomain() } }

    suspend fun insertPayment(record: PaymentRecord) =
        db.paymentRecordDao().insert(PaymentRecordEntity.fromDomain(record))

    suspend fun insertAllPayments(records: List<PaymentRecord>) =
        db.paymentRecordDao().insertAll(records.map { PaymentRecordEntity.fromDomain(it) })

    suspend fun updatePayment(record: PaymentRecord) =
        db.paymentRecordDao().update(PaymentRecordEntity.fromDomain(record))
}