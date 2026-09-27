package com.example.mbhm.data.repository

import com.example.mbhm.data.database.AppDatabase
import com.example.mbhm.data.entity.BoarderEntity
import com.example.mbhm.model.Boarder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BoarderRepository(private val db: AppDatabase) {
    fun getAllBoarders(): Flow<List<Boarder>> =
        db.boarderDao().getAll().map { it.map { it.toDomain() } }

    suspend fun getBoarderById(id: String): Boarder? =
        db.boarderDao().getById(id)?.toDomain()

    fun getBoardersByRoom(room: String): Flow<List<Boarder>> =
        db.boarderDao().getByRoom(room).map { it.map { it.toDomain() } }

    fun getBoardersByPaymentStatus(status: String): Flow<List<Boarder>> =
        db.boarderDao().getByPaymentStatus(status).map { it.map { it.toDomain() } }

    suspend fun insertBoarder(boarder: Boarder) =
        db.boarderDao().insert(BoarderEntity.fromDomain(boarder))

    suspend fun insertAllBoarders(boarders: List<Boarder>) =
        db.boarderDao().insertAll(boarders.map { BoarderEntity.fromDomain(it) })

    suspend fun updateBoarder(boarder: Boarder) =
        db.boarderDao().update(BoarderEntity.fromDomain(boarder))

    suspend fun deleteBoarder(boarder: Boarder) =
        db.boarderDao().delete(BoarderEntity.fromDomain(boarder))
}