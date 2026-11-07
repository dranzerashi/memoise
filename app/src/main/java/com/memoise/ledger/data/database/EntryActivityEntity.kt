package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_activities",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["entryId", "occurredAt"])],
)
data class EntryActivityEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val action: String,
    val details: String?,
    val occurredAt: Long,
)
