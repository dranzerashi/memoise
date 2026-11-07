package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_reminders",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["entryId", "triggerAt"])],
)
data class EntryReminderEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val triggerAt: Long,
    val enabled: Boolean,
    val relativeOffsetDays: Int?,
    val relativeMinuteOfDay: Int?,
    val parentReminderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
