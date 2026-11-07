package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedEntryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["entryId", "sortOrder"]),
        Index(value = ["linkedEntryId"]),
    ],
)
data class EntryChecklistEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val title: String,
    val completed: Boolean,
    val sortOrder: Int,
    val linkedEntryId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
