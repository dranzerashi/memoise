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
import com.memoise.ledger.data.database.RecurrenceSeriesDao
import com.memoise.ledger.data.database.RecurrenceSeriesEntity
import com.memoise.ledger.domain.model.LedgerRelationship
import com.memoise.ledger.domain.model.LedgerActivity
import com.memoise.ledger.domain.model.LedgerChecklistItem
import com.memoise.ledger.domain.model.LedgerDescriptionBlock
import com.memoise.ledger.domain.model.LedgerNote
import com.memoise.ledger.domain.model.LedgerReminder
import com.memoise.ledger.domain.model.LedgerEntry
import com.memoise.ledger.domain.model.LedgerRecurrenceSeries
import com.memoise.ledger.domain.model.RecurrenceFrequency
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.RelationshipType
import com.memoise.ledger.domain.model.EntryType
import com.memoise.ledger.domain.model.LedgerSearchMatch
import com.memoise.ledger.domain.model.SearchDateFilter
import com.memoise.ledger.domain.model.SearchRelationshipFilter
import com.memoise.ledger.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import java.util.ArrayDeque
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.json.JSONObject

interface EntryRepository {
    fun observeEntries(): Flow<List<LedgerEntry>>
    fun searchEntries(
        query: String,
        status: EntryStatus?,
        type: EntryType?,
        dateFilter: SearchDateFilter,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        relationshipFilter: SearchRelationshipFilter,
    ): Flow<List<LedgerSearchMatch>>
    fun observeRelationships(entryId: String): Flow<List<LedgerRelationship>>
    fun observeChecklist(entryId: String): Flow<List<LedgerChecklistItem>>
    fun observeActivities(entryId: String): Flow<List<LedgerActivity>>
    fun observeDescriptionBlocks(entryId: String): Flow<List<LedgerDescriptionBlock>>
    fun observeNotes(entryId: String): Flow<List<LedgerNote>>
    fun observeReminders(entryId: String): Flow<List<LedgerReminder>>
    fun observeRecurrence(seriesId: String): Flow<LedgerRecurrenceSeries?>
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
    suspend fun addReminder(
        entryId: String,
        triggerAt: Long,
        relativeOffsetDays: Int?,
        relativeMinuteOfDay: Int?,
    )
    suspend fun updateReminder(
        reminder: LedgerReminder,
        triggerAt: Long,
        relativeOffsetDays: Int?,
        relativeMinuteOfDay: Int?,
    )
    suspend fun setReminderEnabled(reminder: LedgerReminder, enabled: Boolean)
    suspend fun deleteReminder(reminderId: String)
    suspend fun createRecurrence(entryId: String, frequency: RecurrenceFrequency)
    suspend fun stopRecurrence(entryId: String)
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
    private val recurrenceDao: RecurrenceSeriesDao,
    private val reminderScheduler: ReminderScheduler,
) : EntryRepository {
    override fun searchEntries(
        query: String,
        status: EntryStatus?,
        type: EntryType?,
        dateFilter: SearchDateFilter,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        relationshipFilter: SearchRelationshipFilter,
    ): Flow<List<LedgerSearchMatch>> {
        val tokens = query.trim()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotBlank)
            .distinct()
        if (tokens.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        val matchQuery = tokens.joinToString(" AND ") { token -> "\"$token\"*" }
        return database.entrySearchDao().search(
            matchQuery = matchQuery,
            rawQuery = query.trim(),
            status = status?.name,
            type = type?.name,
            dateFilter = dateFilter.name,
            dateFrom = dateFrom?.toEpochDay(),
            dateTo = dateTo?.toEpochDay(),
            relationshipFilter = relationshipFilter.name,
            todayEpochDay = LocalDate.now().toEpochDay(),
        ).map { matches -> matches.map { LedgerSearchMatch(it.entryId, it.excerpt) } }
    }

    override fun observeEntries(): Flow<List<LedgerEntry>> = entryDao.observeAll()
        .onStart { maintainRecurrenceWindow() }
        .map { entries -> entries.map(EntryEntity::toDomain) }

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

    override fun observeRecurrence(seriesId: String): Flow<LedgerRecurrenceSeries?> =
        recurrenceDao.observeById(seriesId).map { it?.toDomainOrNull() }

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

    override suspend fun addReminder(
        entryId: String,
        triggerAt: Long,
        relativeOffsetDays: Int?,
        relativeMinuteOfDay: Int?,
    ) {
        val entry = entryDao.findById(entryId) ?: return
        require(
            triggerAt > System.currentTimeMillis() ||
                (relativeOffsetDays != null && isActiveRecurrenceTemplate(entry)),
        ) { "Choose a reminder time in the future." }
        check(entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
            "Completed or cancelled entries cannot have active reminders."
        }
        val now = System.currentTimeMillis()
        val reminder = EntryReminderEntity(
            id = java.util.UUID.randomUUID().toString(),
            entryId = entryId,
            triggerAt = triggerAt,
            enabled = true,
            relativeOffsetDays = relativeOffsetDays,
            relativeMinuteOfDay = relativeMinuteOfDay,
            parentReminderId = null,
            createdAt = now,
            updatedAt = now,
        )
        database.withTransaction {
            reminderDao.upsert(reminder)
            recordActivity(entryId, "Reminder added", formatReminderTime(triggerAt), now)
        }
        if (triggerAt > System.currentTimeMillis()) reminderScheduler.schedule(reminder.id, triggerAt)
        else reminderScheduler.cancel(reminder.id)
        val persistedEntry = entryDao.findById(entryId) ?: return
        syncInheritedReminderIfTemplate(persistedEntry, reminder)
    }

    override suspend fun updateReminder(
        reminder: LedgerReminder,
        triggerAt: Long,
        relativeOffsetDays: Int?,
        relativeMinuteOfDay: Int?,
    ) {
        val now = System.currentTimeMillis()
        val updated = database.withTransaction {
            val existing = reminderDao.findById(reminder.id) ?: return@withTransaction null
            check(existing.entryId == reminder.entryId) { "The reminder belongs to another entry." }
            val entry = entryDao.findById(existing.entryId)
            check(entry != null && entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
                "Completed or cancelled entries cannot have active reminders."
            }
            require(
                triggerAt > now ||
                    (relativeOffsetDays != null && isActiveRecurrenceTemplate(entry)),
            ) { "Choose a reminder time in the future." }
            val replacement = existing.copy(
                triggerAt = triggerAt,
                enabled = true,
                relativeOffsetDays = relativeOffsetDays,
                relativeMinuteOfDay = relativeMinuteOfDay,
                parentReminderId = null,
                updatedAt = now,
            )
            reminderDao.upsert(replacement)
            recordActivity(reminder.entryId, "Reminder changed", formatReminderTime(triggerAt), now)
            replacement
        } ?: return
        if (updated.triggerAt > System.currentTimeMillis()) reminderScheduler.schedule(updated.id, updated.triggerAt)
        else reminderScheduler.cancel(updated.id)
        val entry = entryDao.findById(updated.entryId) ?: return
        syncInheritedReminderIfTemplate(entry, updated)
    }

    override suspend fun setReminderEnabled(reminder: LedgerReminder, enabled: Boolean) {
        val now = System.currentTimeMillis()
        val updated = database.withTransaction {
            val existing = reminderDao.findById(reminder.id) ?: return@withTransaction null
            check(existing.entryId == reminder.entryId) { "The reminder belongs to another entry." }
            if (enabled) {
                val entry = entryDao.findById(existing.entryId)
                check(entry != null && entry.status != EntryStatus.COMPLETED && entry.status != EntryStatus.CANCELLED) {
                    "Completed or cancelled entries cannot have active reminders."
                }
                require(
                    existing.triggerAt > now ||
                        (existing.relativeOffsetDays != null && isActiveRecurrenceTemplate(entry)),
                ) { "This reminder time has passed." }
            }
            val replacement = existing.copy(
                enabled = enabled,
                parentReminderId = null,
                updatedAt = now,
            )
            reminderDao.upsert(replacement)
            recordActivity(
                reminder.entryId,
                if (enabled) "Reminder enabled" else "Reminder disabled",
                formatReminderTime(existing.triggerAt),
                now,
            )
            replacement
        } ?: return
        if (updated.enabled && updated.triggerAt > now) reminderScheduler.schedule(updated.id, updated.triggerAt)
        else reminderScheduler.cancel(updated.id)
        val entry = entryDao.findById(updated.entryId) ?: return
        syncInheritedReminderIfTemplate(entry, updated)
    }

    override suspend fun deleteReminder(reminderId: String) {
        val removed = reminderDao.findById(reminderId) ?: return
        val inherited = reminderDao.childrenOf(reminderId)
        database.withTransaction {
            reminderDao.deleteById(reminderId)
            reminderDao.deleteChildrenOf(reminderId)
            recordActivity(removed.entryId, "Reminder removed", formatReminderTime(removed.triggerAt))
        }
        reminderScheduler.cancel(reminderId)
        inherited.forEach { reminderScheduler.cancel(it.id) }
    }

    override suspend fun createRecurrence(entryId: String, frequency: RecurrenceFrequency) {
        val now = System.currentTimeMillis()
        val series = database.withTransaction {
            val entry = entryDao.findById(entryId) ?: return@withTransaction null
            val startsOn = checkNotNull(entry.scheduledDate) {
                "Add a date before setting a recurrence."
            }
            check(entry.recurrenceSeriesId == null) { "This entry is already part of a recurrence series." }
            val series = RecurrenceSeriesEntity(
                id = java.util.UUID.randomUUID().toString(),
                ruleJson = JSONObject()
                    .put("version", 1)
                    .put("frequency", frequency.name)
                    .put("interval", 1)
                    .toString(),
                startsOn = startsOn.toEpochDay(),
                endsOn = null,
                timeZoneId = ZoneId.systemDefault().id,
                active = true,
                createdAt = now,
                updatedAt = now,
            )
            recurrenceDao.upsert(series)
            entryDao.upsert(entry.copy(recurrenceSeriesId = series.id, updatedAt = now))
            recordActivity(entryId, "Recurrence started", frequency.label, now)
            series
        } ?: return
        generateOccurrences(series)
    }

    override suspend fun stopRecurrence(entryId: String) {
        database.withTransaction {
            val entry = entryDao.findById(entryId) ?: return@withTransaction
            val seriesId = entry.recurrenceSeriesId ?: return@withTransaction
            val series = recurrenceDao.findById(seriesId) ?: return@withTransaction
            if (!series.active) return@withTransaction
            recurrenceDao.upsert(series.copy(active = false, updatedAt = System.currentTimeMillis()))
            recordActivity(entryId, "Recurrence stopped")
        }
    }

    override suspend fun delete(id: String) {
        val reminders = reminderDao.allForEntry(id)
        database.withTransaction {
            val entry = entryDao.findById(id) ?: return@withTransaction
            val seriesId = entry.recurrenceSeriesId
            val series = if (seriesId == null) null else recurrenceDao.findById(seriesId)
            val members = if (series == null) emptyList() else entryDao.findByRecurrenceSeries(series.id)
            val isSource = series != null && members.minByOrNull { it.createdAt }?.id == id
            val replacementSource = if (isSource) members
                .filterNot { it.id == id }
                .minByOrNull { it.createdAt } else null
            entryDao.deleteById(id)
            if (series != null && isSource) {
                reminders.forEach { parentReminder ->
                    reminderDao.childrenOf(parentReminder.id).forEach { inherited ->
                        reminderDao.upsert(inherited.copy(parentReminderId = null))
                    }
                }
                if (replacementSource == null) {
                    recurrenceDao.upsert(series.copy(active = false, updatedAt = System.currentTimeMillis()))
                } else {
                    members.filterNot { it.id == id || it.id == replacementSource.id }.forEach { occurrence ->
                        relationshipDao.insert(
                            EntryRelationshipEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                sourceEntryId = replacementSource.id,
                                targetEntryId = occurrence.id,
                                relationshipType = RelationshipType.RECURRENCE,
                                createdAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            }
        }
        reminders.forEach { reminderScheduler.cancel(it.id) }
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
            reminders.forEach { reminder ->
                if (reminder.relativeOffsetDays == null) {
                    reminderScheduler.schedule(reminder.id, reminder.triggerAt)
                    syncInheritedReminderIfTemplate(entry.toEntity(), reminder)
                } else if (entry.scheduledDate == null) {
                    reminderScheduler.cancel(reminder.id)
                } else {
                    val minute = reminder.relativeMinuteOfDay ?: 9 * 60
                    val triggerAt = relativeTriggerAt(entry.scheduledDate, reminder.relativeOffsetDays, minute)
                    val updated = reminder.copy(triggerAt = triggerAt, relativeMinuteOfDay = minute)
                    reminderDao.upsert(updated)
                    if (triggerAt > System.currentTimeMillis()) reminderScheduler.schedule(updated.id, triggerAt)
                    else reminderScheduler.cancel(updated.id)
                    syncInheritedReminderIfTemplate(entry.toEntity(), updated)
                }
            }
        }
    }

    private suspend fun maintainRecurrenceWindow() {
        for (series in recurrenceDao.findActive()) generateOccurrences(series)
    }

    private suspend fun syncInheritedReminderIfTemplate(
        entry: EntryEntity,
        sourceReminder: EntryReminderEntity,
    ) {
        val seriesId = entry.recurrenceSeriesId ?: return
        val members = entryDao.findByRecurrenceSeries(seriesId)
        val template = members.minByOrNull { it.createdAt } ?: return
        if (template.id != entry.id) return
        val templateDate = template.scheduledDate ?: return
        val series = recurrenceDao.findById(seriesId) ?: return
        if (!series.active) return
        val zone = ZoneId.of(series.timeZoneId)
        val sourceTime = java.time.Instant.ofEpochMilli(sourceReminder.triggerAt)
            .atZone(zone)
            .toLocalDateTime()
        val offsetDays = sourceReminder.relativeOffsetDays
            ?: ChronoUnit.DAYS.between(sourceTime.toLocalDate(), templateDate).toInt()
        val minuteOfDay = sourceReminder.relativeMinuteOfDay
            ?: sourceTime.hour * 60 + sourceTime.minute
        val inheritedByEntry = reminderDao.childrenOf(sourceReminder.id).associateBy { it.entryId }
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val replacements = members.asSequence()
            .filter { it.id != template.id }
            .filter { it.scheduledDate?.isAfter(today) == true }
            .map { occurrence ->
                val current = inheritedByEntry[occurrence.id]
                EntryReminderEntity(
                    id = current?.id ?: java.util.UUID.randomUUID().toString(),
                    entryId = occurrence.id,
                    triggerAt = relativeTriggerAt(
                        occurrence.scheduledDate ?: return@map null,
                        offsetDays,
                        minuteOfDay,
                        zone,
                    ),
                    enabled = sourceReminder.enabled,
                    relativeOffsetDays = offsetDays,
                    relativeMinuteOfDay = minuteOfDay,
                    parentReminderId = sourceReminder.id,
                    createdAt = current?.createdAt ?: now,
                    updatedAt = now,
                )
            }
            .filterNotNull()
            .toList()
        database.withTransaction { replacements.forEach { reminderDao.upsert(it) } }
        replacements.forEach { reminder ->
            if (reminder.enabled && reminder.triggerAt > now) {
                reminderScheduler.schedule(reminder.id, reminder.triggerAt)
            } else {
                reminderScheduler.cancel(reminder.id)
            }
        }
    }

    private suspend fun isActiveRecurrenceTemplate(entry: EntryEntity): Boolean {
        val seriesId = entry.recurrenceSeriesId ?: return false
        val series = recurrenceDao.findById(seriesId) ?: return false
        return series.active && entryDao.findByRecurrenceSeries(seriesId)
            .minByOrNull { it.createdAt }
            ?.id == entry.id
    }

    private suspend fun generateOccurrences(series: RecurrenceSeriesEntity) {
        if (!series.active) return
        val rule = runCatching { JSONObject(series.ruleJson) }.getOrNull() ?: return
        if (rule.optInt("version", 0) != 1 || rule.optInt("interval", 0) != 1) return
        val frequency = runCatching {
            RecurrenceFrequency.valueOf(rule.optString("frequency"))
        }.getOrNull() ?: return
        val start = LocalDate.ofEpochDay(series.startsOn)
        val end = series.endsOn?.let(LocalDate::ofEpochDay)
        val today = LocalDate.now(ZoneId.of(series.timeZoneId))
        val horizon = today.plusMonths(12)
        val existing = entryDao.findByRecurrenceSeries(series.id)
        val existingDates = existing.mapNotNull { it.scheduledDate?.toEpochDay() }.toHashSet()
        val template = existing.minByOrNull { it.createdAt } ?: return
        val templateDate = template.scheduledDate ?: return
        val inheritedReminders = reminderDao.allForEntry(template.id)
        val now = System.currentTimeMillis()
        val additions = mutableListOf<EntryEntity>()
        val reminderAdditions = mutableListOf<EntryReminderEntity>()
        var index = 1L
        while (index <= 100_000) {
            val date = when (frequency) {
                RecurrenceFrequency.DAILY -> start.plusDays(index)
                RecurrenceFrequency.WEEKLY -> start.plusWeeks(index)
                RecurrenceFrequency.MONTHLY -> start.plusMonths(index)
                RecurrenceFrequency.YEARLY -> start.plusYears(index)
            }
            if (date.isAfter(horizon) || (end != null && date.isAfter(end))) break
            val day = date.toEpochDay()
            if (date.isAfter(today) && day !in existingDates) {
                val occurrence = template.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    status = EntryStatus.PENDING,
                    scheduledDate = date,
                    createdAt = now,
                    updatedAt = now,
                    completedAt = null,
                    recurrenceSeriesId = series.id,
                    sourceEntryId = null,
                    sourceContentId = null,
                )
                additions += occurrence
                inheritedReminders.forEach { sourceReminder ->
                    val sourceDateTime = java.time.Instant.ofEpochMilli(sourceReminder.triggerAt)
                        .atZone(ZoneId.of(series.timeZoneId))
                        .toLocalDateTime()
                    val offsetDays = sourceReminder.relativeOffsetDays
                        ?: ChronoUnit.DAYS.between(sourceDateTime.toLocalDate(), templateDate).toInt()
                    val minuteOfDay = sourceReminder.relativeMinuteOfDay
                        ?: sourceDateTime.hour * 60 + sourceDateTime.minute
                    reminderAdditions += EntryReminderEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        entryId = occurrence.id,
                        triggerAt = relativeTriggerAt(
                            date,
                            offsetDays,
                            minuteOfDay,
                            ZoneId.of(series.timeZoneId),
                        ),
                        enabled = sourceReminder.enabled,
                        relativeOffsetDays = offsetDays,
                        relativeMinuteOfDay = minuteOfDay,
                        parentReminderId = sourceReminder.parentReminderId ?: sourceReminder.id,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }
            index++
        }
        if (additions.isNotEmpty()) {
            database.withTransaction {
                additions.forEach { occurrence ->
                    entryDao.upsert(occurrence)
                    relationshipDao.insert(
                        EntryRelationshipEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            sourceEntryId = template.id,
                            targetEntryId = occurrence.id,
                            relationshipType = RelationshipType.RECURRENCE,
                            createdAt = now,
                        ),
                    )
                }
                reminderAdditions.forEach { reminderDao.upsert(it) }
            }
            reminderAdditions.filter { it.enabled && it.triggerAt > System.currentTimeMillis() }
                .forEach { reminderScheduler.schedule(it.id, it.triggerAt) }
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

private fun RecurrenceSeriesEntity.toDomainOrNull(): LedgerRecurrenceSeries? {
    val rule = runCatching { JSONObject(ruleJson) }.getOrNull() ?: return null
    if (rule.optInt("version", 0) != 1) return null
    val frequency = runCatching {
        RecurrenceFrequency.valueOf(rule.optString("frequency"))
    }.getOrNull() ?: return null
    return LedgerRecurrenceSeries(
        id = id,
        frequency = frequency,
        startsOnEpochDay = startsOn,
        endsOnEpochDay = endsOn,
        active = active,
    )
}

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
    relativeOffsetDays = relativeOffsetDays,
    relativeMinuteOfDay = relativeMinuteOfDay,
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
    relativeOffsetDays = relativeOffsetDays,
    relativeMinuteOfDay = relativeMinuteOfDay,
    parentReminderId = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun relativeTriggerAt(
    date: LocalDate,
    offsetDays: Int,
    minuteOfDay: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long =
    date.minusDays(offsetDays.toLong())
        .atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

private fun EntryActivityEntity.toDomain() = LedgerActivity(
    id = id,
    entryId = entryId,
    action = action,
    details = details,
    occurredAt = occurredAt,
)

private fun EntryStatus.displayName(): String = when (this) {
    EntryStatus.PENDING -> "Not started"
    EntryStatus.IN_PROGRESS -> "In progress"
    EntryStatus.COMPLETED -> "Done"
    EntryStatus.CANCELLED -> "Cancelled"
}

private fun RelationshipType.activityLabel(): String = when (this) {
    RelationshipType.PREPARATION -> "Preparation"
    RelationshipType.FOLLOW_UP -> "Follow-up"
    RelationshipType.RELATED -> "Related entry"
    RelationshipType.RECURRENCE -> "Recurring occurrence"
}

private fun String.activityPreview(): String =
    if (length <= 80) this else "${take(80)}…"

private fun formatReminderTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a"))
