package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurrence_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceSeriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["recurrenceSeriesId", "occurrenceDate"], unique = true),
        Index(value = ["occurrenceEntryId"]),
    ],
)
data class RecurrenceExceptionEntity(
    @PrimaryKey val id: String,
    val recurrenceSeriesId: String,
    val occurrenceDate: Long,
    val exceptionType: String,
    val modifiedDate: Long?,
    val occurrenceEntryId: String?,
    val createdAt: Long,
)

enum class RecurrenceExceptionType {
    SKIPPED,
    MOVED,
}
