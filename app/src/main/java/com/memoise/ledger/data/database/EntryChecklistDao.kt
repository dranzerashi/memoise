package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryChecklistDao {
    @Query(
        "SELECT * FROM entry_checklist_items WHERE entryId = :entryId " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    fun observeForEntry(entryId: String): Flow<List<EntryChecklistEntity>>

    @Query("SELECT * FROM entry_checklist_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EntryChecklistEntity?

    @Query("SELECT MAX(sortOrder) FROM entry_checklist_items WHERE entryId = :entryId")
    suspend fun maxSortOrder(entryId: String): Int?

    @Upsert
    suspend fun upsert(item: EntryChecklistEntity)

    @Query("DELETE FROM entry_checklist_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
