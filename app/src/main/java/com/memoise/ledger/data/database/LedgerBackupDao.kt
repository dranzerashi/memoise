package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface LedgerBackupDao {
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
