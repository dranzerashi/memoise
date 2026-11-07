# Personal Ledger – Android Application
## Product Requirements & Technical Implementation Specification

### 1. Project overview

Build a native Android application that acts as a personal chronological memory system.

The application is intended to help users remember, organize and follow through on everyday activities, obligations, appointments, events, preparations and follow-ups.

It is NOT a conventional calendar, task manager, reminder application or a combination of these.

The central concept is a **continuous chronological ledger of life events and obligations**, where every entry can contain additional information and be connected to other entries through preparations and follow-ups.

The application should feel like a personal logbook that evolves over time.

Its primary purpose is to reduce the cognitive effort required to remember things.

### 2. Core product philosophy

The following principles must guide every design and implementation decision:

1. One unified chronological timeline is the primary interface.
2. Creating an entry must be extremely quick.
3. Entries should be flexible and progressively enrichable.
4. Entries can be connected to other entries through preparations and follow-ups.
5. Relationships should be created through natural user actions, not graph configuration.
6. Checklists and description content should be independently actionable.
7. Unscheduled and overdue entries should have dedicated views.
8. Historical entries must remain accessible.
9. The application must work completely offline.
10. The user should never be forced to organize information when capturing it.

Avoid introducing conventional calendar-centric or project-management-centric UX patterns.

---

# 3. Platform and technology choices

Build a native Android application.

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Architecture | MVVM with repository pattern |
| Database | Room + SQLite |
| Relationships | Relational graph using edge table |
| Graph traversal | SQLite recursive CTEs |
| Search | SQLite FTS5 where supported; compatible Room FTS implementation otherwise |
| Asynchronous operations | Kotlin Coroutines |
| Reactive state | StateFlow |
| Dependency injection | Hilt |
| Navigation | Navigation Compose |
| Preferences | Jetpack DataStore |
| Background work | WorkManager |
| Exact reminders | AlarmManager where permitted |
| Notifications | Android Notification APIs |
| Home screen widget | Jetpack Glance |
| Testing | JUnit, Room tests, Compose UI tests |

Use current stable versions of Android libraries and dependencies at implementation time. Avoid unnecessary experimental or alpha dependencies.

Set a sensible minimum Android SDK, preferably API 26 or higher unless a strong compatibility reason suggests otherwise.

Do not introduce a backend, account system, cloud database, graph database or external search infrastructure.

The architecture should remain simple and maintainable.

# 4. Application navigation and information architecture

The application has four primary destinations.

### 4.1 Timeline

The primary screen.

Displays all dated entries in one continuous chronological list, ordered by scheduled date.

Requirements:

- Past and future entries appear in the same list.
- Group entries using subtle date separators.
- No calendar grids or day/week/month containers.
- No separate task and event tabs.
- Scroll forwards and backwards through time.
- Automatically position the timeline around the current date when opening the application.
- Provide a floating Today action to return to the current date.
- Preserve completed historical entries.
- Keep overdue entries at their original chronological positions rather than moving them to today.
- Surface overdue entries in their dedicated view.
- Display upcoming entries without requiring calendar navigation.
- Support multiple entries on the same date.
- Use incremental loading or pagination for performance.

The timeline must remain the visual centre of the application.

### 4.2 Unscheduled

A dedicated view containing entries that do not have an assigned scheduled date.

Requirements:

- Include all entry types.
- Allow entries to be edited and assigned dates.
- Allow entries to be connected to existing entries.
- Allow entries to be searched and filtered.
- Moving an entry from Unscheduled to a dated entry must update its timeline placement.
- Entries must never be duplicated when moved between views.

### 4.3 Overdue

A dedicated view aggregating unfinished obligations whose scheduled dates have passed.

Requirements:

- Include unfinished tasks and deadlines.
- Do not classify historical appointments or informational events as overdue merely because their dates have passed.
- Allow users to complete, reschedule, cancel or edit entries.
- Preserve the original scheduled date until the user explicitly changes it.
- Completing an overdue entry must preserve its original scheduled date and record the actual completion timestamp.
- Do not create duplicate entries.

Overdue classification should be based on the entry's date purpose and completion state.

### 4.4 Search

Provide global search across the entire ledger.

Search must include:

- Titles.
- Descriptions.
- Description blocks.
- Notes.
- Checklist items.
- Related entry titles.

Results must show the title, scheduled date (if any), status and relevant matching content.

Tapping a result should open its entry detail screen.

Provide optional filters for status, entry type, date range and relationship context.

---

# 5. Entry model

Everything in the application should be represented by a unified Entry model.

Do not create separate task, event, reminder and occasion database subsystems.

An entry can optionally have a type:

- Task
- Event
- Occasion
- Note

The type is descriptive and should not force the user through different creation workflows.

### Entry properties

Each entry should support:

| Property | Description |
|---|---|
| ID | Unique identifier |
| Title | Required |
| Description | Optional structured content |
| Type | Optional entry classification |
| Status | Pending, in progress, completed, cancelled |
| Scheduled date | Optional primary date |
| Scheduled time | Optional |
| Descriptive end date | Optional metadata only |
| Date purpose | Event, deadline or flexible task date |
| Created timestamp | Creation time |
| Updated timestamp | Last modification |
| Completed timestamp | Actual completion time |
| Recurrence reference | Optional |
| Source entry | Optional originating entry |
| Source content reference | Optional originating checklist item or description block |

### Important date semantics

An entry has exactly one primary scheduled date for timeline placement.

An optional end date is permitted as descriptive metadata, but it must NEVER cause the entry to span multiple days in the timeline.

For example:

Vacation:
- Scheduled date: December 5.
- End date: December 10.

The entry appears once on December 5.

It must not occupy December 5–10 or create automatic daily entries.

Do not implement multi-day timeline cards.

### Entry status

Support:

- Pending
- In progress
- Completed
- Cancelled

Completion must record an independent timestamp.

Changing an entry's status must not silently change its scheduled date.

---

# 6. Quick capture

This is one of the highest-priority features.

Creating an entry must require only a title.

A user should be able to capture something in a few seconds and enrich it later.

### Quick capture interface

Provide a lightweight capture screen containing:

- Title input.
- Optional date selector.
- Optional description.
- Optional reminder.
- Save action.

The title is the only mandatory field.

If no date is selected, save the entry as Unscheduled.

Avoid forcing users to select an entry type, relationship, date or reminder.

### Expanded entry creation

Users may optionally expand the capture interface to add:

- Description.
- Checklist.
- Date and time.
- Recurrence.
- Reminder.
- Entry type.

Both quick and expanded creation must use the same underlying Entry model.

### Android quick capture widget

Implement a small home-screen widget using Jetpack Glance.

The initial widget should be a simple plus icon.

Tapping it should open a dedicated lightweight capture Activity or screen.

It should not require navigating through the entire application.

Also implement an Android launcher shortcut for quick capture.

Do not depend on editable text fields inside the widget.

The capture screen should support saving an entry without a date.

---

# 7. Entry detail screen

Tapping any entry opens its detail screen.

The screen should behave like a lightweight editable document rather than a rigid task form.

Suggested sections:

1. Title.
2. Date and status.
3. Description.
4. Checklist.
5. Preparations.
6. Follow-ups.
7. Related entries.
8. Notes.
9. Activity history.
10. Reminder and recurrence settings.

All sections should be optional.

Users must be able to progressively add information after the initial creation.

### Description editor

Use structured content blocks rather than one unstructured description string.

Support:

- Multiple paragraphs.
- Editing existing paragraphs.
- Adding and removing paragraphs.
- Text selection within paragraphs.
- Long-press contextual actions.
- Converting selected text or a whole paragraph into a preparation or follow-up entry.

Each content block must have a stable identifier.

Do not identify content blocks through their text alone.

When converting content into an entry:

1. Preserve the original text.
2. Create a new entry using the selected text as its initial title.
3. Create the appropriate relationship.
4. Preserve the source content reference.
5. Display a subtle indication that the content is linked to another entry.
6. Allow navigation to the created entry.

Editing the original content must not break the relationship.

### Checklist

Users can create multiple checklist items inside an entry.

Each item supports:

- Title.
- Completion state.
- Ordering.
- Editing.
- Deletion.
- Optional association with a generated entry.

Long-pressing a checklist item should provide contextual actions:

- Create preparation.
- Create follow-up.
- Edit.
- Delete.

Support selecting multiple checklist items and creating one new entry from the selected items.

When converting checklist items into an entry, retain their original context and maintain navigable references.

The original entry must not lose information.

---

# 8. Relationships and graph model

The application must internally support graph-like relationships between entries.

However, users must not be exposed to graph terminology, edge types or graph configuration.

Users interact through natural actions.

### Relationship creation

From an existing entry, provide:

- Add preparation.
- Add follow-up.
- Add related entry.

These actions automatically establish the appropriate relationship.

A preparation is something that needs to happen before or in preparation for the current entry.

A follow-up is something that needs to happen as a consequence of or after the current entry.

General related entries are used when neither classification applies.

### Examples

Journey home:
- Book train tickets.
- Pack luggage.
- Check PNR.

Pack luggage:
- Pack medicines.
- Pack clothes.
- Pack documents.

Buy medicines:
- Collect remaining medicine.

Relationships may be nested to arbitrary reasonable depth.

An entry may also be related to multiple other entries.

### Relationship navigation

Entry details should display relationships in distinct sections:

- Preparations.
- Follow-ups.
- Related entries.

Each related entry should show its title, date and status.

Allow users to navigate in both directions.

Provide a way to view the broader relationship chain using a nested list.

Do not build a visual graph editor in V1.

### Graph persistence

Represent graph nodes using Entry records.

Represent relationships using a separate relational table.

Suggested schema:

EntryRelationship:
- id
- sourceEntryId
- targetEntryId
- relationshipType
- createdAt

Use foreign keys and appropriate indexes.

Relationship creation and entry creation must occur in a single database transaction.

Prevent accidental circular preparation/follow-up chains through repository-level validation.

Deleting an entry must not leave dangling relationships.

Use recursive SQL queries or repository-level traversal to retrieve nested relationships.

The application must not require Neo4j or another external graph database.

---

# 9. Notes and activity history

Notes and activity history must be separate concepts.

### Notes

Users can append additional information to an entry at any time.

Notes may contain:

- Text.
- Date/time.
- Optional structured paragraphs.
- References to related entries.

Notes should not overwrite the original description.

### Activity history

Maintain a history of meaningful changes.

Examples:

- Entry created.
- Description updated.
- Checklist item completed.
- Follow-up created.
- Reminder changed.
- Entry completed.
- Entry rescheduled.

Each activity record should have:

- ID.
- Entry ID.
- Action type.
- Relevant details.
- Timestamp.

The activity history should be readable but not unnecessarily verbose.

---

# 10. Reminders and notifications

Reminders are independent of the entry's scheduled date.

Support:

- One or more reminders per entry.
- Exact or approximate notification times, depending on Android permissions and OS restrictions.
- Reminders relative to an entry's scheduled date.
- Custom reminder dates and times.
- Editing and disabling reminders.
- Rescheduling.
- Notification dismissal.
- Notification actions where appropriate.

Examples:

Journey home on December 5:
- Book tickets reminder: November 5.
- Packing reminder: December 4.
- Check PNR reminder: December 5.

These may be independent entries rather than multiple notifications on the main journey entry.

Use WorkManager for suitable inexact/background scheduling and AlarmManager for user-requested exact reminders where permitted.

Handle:

- Android notification permission.
- Exact alarm restrictions.
- Device reboot.
- Timezone changes.
- Updated scheduled dates.
- Deleted or completed entries.
- Duplicate notification prevention.

Notifications must always reflect the latest entry state.

---

# 11. Recurring entries

Support recurring occasions and obligations.

Initial recurrence types:

- Daily.
- Weekly.
- Monthly.
- Yearly.

Examples:

- Anniversary.
- Birthday.
- Monthly bill.
- Weekly activity.
- Annual insurance renewal.

Use a recurrence series with independently stored occurrences.

Each occurrence must have independent:

- Status.
- Notes.
- Reminders.
- Preparations.
- Follow-ups.
- Completion timestamp.

Support:

- Editing one occurrence.
- Editing this and future occurrences.
- Editing the recurrence definition.
- Skipping an occurrence.
- Changing one occurrence's date.

Generate a rolling window of future occurrences rather than creating an unlimited number.

Ensure completed occurrences do not affect future occurrences.

Store recurrence rules in a versioned, extensible format.

Handle timezone and daylight-saving changes correctly.

---

# 12. Search implementation

Use local SQLite full-text search.

Prefer FTS5 if supported by the selected Android SQLite environment. If the minimum SDK or Room integration makes that unreliable, use a compatible Room-supported FTS implementation such as FTS4.

The search implementation must be fully local.

Maintain an index for:

- Entry title.
- Description blocks.
- Notes.
- Checklist items.

Update the search index transactionally or through a reliable synchronization mechanism whenever indexed content changes.

Search should support:

- Partial text matching where supported.
- Multiple search terms.
- Relevance ranking.
- Date filters.
- Status filters.
- Entry type filters.

Search should return the owning entry even when the matching content belongs to a note or checklist item.

Keep the search implementation replaceable so vector search can potentially be introduced in a later version.

Do not introduce Elasticsearch, a remote search service or a vector database in V1.

---

# 13. Database schema

Use Room entities and DAOs.

Proposed entities:

### Entry
- id: UUID/String primary key
- title
- type
- status
- scheduledDate
- scheduledTime
- descriptiveEndDate
- datePurpose
- completedAt
- createdAt
- updatedAt
- recurrenceSeriesId
- sourceEntryId
- sourceContentId

### EntryRelationship
- id
- sourceEntryId
- targetEntryId
- relationshipType
- createdAt

### DescriptionBlock
- id
- entryId
- content
- sortOrder
- createdAt
- updatedAt

### ChecklistItem
- id
- entryId
- title
- completed
- sortOrder
- linkedEntryId
- createdAt
- updatedAt

### EntryNote
- id
- entryId
- content
- createdAt
- updatedAt

### EntryActivity
- id
- entryId
- action
- details
- occurredAt

### Reminder
- id
- entryId
- triggerAt
- relativeOffset
- enabled
- reminderType
- notificationState

### RecurrenceSeries
- id
- rule
- startsAt
- endsAt
- timezone
- generationMetadata

### RecurrenceException
- id
- recurrenceSeriesId
- occurrenceDate
- exceptionType
- modifiedDate

### Search index

Create suitable FTS virtual tables for searchable entry content.

Add appropriate indexes for:

- scheduledDate
- status
- recurrenceSeriesId
- entryId foreign keys
- relationship source and target IDs
- reminder trigger times

Use migrations. Never rely on destructive database migrations that discard user data.

Use transactions for multi-entity operations.

The exact schema may be refined during implementation, but all product requirements must remain supported.

---

# 14. Local storage and backup

The application is LOCAL-ONLY in V1.

There must be:

- No backend.
- No account registration.
- No authentication system.
- No remote database.
- No automatic cloud synchronization.
- No dependency on internet connectivity for normal application functionality.

The local Room database is the single source of truth.

### Backup options

Implement:

1. Manual JSON export.
2. Complete application backup suitable for restoration.
3. User-selected Google Drive destination through Android's Storage Access Framework.
4. User-selected local file destination.
5. Import and restore.

Do not implement live Google Drive synchronization.

Google Drive is simply a destination for backup files.

Use Android's document/file picker so the user can choose Google Drive or another available document provider without requiring broad storage permissions or a custom Google Drive backend integration.

### Backup contents

Include:

- Entries.
- Relationships.
- Description blocks.
- Checklist items.
- Notes.
- Activity history.
- Reminders.
- Recurrence definitions.
- Recurrence exceptions.
- Application metadata.
- Backup schema version.

If attachments are introduced, include them in the backup archive.

### Backup format

Provide a versioned portable export format.

Recommended structure:

- Manifest containing application and schema versions.
- Structured JSON data.
- Optional attachment directory.
- Integrity checksum.

Also provide a complete database snapshot mechanism for reliable restoration.

Ensure SQLite WAL state is handled safely when creating database snapshots.

### Restore

Before restoring:

- Validate the archive.
- Validate its schema version.
- Validate data integrity.
- Protect the existing local database.
- Ask for confirmation before replacing local data.

Restore must be transactional or recoverable.

The application must not lose existing data if an import fails.

Provide backup and restore functionality in Settings.

Do not silently upload backups.

Do not rely on Android automatic backup as a substitute for the application's explicit backup system. Make the app's backup behaviour clear to the user.

---

# 15. UI and visual design

The application should have a clean, understated, modern design.

It should feel like a personal logbook rather than a productivity dashboard.

### Visual principles

- Timeline-first design.
- Minimal visual clutter.
- Subtle date separators.
- Compact but readable entry cards.
- Clear typography.
- Neutral colours.
- Consistent visual language across entry types.
- Status indicators that are immediately understandable.
- Support light and dark themes.
- Material 3 components where appropriate.

Avoid:

- Calendar grids.
- Kanban boards.
- Excessive coloured cards.
- Large dashboard statistics.
- Separate task and event sections.
- Complicated creation forms.
- Excessive mandatory categorization.

### Timeline cards

Each card should show relevant information:

- Title.
- Date/time where useful.
- Status.
- Optional entry type indicator.
- Small relationship indicator when related entries exist.
- Optional checklist progress.

Different entry types may have subtle visual differences, but should share a consistent layout.

### Entry interactions

Support:

- Tap to open details.
- Long-press for contextual actions.
- Quick completion where appropriate.
- Quick editing.
- Creating preparations and follow-ups.
- Navigating to related entries.

Provide a persistent quick-capture action.

---

# 16. Architecture and package organization

Use a maintainable architecture with clear separation of responsibilities.

Suggested organization:

app/
- data/
  - database/
  - dao/
  - entities/
  - repository/
  - search/
  - backup/
- domain/
  - model/
  - usecase/
  - relationship/
  - recurrence/
- ui/
  - timeline/
  - entry/
  - capture/
  - search/
  - overdue/
  - unscheduled/
  - settings/
- notifications/
- widget/
- navigation/

Avoid premature Gradle module fragmentation.

Keep business logic outside Composables.

ViewModels should expose UI state through StateFlow.

Repositories should own persistence and coordinate transactions.

Domain logic should be independently testable.

Use dependency injection consistently.

---

# 17. Performance and reliability

The application must remain responsive with thousands or tens of thousands of entries.

Requirements:

- Paginated timeline loading.
- Indexed date queries.
- Indexed relationship queries.
- Indexed full-text search.
- Avoid loading the entire database into memory.
- Avoid unnecessary recomposition.
- Persist changes immediately.
- Use database transactions for related operations.
- Use stable IDs for all entities.
- Handle application process death.
- Restore reminders after reboot.
- Handle timezone changes.
- Handle database migrations safely.

No periodic polling should be required to update the timeline.

Use reactive database observation where appropriate.

---

# 18. Testing requirements

Implement automated tests for important domain and persistence behaviour.

### Database tests

- Create and retrieve entries.
- Update entries.
- Delete entries.
- Create relationships.
- Retrieve nested relationships.
- Prevent invalid relationship cycles.
- Create an entry and relationship transactionally.
- Search across content types.
- Database migrations.
- Backup and restore integrity.

### Timeline tests

- Chronological ordering.
- Same-date entries.
- Past and future entries.
- Unscheduled exclusion.
- Overdue classification.
- Completed entry retention.
- Descriptive end dates not spanning timeline.

### Relationship tests

- Multiple preparations.
- Multiple follow-ups.
- Nested preparations.
- Cross-linked entries.
- Bidirectional navigation.
- Conversion from checklist items.
- Conversion from description blocks.

### Recurrence tests

- Daily recurrence.
- Weekly recurrence.
- Monthly recurrence.
- Yearly recurrence.
- Independent occurrence status.
- Exceptions.
- Editing future occurrences.

### Notification tests

- Reminder scheduling.
- Rescheduling.
- Cancellation.
- Reboot recovery.
- Duplicate prevention.
- Permission handling.

### Backup tests

- Export.
- Import.
- Corrupted archive rejection.
- Unsupported schema version.
- Restore without data loss.

### UI tests

- Quick capture.
- Timeline scrolling.
- Entry editing.
- Checklist interactions.
- Long-press contextual actions.
- Preparation creation.
- Follow-up creation.
- Search.
- Overdue and Unscheduled navigation.

---

# 19. Implementation phases

Do not attempt to build everything in one unreviewed pass.

Implement in the following phases.

### Phase 1: Foundation

- Initialize Android project.
- Configure Kotlin, Compose, Material 3 and Hilt.
- Implement Room database.
- Implement Entry model and repositories.
- Implement basic navigation.
- Implement DataStore preferences.

Deliverable: A working application foundation.

### Phase 2: Core ledger

- Implement timeline.
- Implement chronological grouping.
- Implement quick capture.
- Implement entry details.
- Implement editing and deletion.
- Implement completion.
- Implement Unscheduled view.
- Implement Overdue view.

Deliverable: A functional personal ledger.

### Phase 3: Relationships and rich entries

- Implement graph relationship persistence.
- Implement preparations and follow-ups.
- Implement nested relationship navigation.
- Implement structured description blocks.
- Implement checklist items.
- Implement conversion of description selections into entries.
- Implement conversion of checklist items into entries.
- Implement notes and activity history.

Deliverable: A fully interconnected ledger.

### Phase 4: Reminders and recurrence

- Implement reminder management.
- Implement notification scheduling.
- Implement reboot recovery.
- Implement recurrence series.
- Implement independently managed occurrences.

Deliverable: Reliable reminders and recurring entries.

### Phase 5: Search

- Implement full-text indexing.
- Implement search UI.
- Implement filters.
- Implement result navigation.
- Validate search performance.

Deliverable: Complete local search.

### Phase 6: Backup and widget

- Implement JSON export.
- Implement database backup.
- Implement restore.
- Integrate Android document picker and Google Drive destinations.
- Implement Jetpack Glance quick-capture widget.
- Implement launcher shortcut.

Deliverable: Data portability and quick access.

### Phase 7: Polish and verification

- Light and dark themes.
- Accessibility.
- UI refinement.
- Performance testing.
- Database migration tests.
- Notification reliability testing.
- Backup integrity testing.
- End-to-end verification.

Deliverable: A release-ready V1.

---

# 20. Explicitly out of scope for V1

Do not implement:

- Backend services.
- User accounts.
- Cloud synchronization.
- Multi-device synchronization.
- Neo4j or another graph database.
- Elasticsearch.
- Vector databases.
- AI or LLM integration.
- Automatic email or message ingestion.
- Complex calendar interfaces.
- Project management features.
- Multi-user collaboration.
- Social features.

The architecture may accommodate some of these capabilities in the future, but they must not complicate V1.

---

# 21. Definition of done

The first release is complete when the following scenario works end to end:

1. User creates an entry called "Journey home" scheduled for December 5.
2. User opens it and creates a preparation called "Book train tickets" scheduled for November 5.
3. User creates another preparation called "Pack luggage" scheduled for December 4.
4. User adds a checklist to Pack luggage.
5. User long-presses "Medicines" and converts it into a separate preparation entry.
6. User adds a description to Journey home.
7. User selects a sentence and converts it into a follow-up entry.
8. User creates a recurring anniversary.
9. User creates an unscheduled entry and later assigns it a date.
10. User creates an overdue task and sees it in the Overdue view without changing its original date.
11. User searches for a term and finds matching entries across descriptions, notes and checklists.
12. User receives a configured reminder.
13. User exports a complete backup.
14. User restores the backup and verifies that entries, relationships, notes, checklists and recurrence data are preserved.
15. User can continue using the application with no internet connection.

All of these operations must work without a backend.

---

# 22. Instructions to the coding agent

Before implementing:

1. Review the entire specification.
2. Produce a proposed project structure.
3. Identify any Android platform constraints, particularly around exact alarms, widgets, text selection and SQLite FTS support.
4. Propose the final Room schema and database relationships.
5. Identify potential ambiguities or implementation risks.
6. Present the implementation plan.

Do not silently omit requirements.

Do not replace the timeline with a calendar.

Do not introduce a graph database merely because the underlying data model is graph-like.

Do not create separate task and event management systems.

Prioritize a cohesive, easy-to-use product over the number of features implemented.

Implement in phases, build and test after each phase, and keep the project compiling throughout development.

Use sensible defaults and avoid asking the user to configure details that can be inferred from context.

The final product should feel like a natural extension of the user's memory: a place where things can be captured immediately, organized gradually, connected to other things and retrieved whenever needed.

**The central objective: The user should not have to remember to remember.**