package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryNoteDao {
    @Query("SELECT * FROM entry_notes WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeForEntry(entryId: String): Flow<List<EntryNoteEntity>>

    @Query("SELECT * FROM entry_notes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EntryNoteEntity?

    @Upsert
    suspend fun upsert(note: EntryNoteEntity)

    @Query("DELETE FROM entry_notes WHERE id = :id")
    suspend fun deleteById(id: String)
}
