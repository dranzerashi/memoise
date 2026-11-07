package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryReminderDao {
    @Query("SELECT * FROM entry_reminders WHERE entryId = :entryId ORDER BY triggerAt ASC")
    fun observeForEntry(entryId: String): Flow<List<EntryReminderEntity>>

    @Query("SELECT * FROM entry_reminders WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EntryReminderEntity?

    @Query("SELECT * FROM entry_reminders WHERE entryId = :entryId AND enabled = 1")
    suspend fun enabledForEntry(entryId: String): List<EntryReminderEntity>

    @Query("SELECT * FROM entry_reminders WHERE entryId = :entryId")
    suspend fun allForEntry(entryId: String): List<EntryReminderEntity>

    @Upsert
    suspend fun upsert(reminder: EntryReminderEntity)

    @Query("DELETE FROM entry_reminders WHERE id = :id")
    suspend fun deleteById(id: String)
}
