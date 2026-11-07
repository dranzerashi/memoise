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
