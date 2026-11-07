package com.memoise.ledger.domain.model

data class LedgerSearchMatch(
    val entryId: String,
    val excerpt: String,
)

enum class SearchDateFilter {
    ANY,
    PAST,
    UPCOMING,
    UNSCHEDULED,
    RANGE,
}

enum class SearchRelationshipFilter {
    ANY,
    LINKED,
    UNLINKED,
}

data class LedgerRestorePreview(
    val entryCount: Int,
    val relationshipCount: Int,
    val exportedAt: String,
)
