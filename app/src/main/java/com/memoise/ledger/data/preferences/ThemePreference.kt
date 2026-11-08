package com.memoise.ledger.data.preferences

enum class ThemePreference(val storedValue: String, val label: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStoredValue(value: String?): ThemePreference =
            entries.firstOrNull { it.storedValue == value } ?: SYSTEM
    }
}
