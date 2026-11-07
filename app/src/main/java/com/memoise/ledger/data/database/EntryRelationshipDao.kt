package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRelationshipDao {
    @Query(
        "SELECT * FROM entry_relationships " +
            "WHERE sourceEntryId = :entryId OR targetEntryId = :entryId ORDER BY createdAt ASC",
    )
    fun observeForEntry(entryId: String): Flow<List<EntryRelationshipEntity>>

    @Query(
        "SELECT targetEntryId FROM entry_relationships " +
            "WHERE sourceEntryId = :entryId AND relationshipType IN ('PREPARATION', 'FOLLOW_UP', 'RECURRENCE')",
    )
    suspend fun directedChildren(entryId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(relationship: EntryRelationshipEntity)
}
