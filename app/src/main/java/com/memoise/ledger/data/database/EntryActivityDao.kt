package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryActivityDao {
    @Query("SELECT * FROM entry_activities WHERE entryId = :entryId ORDER BY occurredAt DESC")
    fun observeForEntry(entryId: String): Flow<List<EntryActivityEntity>>

    @Insert
    suspend fun insert(activity: EntryActivityEntity)
}
