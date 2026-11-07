package com.memoise.ledger.data.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class EntrySearchMatchRow(
    val entryId: String,
    val excerpt: String,
)

@Dao
interface EntrySearchDao {
    @Query(
        """SELECT search.entryId AS entryId,
                   snippet(ledger_search_fts, '“', '”', '…', -1, 16) AS excerpt
            FROM ledger_search_fts AS search
            JOIN ledger_entries AS entry ON entry.id = search.entryId
            WHERE ledger_search_fts MATCH :matchQuery
              AND (:status IS NULL OR entry.status = :status)
              AND (:type IS NULL OR entry.entryType = :type)
              AND (
                    :dateFilter = 'ANY'
                    OR (:dateFilter = 'UNDATED' AND entry.scheduledDate IS NULL)
                    OR (:dateFilter = 'PAST' AND entry.scheduledDate < :todayEpochDay)
                    OR (:dateFilter = 'UPCOMING' AND entry.scheduledDate >= :todayEpochDay)
                    OR (:dateFilter = 'RANGE' AND entry.scheduledDate IS NOT NULL
                        AND (:dateFrom IS NULL OR entry.scheduledDate >= :dateFrom)
                        AND (:dateTo IS NULL OR entry.scheduledDate <= :dateTo))
              )
              AND (
                    :relationshipFilter = 'ANY'
                    OR (:relationshipFilter = 'LINKED' AND EXISTS (
                        SELECT 1 FROM entry_relationships AS relationship
                        WHERE relationship.sourceEntryId = entry.id OR relationship.targetEntryId = entry.id
                    ))
                    OR (:relationshipFilter = 'UNLINKED' AND NOT EXISTS (
                        SELECT 1 FROM entry_relationships AS relationship
                        WHERE relationship.sourceEntryId = entry.id OR relationship.targetEntryId = entry.id
                    ))
              )
            ORDER BY CASE
                       WHEN lower(entry.title) LIKE '%' || lower(:rawQuery) || '%' THEN 0
                       ELSE 1
                     END,
                     entry.createdAt DESC
            LIMIT 200""",
    )
    fun search(
        matchQuery: String,
        rawQuery: String,
        status: String?,
        type: String?,
        dateFilter: String,
        dateFrom: Long?,
        dateTo: Long?,
        relationshipFilter: String,
        todayEpochDay: Long,
    ): Flow<List<EntrySearchMatchRow>>
}
