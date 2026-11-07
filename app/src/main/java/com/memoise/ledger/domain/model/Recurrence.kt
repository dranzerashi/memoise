package com.memoise.ledger.domain.model

enum class RecurrenceFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
}

data class LedgerRecurrenceSeries(
    val id: String,
    val frequency: RecurrenceFrequency,
    val startsOnEpochDay: Long,
    val endsOnEpochDay: Long?,
    val active: Boolean,
)
