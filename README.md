# Memoise

Memoise is a native, local-first personal ledger for Android.

## Current milestone: core ledger

The app saves entries and relationships locally with Room. Preparations, follow-ups, and related entries can be created from an entry, then opened from either side of the connection. Relationship and entry creation share a database transaction, directed preparation/follow-up loops are checked, and entry deletion cascades to its links. The Room schema has a non-destructive migration from the previous build.

Entry details support ordered description paragraphs with stable IDs and add, edit, and remove actions. Long-pressing a paragraph opens actions to create a preparation or follow-up, edit, or remove it; created entries link back to the preserved source paragraph. Ordered checklist items support add, edit, completion, and delete actions. Long-pressing an item can create a preparation or follow-up. Multiple selected items can also create one related entry, with each original item retaining its single navigable link. Notes are stored separately from descriptions, timestamped, and can be added, edited, or removed. Custom date/time reminders can be added, edited, disabled, or removed; WorkManager delivers them approximately and Android notification permission is requested when needed. A compact activity history records entry creation, status changes, relationship creation, checklist changes, note changes, and reminder changes. These additions use non-destructive Room migrations.

The timeline, Unscheduled, and Overdue views use the same entry records. Quick capture requires only a title and can optionally include a date. Entry details support title/date editing, date purpose, status, completion time, and deletion. Events remain out of the Overdue view, and completed entries keep their scheduled date.

The presentation guides the visual language with warm paper surfaces, serif display type, olive timeline marks, and restrained terracotta accents. Search, exact and relative reminder controls, recurrence, backup and restore, and the quick-capture widget are later milestones.

## Android configuration

- Kotlin with Jetpack Compose and Material 3
- MVVM, repository pattern, Hilt, and Room/SQLite
- Compile and target SDK 37
- Minimum SDK 26
- Java 17

Open the project directory in Android Studio and run the `app` configuration, or build a debug APK with `./gradlew assembleDebug`.

## Documentation

- [Product requirements and technical specification](docs/product-requirements.md)
- [UI/UX design and screen layout specification](docs/ui-ux-specification.md)
- [Append-only progress log](docs/progress.md)
- [Decisions log](docs/decisions.md)
- [Almanac design reference PDF](docs/references/Memoise%20Personal%20Ledger.pdf)
