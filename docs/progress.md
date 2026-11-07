# Memoise Progress Log

This is an append-only project log. Add new dated entries at the end; do not rewrite or remove earlier entries.

## 2026-11-07 — Documentation and current implementation checkpoint

### Source documents

- Added the complete Product Requirements & Technical Implementation Specification to [`product-requirements.md`](product-requirements.md). This is the source of truth for functionality and architecture.
- Added the complete UI/UX Design & Screen Layout Specification to [`ui-ux-specification.md`](ui-ux-specification.md). This is the source of truth for visual design and interaction patterns. The Canva/almanac presentation is a visual reference, not a strict template.

### Implemented

- Native Kotlin Android app using Jetpack Compose, Material 3, MVVM, Hilt, and Room/SQLite.
- Android compile and target SDK 37, minimum SDK 26, Java 17.
- Core ledger views: chronological Timeline, Unscheduled, and Overdue.
- Quick capture with a required title and optional date; entry detail editing, status/date-purpose changes, completion timestamp, and deletion.
- Preparations, follow-ups, and related entries, with Room-backed relationships, transactional creation, cycle validation for directed relationships, and cascading cleanup on deletion.
- Ordered checklist items with add, edit, completion, and delete actions.
- Compact activity history for entry creation, status changes, relationship creation, and checklist changes.
- Room schema snapshots and non-destructive migrations through schema version 3.
- Almanac-inspired styling cues including warm paper surfaces, serif display type, olive timeline marks, and restrained terracotta accents.

### Build, device verification, and repository

- The debug build assembled successfully and was provided for Pixel testing.
- The user has confirmed earlier delivered builds worked. The latest build includes the checklist and activity-history additions; explicit device verification of that latest checkpoint is still pending.
- Initialized the local Git repository on `main`, retained the GitHub repository's original `.gitignore` and `LICENSE`, and connected `origin` to `git@github.com:dranzerashi/memoise.git`.
- Pushed commit `d21d37e` (`Add Memoise Android ledger foundation`) on top of the remote's initial commit. The working tree was clean after the push.
- No CI/CD configuration was added.

### Planned work not yet implemented

Search, structured description blocks, notes, reminders/notifications, recurrence, backup and restore, the quick-capture widget and launcher shortcut, and broader accessibility/polish remain future work. Checklist and basic relationships/activity history are already implemented as described above.

## 2026-11-07 — Visual reference added

- Added the provided [Memoise Personal Ledger PDF](references/Memoise%20Personal%20Ledger.pdf) as the almanac-style visual reference. It informs design choices but does not override the product requirements or UI/UX specification.

## 2026-11-07 — Structured description paragraphs

- Added ordered description paragraphs to entry details. Paragraphs have stable IDs and support adding, editing, and removing while preserving their order.
- Added a Room `entry_description_blocks` table and non-destructive migration from schema version 3 to 4. Paragraph changes are recorded in entry activity history.
- `./gradlew assembleDebug` completed successfully. The updated build is ready for Pixel verification; device confirmation is pending. No automated test suite was run.

## 2026-11-07 — Pixel verification

- The user installed and verified the structured description paragraph build on their Pixel and confirmed it is working.
- This verified checkpoint is ready to commit and push before the next feature.

## 2026-11-07 — Description paragraph conversion

- Long-pressing a description paragraph now offers creation as a preparation or follow-up. The paragraph text fills the new entry title, the original paragraph remains unchanged, and the created entry stores stable source-entry and source-content references.
- The original entry shows a link to the created entry. Relationship creation continues to use the existing transactional path; no new schema migration was needed.
- `./gradlew assembleDebug` completed successfully. This build is ready for Pixel verification; device confirmation is pending. Converting only part of a selected paragraph remains future work.

## 2026-11-07 — Description paragraph action menu

- Moved paragraph Edit and Remove into the long-press context menu alongside relationship creation and linked-entry navigation. Removed the separate row buttons; Remove still asks for confirmation.
- `./gradlew assembleDebug` completed successfully. The adjusted menu build is ready for Pixel verification; device confirmation is pending.

## 2026-11-07 — Description paragraph actions

- The description context menu supports creating preparations/follow-ups, opening a linked entry, editing, and removing a paragraph. Current link cardinality is recorded as decision D-001.

## 2026-11-07 — Checklist item conversion

- Long-pressing a checklist item can create a preparation or follow-up with the item title prefilled. The original item retains a single link to the generated entry, and both records are saved with the relationship in one transaction.

## 2026-11-07 — Multi-item checklist conversion

- Checklist selection mode can convert multiple items into one preparation or follow-up. Each selected item keeps its own link to that shared entry; creation and linking are transactional.

## 2026-11-07 — Entry notes

- Added separate timestamped notes with add, edit, and remove actions, plus activity history entries for note changes.
- Added the `entry_notes` Room table and non-destructive migration from schema version 4 to 5.

## 2026-11-07 — Custom date/time reminders

- Added timestamped reminders with add, edit, enable/disable, and remove actions. Notifications use WorkManager and are approximate; completed or cancelled entries stop active reminders.
- Added the `entry_reminders` Room table and non-destructive migration from schema version 5 to 6.
- Delivery issue reported: reminders appear only while Memoise is open or in the background. Investigate on the Pixel when device access is available.

## 2026-11-07 — Recurring entry foundation

- Added daily, weekly, monthly, and yearly recurrence for dated entries. Occurrences are independent ledger entries, generated through a rolling 12-month window; stopping a series keeps existing occurrences.
- Added the versioned recurrence rule table and non-destructive migration from schema version 6 to 7.
- Debug build assembled successfully and is ready for Pixel verification. Editing a recurrence definition and choosing an edit scope remain future steps.

## 2026-11-07 — Relative reminders for recurring entries

- Added relative reminder choices including one week, three days, and one day before, plus on the day. Relative reminders follow scheduled-date changes.
- Future recurring occurrences inherit the parent’s reminder timing; parent reminder changes update upcoming copies, while editing an occurrence reminder makes it independent.
- Added non-destructive Room migrations from schema version 7 to 9. The updated debug build is ready for Pixel verification.

## 2026-11-07 — Reminder pattern selector

- Made reminder timing choices explicit when adding a reminder. Active recurring series can save a relative pattern even when its first occurrence’s reminder date has passed; the pattern applies to future occurrences.

## 2026-11-07 — Recurrence and reminder verification

- The user verified that recurrence and inherited reminder patterns work on the Pixel.
