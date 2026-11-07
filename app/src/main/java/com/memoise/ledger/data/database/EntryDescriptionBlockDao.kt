package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDescriptionBlockDao {
    @Query(
        "SELECT * FROM entry_description_blocks WHERE entryId = :entryId " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    fun observeForEntry(entryId: String): Flow<List<EntryDescriptionBlockEntity>>

    @Query("SELECT * FROM entry_description_blocks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EntryDescriptionBlockEntity?

    @Query("SELECT MAX(sortOrder) FROM entry_description_blocks WHERE entryId = :entryId")
    suspend fun maxSortOrder(entryId: String): Int?

    @Upsert
    suspend fun upsert(block: EntryDescriptionBlockEntity)

    @Query("DELETE FROM entry_description_blocks WHERE id = :id")
    suspend fun deleteById(id: String)
}
