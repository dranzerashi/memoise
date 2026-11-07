package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurrence_series")
data class RecurrenceSeriesEntity(
    @PrimaryKey val id: String,
    val ruleJson: String,
    val startsOn: Long,
    val endsOn: Long?,
    val timeZoneId: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
