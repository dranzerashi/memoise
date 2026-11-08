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

    @Query(
        """SELECT reminder.* FROM entry_reminders AS reminder
            JOIN ledger_entries AS entry ON entry.id = reminder.entryId
            WHERE reminder.enabled = 1
              AND reminder.triggerAt > :now
              AND entry.status NOT IN ('COMPLETED', 'CANCELLED')""",
    )
    suspend fun enabledUpcoming(now: Long): List<EntryReminderEntity>

    @Query("SELECT * FROM entry_reminders WHERE entryId = :entryId")
    suspend fun allForEntry(entryId: String): List<EntryReminderEntity>

    @Query("SELECT * FROM entry_reminders WHERE parentReminderId = :parentReminderId")
    suspend fun childrenOf(parentReminderId: String): List<EntryReminderEntity>

    @Query("DELETE FROM entry_reminders WHERE parentReminderId = :parentReminderId")
    suspend fun deleteChildrenOf(parentReminderId: String)

    @Upsert
    suspend fun upsert(reminder: EntryReminderEntity)

    @Query("DELETE FROM entry_reminders WHERE id = :id")
    suspend fun deleteById(id: String)
}
