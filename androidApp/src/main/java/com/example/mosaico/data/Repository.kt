package com.example.mosaico.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository(private val db: AppDatabase) {
    private val clientDao = db.clientDao()
    private val reservationDao = db.reservationDao()

    suspend fun saveClients(clients: List<ClientEntity>) = withContext(Dispatchers.IO) {
        clientDao.clear()
        if (clients.isNotEmpty()) clientDao.upsert(*clients.toTypedArray())
    }

    suspend fun addOrUpdateClient(client: ClientEntity) = withContext(Dispatchers.IO) {
        clientDao.upsert(client)
    }

    suspend fun getClients(): List<ClientEntity> = withContext(Dispatchers.IO) { clientDao.getAll() }

    suspend fun deleteClient(id: Long) = withContext(Dispatchers.IO) { clientDao.deleteById(id) }

    suspend fun saveReservation(reservation: ReservationEntity) = withContext(Dispatchers.IO) {
        reservationDao.upsert(reservation)
    }

    suspend fun getReservations(): List<ReservationEntity> = withContext(Dispatchers.IO) { reservationDao.getAll() }

    suspend fun deleteReservation(id: Long) = withContext(Dispatchers.IO) { reservationDao.deleteById(id) }
}
