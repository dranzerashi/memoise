package com.memoise.ledger.ui.ledger

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoise.ledger.data.repository.EntryRepository
import com.memoise.ledger.domain.model.DatePurpose
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.LedgerEntry
import com.memoise.ledger.domain.model.LedgerActivity
import com.memoise.ledger.domain.model.LedgerChecklistItem
import com.memoise.ledger.domain.model.LedgerDescriptionBlock
import com.memoise.ledger.domain.model.LedgerNote
import com.memoise.ledger.domain.model.LedgerReminder
import com.memoise.ledger.domain.model.LedgerRelationship
import com.memoise.ledger.domain.model.LedgerRecurrenceSeries
import com.memoise.ledger.domain.model.RecurrenceFrequency
import com.memoise.ledger.domain.model.RelationshipType
import com.memoise.ledger.domain.model.EntryType
import com.memoise.ledger.domain.model.LedgerSearchMatch
import com.memoise.ledger.domain.model.SearchDateFilter
import com.memoise.ledger.domain.model.SearchRelationshipFilter
import com.memoise.ledger.domain.model.LedgerRestorePreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repository: EntryRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var pendingRestoreJson: String? = null
    private var pendingDatabaseRestore: java.io.File? = null
    val entries: StateFlow<List<LedgerEntry>> = repository.observeEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun searchEntries(
        query: String,
        status: EntryStatus?,
        type: EntryType?,
        dateFilter: SearchDateFilter,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        relationshipFilter: SearchRelationshipFilter,
    ) = repository.searchEntries(query, status, type, dateFilter, dateFrom, dateTo, relationshipFilter)

    fun exportJson(destination: Uri, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val saved = try {
                val json = repository.exportJson()
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(destination, "wt")
                        ?: error("Could not open the selected file")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
                }
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            onFinished(saved)
        }
    }

    fun exportDatabaseSnapshot(destination: Uri, onFinished: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            var snapshot: java.io.File? = null
            try {
                val snapshotFile = withContext(Dispatchers.IO) { repository.createDatabaseSnapshot() }
                snapshot = snapshotFile
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(destination, "wt")
                        ?: error("Could not open the selected file")
                    output.use { stream -> snapshotFile.inputStream().use { it.copyTo(stream) } }
                }
                onFinished(true, null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onFinished(false, error.message ?: "Could not create the database backup.")
            } finally {
                snapshot?.let { file ->
                    withContext(NonCancellable + Dispatchers.IO) { file.delete() }
                }
            }
        }
    }

    fun prepareRestore(uri: Uri, onPrepared: (LedgerRestorePreview?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                pendingDatabaseRestore?.delete()
                pendingDatabaseRestore = null
                val json = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Could not open the selected file")
                    input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                val preview = withContext(Dispatchers.Default) { repository.inspectBackup(json) }
                pendingRestoreJson = json
                onPrepared(preview, null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                pendingRestoreJson = null
                onPrepared(null, error.message ?: "Could not read this backup file.")
            }
        }
    }

    fun discardPreparedRestore() {
        pendingRestoreJson = null
        pendingDatabaseRestore?.delete()
        pendingDatabaseRestore = null
    }

    fun prepareDatabaseRestore(uri: Uri, onPrepared: (LedgerRestorePreview?, String?) -> Unit) {
        viewModelScope.launch {
            val snapshot = java.io.File(context.cacheDir, "memoise-import-${UUID.randomUUID()}.db")
            try {
                pendingRestoreJson = null
                pendingDatabaseRestore?.delete()
                pendingDatabaseRestore = null
                withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Could not open the selected file")
                    input.use { source -> snapshot.outputStream().use { source.copyTo(it) } }
                }
                val preview = withContext(Dispatchers.IO) { repository.inspectDatabaseBackup(snapshot) }
                pendingDatabaseRestore = snapshot
                onPrepared(preview, null)
            } catch (cancelled: CancellationException) {
                snapshot.delete()
                throw cancelled
            } catch (error: Exception) {
                snapshot.delete()
                onPrepared(null, error.message ?: "Could not read this database backup.")
            }
        }
    }

    fun restorePrepared(onFinished: (Boolean, String?) -> Unit) {
        val json = pendingRestoreJson
        val snapshot = pendingDatabaseRestore
        if (json == null && snapshot == null) {
            onFinished(false, "Choose a backup file first.")
            return
        }
        viewModelScope.launch {
            try {
                if (json != null) repository.restoreBackup(json)
                else repository.restoreDatabaseSnapshot(checkNotNull(snapshot))
                pendingRestoreJson = null
                pendingDatabaseRestore?.delete()
                pendingDatabaseRestore = null
                onFinished(true, null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onFinished(false, error.message ?: "Could not restore this backup.")
            }
        }
    }

    fun create(title: String, scheduledDate: LocalDate?) {
        val entry = newEntry(title, scheduledDate) ?: return
        viewModelScope.launch { repository.create(entry) }
    }

    fun createRelated(
        parentId: String,
        type: RelationshipType,
        title: String,
        scheduledDate: LocalDate?,
        sourceContentId: String? = null,
        sourceChecklistItemIds: List<String> = emptyList(),
    ) {
        val sourceId = sourceContentId ?: sourceChecklistItemIds.singleOrNull()
        val entry = newEntry(
            title = title,
            scheduledDate = scheduledDate,
            sourceEntryId = if (sourceContentId != null || sourceChecklistItemIds.isNotEmpty()) parentId else null,
            sourceContentId = sourceId,
        ) ?: return
        viewModelScope.launch {
            repository.createRelated(parentId, type, entry, sourceChecklistItemIds)
        }
    }

    fun relationshipsFor(entryId: String) = repository.observeRelationships(entryId)
    fun checklistFor(entryId: String) = repository.observeChecklist(entryId)
    fun activitiesFor(entryId: String) = repository.observeActivities(entryId)
    fun descriptionBlocksFor(entryId: String) = repository.observeDescriptionBlocks(entryId)
    fun notesFor(entryId: String) = repository.observeNotes(entryId)
    fun remindersFor(entryId: String) = repository.observeReminders(entryId)
    fun recurrenceFor(seriesId: String?): Flow<LedgerRecurrenceSeries?> =
        seriesId?.let(repository::observeRecurrence) ?: flowOf(null)

    fun createRecurrence(entryId: String, frequency: RecurrenceFrequency) {
        viewModelScope.launch { repository.createRecurrence(entryId, frequency) }
    }

    fun stopRecurrence(entryId: String) {
        viewModelScope.launch { repository.stopRecurrence(entryId) }
    }

    fun addChecklistItem(entryId: String, title: String) {
        viewModelScope.launch { repository.addChecklistItem(entryId, title) }
    }

    fun updateChecklistItem(item: LedgerChecklistItem) {
        viewModelScope.launch { repository.updateChecklistItem(item) }
    }

    fun deleteChecklistItem(item: LedgerChecklistItem) {
        viewModelScope.launch { repository.deleteChecklistItem(item.id) }
    }

    fun addDescriptionBlock(entryId: String, content: String) {
        viewModelScope.launch { repository.addDescriptionBlock(entryId, content) }
    }

    fun updateDescriptionBlock(block: LedgerDescriptionBlock) {
        viewModelScope.launch { repository.updateDescriptionBlock(block) }
    }

    fun deleteDescriptionBlock(block: LedgerDescriptionBlock) {
        viewModelScope.launch { repository.deleteDescriptionBlock(block.id) }
    }

    fun addNote(entryId: String, content: String) {
        viewModelScope.launch { repository.addNote(entryId, content) }
    }

    fun updateNote(note: LedgerNote) {
        viewModelScope.launch { repository.updateNote(note) }
    }

    fun deleteNote(note: LedgerNote) {
        viewModelScope.launch { repository.deleteNote(note.id) }
    }

    fun addReminder(entryId: String, triggerAt: Long, relativeOffsetDays: Int?, minuteOfDay: Int?) {
        viewModelScope.launch {
            repository.addReminder(entryId, triggerAt, relativeOffsetDays, minuteOfDay)
        }
    }

    fun updateReminder(
        reminder: LedgerReminder,
        triggerAt: Long,
        relativeOffsetDays: Int?,
        minuteOfDay: Int?,
    ) {
        viewModelScope.launch {
            repository.updateReminder(reminder, triggerAt, relativeOffsetDays, minuteOfDay)
        }
    }

    fun setReminderEnabled(reminder: LedgerReminder, enabled: Boolean) {
        viewModelScope.launch { repository.setReminderEnabled(reminder, enabled) }
    }

    fun deleteReminder(reminder: LedgerReminder) {
        viewModelScope.launch { repository.deleteReminder(reminder.id) }
    }

    fun update(entry: LedgerEntry) {
        if (entry.title.isBlank()) return
        viewModelScope.launch {
            repository.save(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun setStatus(entry: LedgerEntry, status: EntryStatus) {
        viewModelScope.launch { repository.setStatus(entry, status) }
    }

    fun setDate(entry: LedgerEntry, date: LocalDate?) {
        update(entry.copy(scheduledDate = date))
    }

    fun setDatePurpose(entry: LedgerEntry, purpose: DatePurpose) {
        update(entry.copy(datePurpose = purpose))
    }

    fun delete(entry: LedgerEntry) {
        viewModelScope.launch { repository.delete(entry.id) }
    }

    private fun newEntry(
        title: String,
        scheduledDate: LocalDate?,
        sourceEntryId: String? = null,
        sourceContentId: String? = null,
    ): LedgerEntry? {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return null
        val now = System.currentTimeMillis()
        return LedgerEntry(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            scheduledDate = scheduledDate,
            sourceEntryId = sourceEntryId,
            sourceContentId = sourceContentId,
            createdAt = now,
            updatedAt = now,
        )
    }
}
