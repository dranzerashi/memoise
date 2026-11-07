package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query(
        """SELECT * FROM ledger_entries
            ORDER BY CASE WHEN scheduledDate IS NULL THEN 1 ELSE 0 END,
                     scheduledDate ASC,
                     CASE WHEN scheduledTime IS NULL THEN 1 ELSE 0 END,
                     scheduledTime ASC,
                     createdAt ASC""",
    )
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT id FROM ledger_entries WHERE id = :id LIMIT 1")
    suspend fun findId(id: String): String?

    @Upsert
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
