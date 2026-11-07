package com.memoise.ledger.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.memoise.ledger.domain.model.RelationshipType

@Entity(
    tableName = "entry_relationships",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sourceEntryId"]),
        Index(value = ["targetEntryId"]),
        Index(value = ["sourceEntryId", "targetEntryId", "relationshipType"], unique = true),
    ],
)
data class EntryRelationshipEntity(
    @PrimaryKey val id: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val relationshipType: RelationshipType,
    val createdAt: Long,
)
