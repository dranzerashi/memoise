package com.memoise.ledger.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoise.ledger.data.repository.EntryRepository
import com.memoise.ledger.domain.model.DatePurpose
import com.memoise.ledger.domain.model.EntryStatus
import com.memoise.ledger.domain.model.LedgerEntry
import com.memoise.ledger.domain.model.LedgerActivity
import com.memoise.ledger.domain.model.LedgerChecklistItem
import com.memoise.ledger.domain.model.LedgerDescriptionBlock
import com.memoise.ledger.domain.model.LedgerRelationship
import com.memoise.ledger.domain.model.RelationshipType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repository: EntryRepository,
) : ViewModel() {
    val entries: StateFlow<List<LedgerEntry>> = repository.observeEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

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
    ) {
        val entry = newEntry(
            title = title,
            scheduledDate = scheduledDate,
            sourceEntryId = if (sourceContentId != null) parentId else null,
            sourceContentId = sourceContentId,
        ) ?: return
        viewModelScope.launch { repository.createRelated(parentId, type, entry) }
    }

    fun relationshipsFor(entryId: String) = repository.observeRelationships(entryId)
    fun checklistFor(entryId: String) = repository.observeChecklist(entryId)
    fun activitiesFor(entryId: String) = repository.observeActivities(entryId)
    fun descriptionBlocksFor(entryId: String) = repository.observeDescriptionBlocks(entryId)

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
