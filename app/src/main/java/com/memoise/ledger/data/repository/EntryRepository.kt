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
import com.memoise.ledger.domain.model.DatePurpose
import com.memoise.ledger.domain.model.RelationshipType
import com.memoise.ledger.domain.model.EntryType
import com.memoise.ledger.domain.model.LedgerSearchMatch
import com.memoise.ledger.domain.model.SearchDateFilter
import com.memoise.ledger.domain.model.SearchRelationshipFilter
import com.memoise.ledger.domain.model.LedgerRestorePreview
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
import java.time.Instant
import java.time.ZoneOffset
import java.security.MessageDigest
import java.util.Locale
import com.memoise.ledger.BuildConfig
import org.json.JSONObject
import org.json.JSONArray

interface EntryRepository {
    fun observeEntries(): Flow<List<LedgerEntry>>
    suspend fun exportJson(): String
    suspend fun inspectBackup(json: String): LedgerRestorePreview
    suspend fun restoreBackup(json: String): LedgerRestorePreview
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
    override suspend fun exportJson(): String = database.withTransaction {
        val backupDao = database.ledgerBackupDao()
        val data = JSONObject()
        data.put("entries", JSONArray().apply {
            backupDao.entries().forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    putNullable("type", entry.type?.name)
                    put("status", entry.status.name)
                    putNullable("scheduledDate", entry.scheduledDate?.toString())
                    putNullable("scheduledTime", entry.scheduledTime?.toString())
                    putNullable("descriptiveEndDate", entry.descriptiveEndDate?.toString())
                    put("datePurpose", entry.datePurpose.name)
                    put("createdAt", entry.createdAt)
                    put("updatedAt", entry.updatedAt)
                    putNullable("completedAt", entry.completedAt)
                    putNullable("recurrenceSeriesId", entry.recurrenceSeriesId)
                    putNullable("sourceEntryId", entry.sourceEntryId)
                    putNullable("sourceContentId", entry.sourceContentId)
                })
            }
        })
        data.put("relationships", JSONArray().apply {
            backupDao.relationships().forEach { relationship ->
                put(JSONObject().apply {
                    put("id", relationship.id)
                    put("sourceEntryId", relationship.sourceEntryId)
                    put("targetEntryId", relationship.targetEntryId)
                    put("type", relationship.relationshipType.name)
                    put("createdAt", relationship.createdAt)
                })
            }
        })
        data.put("descriptionBlocks", JSONArray().apply {
            backupDao.descriptionBlocks().forEach { block ->
                put(JSONObject().apply {
                    put("id", block.id)
                    put("entryId", block.entryId)
                    put("content", block.content)
                    put("sortOrder", block.sortOrder)
                    put("createdAt", block.createdAt)
                    put("updatedAt", block.updatedAt)
                })
            }
        })
        data.put("checklistItems", JSONArray().apply {
            backupDao.checklistItems().forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("entryId", item.entryId)
                    put("title", item.title)
                    put("completed", item.completed)
                    put("sortOrder", item.sortOrder)
                    putNullable("linkedEntryId", item.linkedEntryId)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        })
        data.put("notes", JSONArray().apply {
            backupDao.notes().forEach { note ->
                put(JSONObject().apply {
                    put("id", note.id)
                    put("entryId", note.entryId)
                    put("content", note.content)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                })
            }
        })
        data.put("activityHistory", JSONArray().apply {
            backupDao.activities().forEach { activity ->
                put(JSONObject().apply {
                    put("id", activity.id)
                    put("entryId", activity.entryId)
                    put("action", activity.action)
                    putNullable("details", activity.details)
                    put("occurredAt", activity.occurredAt)
                })
            }
        })
        data.put("reminders", JSONArray().apply {
            backupDao.reminders().forEach { reminder ->
                put(JSONObject().apply {
                    put("id", reminder.id)
                    put("entryId", reminder.entryId)
                    put("triggerAt", reminder.triggerAt)
                    put("enabled", reminder.enabled)
                    putNullable("relativeOffsetDays", reminder.relativeOffsetDays)
                    putNullable("relativeMinuteOfDay", reminder.relativeMinuteOfDay)
                    putNullable("parentReminderId", reminder.parentReminderId)
                    put("createdAt", reminder.createdAt)
                    put("updatedAt", reminder.updatedAt)
                })
            }
        })
        data.put("recurrenceSeries", JSONArray().apply {
            backupDao.recurrenceSeries().forEach { series ->
                put(JSONObject().apply {
                    put("id", series.id)
                    put("rule", JSONObject(series.ruleJson))
                    put("startsOn", LocalDate.ofEpochDay(series.startsOn).toString())
                    putNullable("endsOn", series.endsOn?.let { LocalDate.ofEpochDay(it).toString() })
                    put("timeZoneId", series.timeZoneId)
                    put("active", series.active)
                    put("createdAt", series.createdAt)
                    put("updatedAt", series.updatedAt)
                })
            }
        })

        val dataText = data.toString()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(dataText.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        val manifest = JSONObject()
            .put("format", "memoise-personal-ledger")
            .put("backupSchemaVersion", 1)
            .put("applicationId", BuildConfig.APPLICATION_ID)
            .put("applicationVersion", BuildConfig.VERSION_NAME)
            .put("databaseSchemaVersion", database.openHelper.readableDatabase.version)
            .put("exportedAt", Instant.now().toString())
            .put("integrity", JSONObject().put("algorithm", "SHA-256").put("digest", digest))
        JSONObject().put("manifest", manifest).put("data", data).toString(2)
    }

    override suspend fun inspectBackup(json: String): LedgerRestorePreview =
        parseBackup(json).preview

    override suspend fun restoreBackup(json: String): LedgerRestorePreview {
        val backup = parseBackup(json)
        val backupDao = database.ledgerBackupDao()
        val previousReminderIds = backupDao.reminders().map(EntryReminderEntity::id)
        database.withTransaction {
            backupDao.clearRelationships()
            backupDao.clearReminders()
            backupDao.clearNotes()
            backupDao.clearDescriptionBlocks()
            backupDao.clearChecklistItems()
            backupDao.clearActivities()
            backupDao.clearEntries()
            backupDao.clearRecurrenceSeries()

            backupDao.insertRecurrenceSeries(backup.recurrenceSeries)
            backupDao.insertEntries(backup.entries)
            backupDao.insertRelationships(backup.relationships)
            backupDao.insertDescriptionBlocks(backup.descriptionBlocks)
            backupDao.insertChecklistItems(backup.checklistItems)
            backupDao.insertNotes(backup.notes)
            backupDao.insertActivities(backup.activities)
            backupDao.insertReminders(backup.reminders)
        }
        (previousReminderIds + backup.reminders.map(EntryReminderEntity::id)).distinct()
            .forEach(reminderScheduler::cancel)
        val now = System.currentTimeMillis()
        backup.reminders.forEach { reminder ->
            if (reminder.enabled && reminder.triggerAt > now) {
                reminderScheduler.schedule(reminder.id, reminder.triggerAt)
            }
        }
        return backup.preview
    }

    private fun parseBackup(json: String): LedgerBackupPayload {
        val root = JSONObject(json)
        val manifest = root.getJSONObject("manifest")
        require(manifest.getString("format") == "memoise-personal-ledger") {
            "This file is not a Memoise ledger export."
        }
        require(manifest.getInt("backupSchemaVersion") == 1) {
            "This backup format is not supported by this version of Memoise."
        }
        val data = root.getJSONObject("data")
        val integrity = manifest.getJSONObject("integrity")
        require(integrity.getString("algorithm") == "SHA-256") {
            "This backup uses an unsupported integrity check."
        }
        val actualDigest = MessageDigest.getInstance("SHA-256")
            .digest(data.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        require(actualDigest.equals(integrity.getString("digest"), ignoreCase = true)) {
            "This backup is incomplete or has been changed."
        }

        val entries = data.getJSONArray("entries").objects().map { row ->
            EntryEntity(
                id = row.getString("id"),
                title = row.getString("title"),
                type = row.nullableString("type")?.let(EntryType::valueOf),
                status = EntryStatus.valueOf(row.getString("status")),
                scheduledDate = row.nullableString("scheduledDate")?.let(LocalDate::parse),
                scheduledTime = row.nullableString("scheduledTime")?.let(LocalTime::parse),
                descriptiveEndDate = row.nullableString("descriptiveEndDate")?.let(LocalDate::parse),
                datePurpose = DatePurpose.valueOf(row.getString("datePurpose")),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
                completedAt = row.nullableLong("completedAt"),
                recurrenceSeriesId = row.nullableString("recurrenceSeriesId"),
                sourceEntryId = row.nullableString("sourceEntryId"),
                sourceContentId = row.nullableString("sourceContentId"),
            )
        }
        val relationships = data.getJSONArray("relationships").objects().map { row ->
            EntryRelationshipEntity(
                id = row.getString("id"),
                sourceEntryId = row.getString("sourceEntryId"),
                targetEntryId = row.getString("targetEntryId"),
                relationshipType = RelationshipType.valueOf(row.getString("type")),
                createdAt = row.getLong("createdAt"),
            )
        }
        val descriptionBlocks = data.getJSONArray("descriptionBlocks").objects().map { row ->
            EntryDescriptionBlockEntity(
                id = row.getString("id"),
                entryId = row.getString("entryId"),
                content = row.getString("content"),
                sortOrder = row.getInt("sortOrder"),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
            )
        }
        val checklistItems = data.getJSONArray("checklistItems").objects().map { row ->
            EntryChecklistEntity(
                id = row.getString("id"),
                entryId = row.getString("entryId"),
                title = row.getString("title"),
                completed = row.getBoolean("completed"),
                sortOrder = row.getInt("sortOrder"),
                linkedEntryId = row.nullableString("linkedEntryId"),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
            )
        }
        val notes = data.getJSONArray("notes").objects().map { row ->
            EntryNoteEntity(
                id = row.getString("id"),
                entryId = row.getString("entryId"),
                content = row.getString("content"),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
            )
        }
        val activities = data.getJSONArray("activityHistory").objects().map { row ->
            EntryActivityEntity(
                id = row.getString("id"),
                entryId = row.getString("entryId"),
                action = row.getString("action"),
                details = row.nullableString("details"),
                occurredAt = row.getLong("occurredAt"),
            )
        }
        val reminders = data.getJSONArray("reminders").objects().map { row ->
            EntryReminderEntity(
                id = row.getString("id"),
                entryId = row.getString("entryId"),
                triggerAt = row.getLong("triggerAt"),
                enabled = row.getBoolean("enabled"),
                relativeOffsetDays = row.nullableInt("relativeOffsetDays"),
                relativeMinuteOfDay = row.nullableInt("relativeMinuteOfDay"),
                parentReminderId = row.nullableString("parentReminderId"),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
            )
        }
        val recurrenceSeries = data.getJSONArray("recurrenceSeries").objects().map { row ->
            RecurrenceSeriesEntity(
                id = row.getString("id"),
                ruleJson = row.getJSONObject("rule").toString(),
                startsOn = LocalDate.parse(row.getString("startsOn")).toEpochDay(),
                endsOn = row.nullableString("endsOn")?.let(LocalDate::parse)?.toEpochDay(),
                timeZoneId = row.getString("timeZoneId"),
                active = row.getBoolean("active"),
                createdAt = row.getLong("createdAt"),
                updatedAt = row.getLong("updatedAt"),
            )
        }

        val entryIds = entries.map(EntryEntity::id).toSet()
        val seriesIds = recurrenceSeries.map(RecurrenceSeriesEntity::id).toSet()
        requireUniqueRecords(entries, EntryEntity::id)
        requireUniqueRecords(relationships, EntryRelationshipEntity::id)
        requireUniqueRecords(descriptionBlocks, EntryDescriptionBlockEntity::id)
        requireUniqueRecords(checklistItems, EntryChecklistEntity::id)
        requireUniqueRecords(notes, EntryNoteEntity::id)
        requireUniqueRecords(activities, EntryActivityEntity::id)
        requireUniqueRecords(reminders, EntryReminderEntity::id)
        requireUniqueRecords(recurrenceSeries, RecurrenceSeriesEntity::id)
        require(entries.all { it.title.isNotBlank() }) {
            "This backup contains an entry with no title."
        }
        require(entryIds.size == entries.size && seriesIds.size == recurrenceSeries.size) {
            "This backup contains duplicate records."
        }
        require(entries.all { it.recurrenceSeriesId == null || it.recurrenceSeriesId in seriesIds }) {
            "This backup refers to a missing repeat rule."
        }
        require(relationships.all { it.sourceEntryId in entryIds && it.targetEntryId in entryIds }) {
            "This backup contains a link to a missing entry."
        }
        require(relationships.none { it.sourceEntryId == it.targetEntryId }) {
            "This backup contains a link from an entry to itself."
        }
        require(descriptionBlocks.all { it.entryId in entryIds } && notes.all { it.entryId in entryIds } &&
            activities.all { it.entryId in entryIds } && reminders.all { it.entryId in entryIds }
        ) { "This backup contains details for a missing entry." }
        require(checklistItems.all {
            it.entryId in entryIds && (it.linkedEntryId == null || it.linkedEntryId in entryIds)
        }) { "This backup contains a checklist link to a missing entry." }
        require(reminders.all { it.parentReminderId == null || reminders.any { parent -> parent.id == it.parentReminderId } }) {
            "This backup contains a reminder that refers to a missing reminder."
        }
        val relationshipKeys = relationships.map { "${it.sourceEntryId}|${it.targetEntryId}|${it.relationshipType}" }
        require(relationshipKeys.toSet().size == relationshipKeys.size) {
            "This backup contains duplicate links."
        }

        val preview = LedgerRestorePreview(
            entryCount = entries.size,
            relationshipCount = relationships.size,
            exportedAt = manifest.getString("exportedAt"),
        )
        return LedgerBackupPayload(
            preview = preview,
            entries = entries,
            relationships = relationships,
            descriptionBlocks = descriptionBlocks,
            checklistItems = checklistItems,
            notes = notes,
            activities = activities,
            reminders = reminders,
            recurrenceSeries = recurrenceSeries,
        )
    }

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

private fun JSONObject.putNullable(name: String, value: Any?) {
    put(name, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else getString(name)

private fun JSONObject.nullableLong(name: String): Long? =
    if (isNull(name)) null else getLong(name)

private fun JSONObject.nullableInt(name: String): Int? =
    if (isNull(name)) null else getInt(name)

private fun JSONArray.objects(): List<JSONObject> =
    List(length()) { index -> getJSONObject(index) }

private fun <T, K> requireUniqueRecords(records: List<T>, key: (T) -> K) {
    require(records.map(key).toSet().size == records.size) {
        "This backup contains duplicate records."
    }
}

private data class LedgerBackupPayload(
    val preview: LedgerRestorePreview,
    val entries: List<EntryEntity>,
    val relationships: List<EntryRelationshipEntity>,
    val descriptionBlocks: List<EntryDescriptionBlockEntity>,
    val checklistItems: List<EntryChecklistEntity>,
    val notes: List<EntryNoteEntity>,
    val activities: List<EntryActivityEntity>,
    val reminders: List<EntryReminderEntity>,
    val recurrenceSeries: List<RecurrenceSeriesEntity>,
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
