package com.memoise.ledger.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "ledger_search_fts")
data class EntrySearchFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val entryId: String,
    val title: String,
    val description: String,
    val notes: String,
    val checklist: String,
    val relatedTitles: String,
)
