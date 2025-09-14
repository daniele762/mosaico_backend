package com.example.mosaico.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAll(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg clients: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clients")
    suspend fun clear()
}

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY date ASC, time ASC")
    fun observeAll(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations ORDER BY date ASC, time ASC")
    suspend fun getAll(): List<ReservationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg reservations: ReservationEntity)

    @Query("DELETE FROM reservations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reservations")
    suspend fun clear()
}
