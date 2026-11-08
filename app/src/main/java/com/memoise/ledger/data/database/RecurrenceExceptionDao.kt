package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RecurrenceExceptionDao {
    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceSeriesId = :seriesId")
    suspend fun forSeries(seriesId: String): List<RecurrenceExceptionEntity>

    @Query("SELECT * FROM recurrence_exceptions WHERE occurrenceEntryId = :entryId LIMIT 1")
    suspend fun forOccurrence(entryId: String): RecurrenceExceptionEntity?

    @Upsert
    suspend fun upsert(exception: RecurrenceExceptionEntity)

    @Query("DELETE FROM recurrence_exceptions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recurrence_exceptions WHERE occurrenceEntryId = :entryId")
    suspend fun deleteForOccurrence(entryId: String)
}
