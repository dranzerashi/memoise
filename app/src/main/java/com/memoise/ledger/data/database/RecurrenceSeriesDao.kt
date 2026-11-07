package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceSeriesDao {
    @Query("SELECT * FROM recurrence_series WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<RecurrenceSeriesEntity?>

    @Query("SELECT * FROM recurrence_series WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RecurrenceSeriesEntity?

    @Query("SELECT * FROM recurrence_series WHERE active = 1")
    suspend fun findActive(): List<RecurrenceSeriesEntity>

    @Upsert
    suspend fun upsert(series: RecurrenceSeriesEntity)
}
