package com.example.mbhm.data.repository

import com.example.mbhm.data.dao.LeaveNoticeDao
import com.example.mbhm.data.entity.LeaveNoticeEntity
import com.example.mbhm.model.LeaveNotice
import com.example.mbhm.model.LeaveNoticeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LeaveNoticeRepository(private val dao: LeaveNoticeDao) {

    fun getAll(): Flow<List<LeaveNotice>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    fun getByBoarder(boarderId: String): Flow<List<LeaveNotice>> =
        dao.getByBoarder(boarderId).map { list -> list.map { it.toDomain() } }

    fun getByStatus(status: LeaveNoticeStatus): Flow<List<LeaveNotice>> =
        dao.getByStatus(status.name).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): LeaveNotice? =
        dao.getById(id)?.toDomain()

    suspend fun insert(notice: LeaveNotice) {
        dao.insert(LeaveNoticeEntity.fromDomain(notice))
    }

    suspend fun update(notice: LeaveNotice) {
        dao.update(LeaveNoticeEntity.fromDomain(notice))
    }

    suspend fun updateStatus(id: String, status: LeaveNoticeStatus) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(status = status))
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
