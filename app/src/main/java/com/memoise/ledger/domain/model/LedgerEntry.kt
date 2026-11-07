package com.memoise.ledger.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class EntryType {
    TASK,
    EVENT,
    OCCASION,
    NOTE,
}

enum class EntryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class DatePurpose {
    EVENT,
    DEADLINE,
    FLEXIBLE_TASK,
}

enum class RelationshipType {
    PREPARATION,
    FOLLOW_UP,
    RELATED,
}

data class LedgerRelationship(
    val id: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val type: RelationshipType,
    val createdAt: Long,
)

data class LedgerChecklistItem(
    val id: String,
    val entryId: String,
    val title: String,
    val completed: Boolean,
    val sortOrder: Int,
    val linkedEntryId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LedgerActivity(
    val id: String,
    val entryId: String,
    val action: String,
    val details: String?,
    val occurredAt: Long,
)

data class LedgerDescriptionBlock(
    val id: String,
    val entryId: String,
    val content: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LedgerNote(
    val id: String,
    val entryId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LedgerEntry(
    val id: String,
    val title: String,
    val type: EntryType? = null,
    val status: EntryStatus = EntryStatus.PENDING,
    val scheduledDate: LocalDate? = null,
    val scheduledTime: LocalTime? = null,
    val descriptiveEndDate: LocalDate? = null,
    val datePurpose: DatePurpose = DatePurpose.FLEXIBLE_TASK,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val recurrenceSeriesId: String? = null,
    val sourceEntryId: String? = null,
    val sourceContentId: String? = null,
)
