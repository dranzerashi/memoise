package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LedgerBackupDao {
    @Query("DELETE FROM entry_relationships")
    suspend fun clearRelationships()

    @Query("DELETE FROM entry_description_blocks")
    suspend fun clearDescriptionBlocks()

    @Query("DELETE FROM entry_checklist_items")
    suspend fun clearChecklistItems()

    @Query("DELETE FROM entry_notes")
    suspend fun clearNotes()

    @Query("DELETE FROM entry_activities")
    suspend fun clearActivities()

    @Query("DELETE FROM entry_reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM recurrence_series")
    suspend fun clearRecurrenceSeries()

    @Query("DELETE FROM ledger_entries")
    suspend fun clearEntries()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(items: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRelationships(items: List<EntryRelationshipEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDescriptionBlocks(items: List<EntryDescriptionBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChecklistItems(items: List<EntryChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotes(items: List<EntryNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActivities(items: List<EntryActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReminders(items: List<EntryReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecurrenceSeries(items: List<RecurrenceSeriesEntity>)

    @Query("SELECT * FROM ledger_entries ORDER BY createdAt, id")
    suspend fun entries(): List<EntryEntity>

    @Query("SELECT * FROM entry_relationships ORDER BY createdAt, id")
    suspend fun relationships(): List<EntryRelationshipEntity>

    @Query("SELECT * FROM entry_description_blocks ORDER BY entryId, sortOrder")
    suspend fun descriptionBlocks(): List<EntryDescriptionBlockEntity>

    @Query("SELECT * FROM entry_checklist_items ORDER BY entryId, sortOrder")
    suspend fun checklistItems(): List<EntryChecklistEntity>

    @Query("SELECT * FROM entry_notes ORDER BY entryId, createdAt")
    suspend fun notes(): List<EntryNoteEntity>

    @Query("SELECT * FROM entry_activities ORDER BY entryId, occurredAt")
    suspend fun activities(): List<EntryActivityEntity>

    @Query("SELECT * FROM entry_reminders ORDER BY entryId, triggerAt")
    suspend fun reminders(): List<EntryReminderEntity>

    @Query("SELECT * FROM recurrence_series ORDER BY createdAt, id")
    suspend fun recurrenceSeries(): List<RecurrenceSeriesEntity>
}
