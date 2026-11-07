package com.memoise.ledger.data.repository

import com.memoise.ledger.data.database.EntryDao
import com.memoise.ledger.data.database.EntryActivityDao
import com.memoise.ledger.data.database.EntryActivityEntity
import com.memoise.ledger.data.database.EntryChecklistDao
import com.memoise.ledger.data.database.EntryChecklistEntity
import com.memoise.ledger.data.database.EntryDescriptionBlockDao
import com.memoise.ledger.data.database.EntryDescriptionBlockEntity
import com.memoise.ledger.data.database.EntryNoteDao
import com.memoise.ledger.data.database.EntryNoteEntity
import com.memoise.ledger.data.database.EntryReminderDao
import com.memoise.ledger.data.database.EntryReminderEntity
import com.memoise.ledger.data.database.EntryEntity
import com.memoise.ledger.data.database.EntryRelationshipDao
import com.memoise.ledger.data.database.EntryRelationshipEntity
import com.memoise.ledger.data.database.MemoiseDatabase
import com.memoise.ledger.domain.model.LedgerRelationship
import com.memoise.ledger.domain.model.LedgerActivity
import com.memoise.ledger.domain.model.LedgerChecklistItem
import com.memoise.ledger.domain.model.LedgerDescriptionBlock
import com.memoise.ledger.domain.model.LedgerNote
import com.memoise.ledger.domain.model.LedgerReminder
import com.memoise.ledger.domain.model.LedgerEntry
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.RelationshipType
import com.memoise.ledger.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import java.util.ArrayDeque

interface EntryRepository {
    fun observeEntries(): Flow<List<LedgerEntry>>
    fun observeRelationships(entryId: String): Flow<List<LedgerRelationship>>
    fun observeChecklist(entryId: String): Flow<List<LedgerChecklistItem>>
    fun observeActivities(entryId: String): Flow<List<LedgerActivity>>
    fun observeDescriptionBlocks(entryId: String): Flow<List<LedgerDescriptionBlock>>
    fun observeNotes(entryId: String): Flow<List<LedgerNote>>
    fun observeReminders(entryId: String): Flow<List<LedgerReminder>>
    suspend fun create(entry: LedgerEntry)
    suspend fun save(entry: LedgerEntry)
    suspend fun setStatus(entry: LedgerEntry, status: EntryStatus)
    suspend fun createRelated(
        parentId: String,
        type: RelationshipType,
        entry: LedgerEntry,
        sourceChecklistItemIds: List<String> = emptyList(),
    )
    suspend fun addChecklistItem(entryId: String, title: String)
    suspend fun updateChecklistItem(item: LedgerChecklistItem)
    suspend fun deleteChecklistItem(itemId: String)
    suspend fun addDescriptionBlock(entryId: String, content: String)
    suspend fun updateDescriptionBlock(block: LedgerDescriptionBlock)
    suspend fun deleteDescriptionBlock(blockId: String)
    suspend fun addNote(entryId: String, content: String)
    suspend fun updateNote(note: LedgerNote)
    suspend fun deleteNote(noteId: String)
    suspend fun addReminder(entryId: String, triggerAt: Long)
    suspend fun updateReminder(reminder: LedgerReminder, triggerAt: Long)
    suspend fun setReminderEnabled(reminder: LedgerReminder, enabled: Boolean)
    suspend fun deleteReminder(reminderId: String)
    suspend fun delete(id: String)
}

@Singleton
class RoomEntryRepository @Inject constructor(
    private val database: MemoiseDatabase,
    private val entryDao: EntryDao,
    private val relationshipDao: EntryRelationshipDao,
    private val checklistDao: EntryChecklistDao,
    private val activityDao: EntryActivityDao,
    private val descriptionBlockDao: EntryDescriptionBlockDao,
    private val noteDao: EntryNoteDao,
    private val reminderDao: EntryReminderDao,
    private val reminderScheduler: ReminderScheduler,
) : EntryRepository {
    override fun observeEntries(): Flow<List<LedgerEntry>> =
        entryDao.observeAll().map { entries -> entries.map(EntryEntity::toDomain) }

    override fun observeRelationships(entryId: String): Flow<List<LedgerRelationship>> =
        relationshipDao.observeForEntry(entryId).map { relationships ->
            relationships.map(EntryRelationshipEntity::toDomain)
        }

    override fun observeChecklist(entryId: String): Flow<List<LedgerChecklistItem>> =
        checklistDao.observeForEntry(entryId).map { items -> items.map(EntryChecklistEntity::toDomain) }

    override fun observeActivities(entryId: String): Flow<List<LedgerActivity>> =
        activityDao.observeForEntry(entryId).map { activities -> activities.map(EntryActivityEntity::toDomain) }

    override fun observeDescriptionBlocks(entryId: String): Flow<List<LedgerDescriptionBlock>> =
        descriptionBlockDao.observeForEntry(entryId).map { blocks ->
            blocks.map(EntryDescriptionBlockEntity::toDomain)
        }

    override fun observeNotes(entryId: String): Flow<List<LedgerNote>> =
        noteDao.observeForEntry(entryId).map { notes -> notes.map(EntryNoteEntity::toDomain) }

    override fun observeReminders(entryId: String): Flow<List<LedgerReminder>> =
        reminderDao.observeForEntry(entryId).map { reminders -> reminders.map(EntryReminderEntity::toDomain) }

    override suspend fun create(entry: LedgerEntry) {
        database.withTransaction {
            entryDao.upsert(entry.toEntity())
            recordActivity(entry.id, "Entry created", entry.title, entry.createdAt)
        }
    }

    override suspend fun save(entry: LedgerEntry) {
        entryDao.upsert(entry.toEntity())
        refreshEnabledReminders(entry)
    }

    override suspend fun setStatus(entry: LedgerEntry, status: EntryStatus) {
        if (entry.status == status) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            entryDao.upsert(
                entry.copy(
                    status = status,
                    updatedAt = now,
                    completedAt = if (status == EntryStatus.COMPLETED) now else null,
                ).toEntity(),
            )
            recordActivity(
                entry.id,
                "Status changed",
                "${entry.status.displayName()} → ${status.displayName()}",
                now,
            )
        }
        refreshEnabledReminders(
            entry.copy(
                status = status,
                completedAt = if (status == EntryStatus.COMPLETED) now else null,
            ),
        )
    }

    override suspend fun createRelated(
        parentId: String,
        type: RelationshipType,
        entry: LedgerEntry,
        sourceChecklistItemIds: List<String>,
    ) {
        require(parentId != entry.id) { "An entry cannot be related to itself." }
        database.withTransaction {
            check(entryDao.findId(parentId) != null) { "The source entry no longer exists." }
            val sourceChecklistItems = mutableListOf<EntryChecklistEntity>()
            for (id in sourceChecklistItemIds.distinct()) {
                val item = checklistDao.findById(id)
                check(item != null && item.entryId == parentId) {
                    "The source checklist item no longer exists."
                }
                check(item.linkedEntryId == null) { "This checklist item already has a linked entry." }
                sourceChecklistItems += item
            }
            if (type != RelationshipType.RELATED) ensureNoCycle(parentId, entry.id)
            entryDao.upsert(entry.toEntity())
            relationshipDao.insert(
                EntryRelationshipEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    sourceEntryId = parentId,
                    targetEntryId = entry.id,
                    relationshipType = type,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            if (sourceChecklistItems.isNotEmpty()) {
                val linkedAt = System.currentTimeMillis()
                sourceChecklistItems.forEach { item ->
                    checklistDao.upsert(
                        item.copy(
                            linkedEntryId = entry.id,
                            updatedAt = linkedAt,
                        ),
                    )
                }
                recordActivity(
                    parentId,
                    if (sourceChecklistItems.size == 1) "Checklist item linked" else "Checklist items linked",
                    "${sourceChecklistItems.joinToString { it.title }} → ${entry.title}",
                )
            }
            recordActivity(entry.id, "Entry created", entry.title, entry.createdAt)
            recordActivity(parentId, "${type.activityLabel()} added", entry.title)
        }
    }

    override suspend fun addChecklistItem(entryId: String, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(entryDao.findId(entryId) != null) { "The entry no longer exists." }
            val sortOrder = (checklistDao.maxSortOrder(entryId) ?: -1) + 1
            checklistDao.upsert(
                EntryChecklistEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entryId = entryId,
                    title = cleanTitle,
                    completed = false,
                    sortOrder = sortOrder,
                    linkedEntryId = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            recordActivity(entryId, "Checklist item added", cleanTitle, now)
        }
    }

    override suspend fun updateChecklistItem(item: LedgerChecklistItem) {
        val title = item.title.trim()
        if (title.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = checklistDao.findById(item.id) ?: return@withTransaction
            checklistDao.upsert(item.copy(title = title, updatedAt = now).toEntity())
            if (existing.title != title) {
                recordActivity(item.entryId, "Checklist item edited", title, now)
            }
            if (existing.completed != item.completed) {
                recordActivity(
                    item.entryId,
                    if (item.completed) "Checklist item completed" else "Checklist item reopened",
                    title,
                    now,
                )
            }
        }
    }

    override suspend fun deleteChecklistItem(itemId: String) {
        database.withTransaction {
            val existing = checklistDao.findById(itemId) ?: return@withTransaction
            checklistDao.deleteById(itemId)
            recordActivity(existing.entryId, "Checklist item removed", existing.title)
        }
    }

    override suspend fun addDescriptionBlock(entryId: String, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(entryDao.findId(entryId) != null) { "The entry no longer exists." }
            val sortOrder = (descriptionBlockDao.maxSortOrder(entryId) ?: -1) + 1
            descriptionBlockDao.upsert(
                EntryDescriptionBlockEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entryId = entryId,
                    content = cleanContent,
                    sortOrder = sortOrder,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            recordActivity(entryId, "Description paragraph added", cleanContent, now)
        }
    }

    override suspend fun updateDescriptionBlock(block: LedgerDescriptionBlock) {
        val cleanContent = block.content.trim()
        if (cleanContent.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = descriptionBlockDao.findById(block.id) ?: return@withTransaction
            if (existing.content == cleanContent) return@withTransaction
            descriptionBlockDao.upsert(block.copy(content = cleanContent, updatedAt = now).toEntity())
            recordActivity(block.entryId, "Description paragraph edited", cleanContent, now)
        }
    }

    override suspend fun deleteDescriptionBlock(blockId: String) {
        database.withTransaction {
            val existing = descriptionBlockDao.findById(blockId) ?: return@withTransaction
            descriptionBlockDao.deleteById(blockId)
            recordActivity(existing.entryId, "Description paragraph removed", existing.content)
        }
    }

    override suspend fun addNote(entryId: String, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(entryDao.findId(entryId) != null) { "The entry no longer exists." }
            noteDao.upsert(
                EntryNoteEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entryId = entryId,
                    content = cleanContent,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            recordActivity(entryId, "Note added", cleanContent.activityPreview(), now)
        }
    }

    override suspend fun updateNote(note: LedgerNote) {
        val cleanContent = note.content.trim()
        if (cleanContent.isEmpty()) return
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = noteDao.findById(note.id) ?: return@withTransaction
            check(existing.entryId == note.entryId) { "The note belongs to another entry." }
            if (existing.content == cleanContent) return@withTransaction
            noteDao.upsert(note.copy(content = cleanContent, updatedAt = now).toEntity())
            recordActivity(note.entryId, "Note edited", cleanContent.activityPreview(), now)
        }
    }

    override suspend fun deleteNote(noteId: String) {
        database.withTransaction {
            val existing = noteDao.findById(noteId) ?: return@withTransaction
            noteDao.deleteById(noteId)
            recordActivity(existing.entryId, "Note removed", existing.content.activityPreview())
        }
    }

    override suspend fun addReminder(entryId: String, triggerAt: Long) {
        require(triggerAt > System.currentTimeMillis()) { "Choose a reminder time in the future." }
        val entry = entryDao.findById(entryId) ?: return
        check(entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
            "Completed or cancelled entries cannot have active reminders."
        }
        val now = System.currentTimeMillis()
        val reminder = EntryReminderEntity(
            id = java.util.UUID.randomUUID().toString(),
            entryId = entryId,
            triggerAt = triggerAt,
            enabled = true,
            createdAt = now,
            updatedAt = now,
        )
        database.withTransaction {
            reminderDao.upsert(reminder)
            recordActivity(entryId, "Reminder added", formatReminderTime(triggerAt), now)
        }
        reminderScheduler.schedule(reminder.id, triggerAt)
    }

    override suspend fun updateReminder(reminder: LedgerReminder, triggerAt: Long) {
        require(triggerAt > System.currentTimeMillis()) { "Choose a reminder time in the future." }
        val now = System.currentTimeMillis()
        val updated = database.withTransaction {
            val existing = reminderDao.findById(reminder.id) ?: return@withTransaction null
            check(existing.entryId == reminder.entryId) { "The reminder belongs to another entry." }
            val entry = entryDao.findById(existing.entryId)
            check(entry != null && entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
                "Completed or cancelled entries cannot have active reminders."
            }
            val replacement = existing.copy(triggerAt = triggerAt, enabled = true, updatedAt = now)
            reminderDao.upsert(replacement)
            recordActivity(reminder.entryId, "Reminder changed", formatReminderTime(triggerAt), now)
            replacement
        } ?: return
        reminderScheduler.schedule(updated.id, updated.triggerAt)
    }

    override suspend fun setReminderEnabled(reminder: LedgerReminder, enabled: Boolean) {
        val now = System.currentTimeMillis()
        val updated = database.withTransaction {
            val existing = reminderDao.findById(reminder.id) ?: return@withTransaction null
            check(existing.entryId == reminder.entryId) { "The reminder belongs to another entry." }
            if (enabled) {
                require(existing.triggerAt > now) { "This reminder time has passed." }
                val entry = entryDao.findById(existing.entryId)
                check(entry != null && entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
                    "Completed or cancelled entries cannot have active reminders."
                }
            }
            val replacement = existing.copy(enabled = enabled, updatedAt = now)
            reminderDao.upsert(replacement)
            recordActivity(
                reminder.entryId,
                if (enabled) "Reminder enabled" else "Reminder disabled",
                formatReminderTime(existing.triggerAt),
                now,
            )
            replacement
        } ?: return
        if (updated.enabled) reminderScheduler.schedule(updated.id, updated.triggerAt)
        else reminderScheduler.cancel(updated.id)
    }

    override suspend fun deleteReminder(reminderId: String) {
        database.withTransaction {
            val existing = reminderDao.findById(reminderId) ?: return@withTransaction
            reminderDao.deleteById(reminderId)
            recordActivity(existing.entryId, "Reminder removed", formatReminderTime(existing.triggerAt))
        }
        reminderScheduler.cancel(reminderId)
    }

    override suspend fun delete(id: String) {
        reminderDao.allForEntry(id).forEach { reminderScheduler.cancel(it.id) }
        entryDao.deleteById(id)
    }

    private suspend fun refreshEnabledReminders(entry: LedgerEntry) {
        val reminders = reminderDao.enabledForEntry(entry.id)
        if (entry.status == EntryStatus.COMPLETED || entry.status == EntryStatus.CANCELLED) {
            val now = System.currentTimeMillis()
            database.withTransaction {
                reminders.forEach { reminderDao.upsert(it.copy(enabled = false, updatedAt = now)) }
            }
            reminders.forEach { reminderScheduler.cancel(it.id) }
        } else {
            reminders.forEach { reminderScheduler.schedule(it.id, it.triggerAt) }
        }
    }

    private suspend fun ensureNoCycle(parentId: String, childId: String) {
        val visited = mutableSetOf<String>()
        val pending = ArrayDeque<String>()
        pending.add(childId)
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (current == parentId) {
                throw IllegalArgumentException("This relationship would create a circular chain.")
            }
            if (visited.add(current)) pending.addAll(relationshipDao.directedChildren(current))
        }
    }

    private suspend fun recordActivity(
        entryId: String,
        action: String,
        details: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        activityDao.insert(
            EntryActivityEntity(
                id = java.util.UUID.randomUUID().toString(),
                entryId = entryId,
                action = action,
                details = details,
                occurredAt = occurredAt,
            ),
        )
    }
}

private fun EntryEntity.toDomain() = LedgerEntry(
    id = id,
    title = title,
    type = type,
    status = status,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    descriptiveEndDate = descriptiveEndDate,
    datePurpose = datePurpose,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    recurrenceSeriesId = recurrenceSeriesId,
    sourceEntryId = sourceEntryId,
    sourceContentId = sourceContentId,
)

private fun LedgerEntry.toEntity() = EntryEntity(
    id = id,
    title = title,
    type = type,
    status = status,
    scheduledDate = scheduledDate,
    scheduledTime = scheduledTime,
    descriptiveEndDate = descriptiveEndDate,
    datePurpose = datePurpose,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    recurrenceSeriesId = recurrenceSeriesId,
    sourceEntryId = sourceEntryId,
    sourceContentId = sourceContentId,
)

private fun EntryRelationshipEntity.toDomain() = LedgerRelationship(
    id = id,
    sourceEntryId = sourceEntryId,
    targetEntryId = targetEntryId,
    type = relationshipType,
    createdAt = createdAt,
)

private fun EntryChecklistEntity.toDomain() = LedgerChecklistItem(
    id = id,
    entryId = entryId,
    title = title,
    completed = completed,
    sortOrder = sortOrder,
    linkedEntryId = linkedEntryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun EntryDescriptionBlockEntity.toDomain() = LedgerDescriptionBlock(
    id = id,
    entryId = entryId,
    content = content,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun EntryNoteEntity.toDomain() = LedgerNote(
    id = id,
    entryId = entryId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun EntryReminderEntity.toDomain() = LedgerReminder(
    id = id,
    entryId = entryId,
    triggerAt = triggerAt,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun LedgerChecklistItem.toEntity() = EntryChecklistEntity(
    id = id,
    entryId = entryId,
    title = title,
    completed = completed,
    sortOrder = sortOrder,
    linkedEntryId = linkedEntryId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun LedgerDescriptionBlock.toEntity() = EntryDescriptionBlockEntity(
    id = id,
    entryId = entryId,
    content = content,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun LedgerNote.toEntity() = EntryNoteEntity(
    id = id,
    entryId = entryId,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun LedgerReminder.toEntity() = EntryReminderEntity(
    id = id,
    entryId = entryId,
    triggerAt = triggerAt,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun EntryActivityEntity.toDomain() = LedgerActivity(
    id = id,
    entryId = entryId,
    action = action,
    details = details,
    occurredAt = occurredAt,
)

private fun EntryStatus.displayName(): String = when (this) {
    EntryStatus.PENDING -> "Pending"
    EntryStatus.IN_PROGRESS -> "In progress"
    EntryStatus.COMPLETED -> "Completed"
    EntryStatus.CANCELLED -> "Cancelled"
}

private fun RelationshipType.activityLabel(): String = when (this) {
    RelationshipType.PREPARATION -> "Preparation"
    RelationshipType.FOLLOW_UP -> "Follow-up"
    RelationshipType.RELATED -> "Related entry"
}

private fun String.activityPreview(): String =
    if (length <= 80) this else "${take(80)}…"

private fun formatReminderTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a"))
