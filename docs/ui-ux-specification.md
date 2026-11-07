# Memoise – Personal Ledger
## UI/UX Design & Screen Layout Specification

This document supplements the main Memoise product requirements and technical implementation specification.

The previous specification defines what the application does and how it should be implemented.

This document defines how the application should look, feel and behave.

Follow both specifications together. If a visual decision conflicts with a functional requirement, preserve the functional requirement while maintaining the intended design philosophy.

---

# 1. Application identity

**Application name:** Memoise

**Subtitle:** Personal Ledger

**Primary tagline:** Everything has a place.

**Secondary description:**
Your personal memory, organised through time.

Memoise is a personal chronological memory system.

It should feel like a thoughtfully designed digital logbook rather than a conventional productivity application.

The brand should communicate:

- Continuity.
- Personal organization.
- Simplicity.
- Reliability.
- Calmness.
- A sense of keeping track of things that matter.

Avoid visual associations with corporate project management, business dashboards, calendars or conventional reminder applications.

The application should feel personal and welcoming without being childish.

Use `Memoise` consistently throughout the application, launcher identity, About screen and backup metadata.

---

# 2. Overall visual direction

Use a modern, understated Material 3 design with a distinctive editorial/logbook aesthetic.

### Design characteristics

- Clean layouts.
- Excellent typography.
- Generous but efficient spacing.
- Subtle separators.
- Minimal card decoration.
- Rounded corners used consistently.
- Neutral backgrounds.
- Restrained use of accent colours.
- Clear visual hierarchy.
- Smooth transitions.
- Comfortable one-handed operation.

Avoid:

- Excessive colourful cards.
- Large dashboard widgets.
- Heavy borders around everything.
- Calendar grids.
- Kanban boards.
- Excessive icons.
- Excessive use of badges.
- Complex floating panels.
- Visually noisy timelines.

### Theme

Implement both light and dark themes.

Prefer a warm, slightly off-white light background rather than pure white.

Use a restrained accent colour such as muted indigo, deep blue or teal.

Dark mode should use dark neutral surfaces rather than pure black everywhere.

Use semantic colours sparingly:

- Green for completed.
- Amber for due/overdue warnings.
- Red for genuinely important alerts.
- Muted neutral for pending.
- Accent colour for selected or highlighted elements.

Do not make every entry type a different colour.

Use Material 3 dynamic colour support if appropriate, but preserve Memoise's visual identity and readability.

### Typography

Use a clear, modern sans-serif typeface.

Prioritize:

- Entry titles.
- Dates.
- Descriptions.
- Checklist readability.

Date separators should be visually distinct but understated.

---

# 3. Primary navigation

The application should have four primary destinations:

1. Timeline.
2. Unscheduled.
3. Overdue.
4. Search.

The Timeline is the default landing screen.

Use a compact navigation structure suitable for Android phones.

A bottom navigation bar is acceptable, with the four destinations represented using clear icons and labels.

The quick-capture action should be accessible independently of the current navigation destination.

Do not introduce separate navigation destinations for tasks, events, occasions or reminders.

These are all entries in the same ledger.

---

# 4. Screen 1 – Timeline

This is the most important screen in the entire application.

It should receive the greatest amount of design and implementation attention.

### Layout

Suggested structure:

```text
┌─────────────────────────────────┐
│ Memoise                  Search │
│ Personal Ledger                 │
├─────────────────────────────────┤
│                                 │
│ THURSDAY · NOVEMBER 5            │
│                                 │
│ ✓  Book train tickets           │
│    For journey home             │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ FRIDAY · NOVEMBER 6              │
│                                 │
│ ✓  Buy medicines                │
│    2 of 3 purchased              │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ TODAY · NOVEMBER 7               │
│                                 │
│ ○  Collect remaining medicine   │
│    From pharmacy                 │
│                                 │
│ ○  Doctor appointment           │
│    5:30 PM                       │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ TOMORROW · NOVEMBER 8            │
│                                 │
│ ○  Prepare for journey          │
│                                 │
│                                 │
│                          [+]    │
├─────────────────────────────────┤
│ Timeline  Unscheduled  Overdue  │
│                    Search      │
└─────────────────────────────────┘
```

This is a structural illustration, not a requirement to reproduce the exact layout.

### Header

The header should contain:

- Memoise branding.
- Search access.
- Optionally a subtle current-date indicator.

Keep the header compact.

### Date grouping

Group entries by date.

Use separators such as:

- TODAY · NOVEMBER 7
- TOMORROW · NOVEMBER 8
- MONDAY · NOVEMBER 9
- DECEMBER 5 · SATURDAY

Avoid large date cards or calendar-style layouts.

### Entry cards

Entries should be compact and readable.

Display:

- Status indicator.
- Title.
- Optional time.
- Optional short description.
- Optional checklist progress.
- Optional subtle relationship indicator.

Completed entries should have a subdued appearance but remain readable.

Pending entries should be visually clear.

Overdue entries should retain their original position and have an appropriate warning indicator.

### Timeline behaviour

- Launch positioned around today.
- Scroll backwards for history.
- Scroll forwards for future events.
- Today shortcut available when scrolled away.
- Quick capture button always accessible.
- Incremental loading.
- Preserve scroll position when returning from an entry detail screen.

Do not automatically move overdue entries to today.

---

# 5. Screen 2 – Unscheduled

A clean list of entries that have been captured but not assigned a date.

Suggested layout:

```text
┌─────────────────────────────────┐
│ Unscheduled                     │
│ Things to organise later        │
├─────────────────────────────────┤
│                                 │
│ Renew passport                  │
│                                 │
│ Research new laptop             │
│                                 │
│ Check insurance policy          │
│                                 │
│ Buy replacement charger         │
│                                 │
│                                 │
│                          [+]    │
├─────────────────────────────────┤
│ Timeline  Unscheduled  Overdue  │
│                    Search      │
└─────────────────────────────────┘
```

Requirements:

- Same entry visual language as Timeline.
- No forced categorization.
- Quick assignment of dates.
- Support editing and relationship creation.
- Allow entries to be pinned or filtered if implemented.
- Completed entries should remain accessible through search/history rather than being silently discarded.

This screen should feel like a collection of things waiting to find their place in time.

---

# 6. Screen 3 – Overdue

This screen should help users recover unfinished obligations.

Suggested layout:

```text
┌─────────────────────────────────┐
│ Overdue                         │
│ Things that still need attention│
├─────────────────────────────────┤
│                                 │
│ NOVEMBER 5                      │
│                                 │
│ ○  Submit insurance documents   │
│    2 days overdue               │
│                                 │
│ NOVEMBER 6                      │
│                                 │
│ ○  Collect medicines            │
│    1 day overdue                │
│                                 │
│                                 │
│                          [+]    │
├─────────────────────────────────┤
│ Timeline  Unscheduled  Overdue  │
│                    Search      │
└─────────────────────────────────┘
```

Requirements:

- Only unfinished obligations.
- Historical events should not automatically appear here.
- Show original due date.
- Show how long overdue.
- Provide quick actions to complete, reschedule or edit.
- Never change the original date just because an entry is overdue.

Use warning colours sparingly.

The screen should be useful without feeling alarming or guilt-inducing.

---

# 7. Screen 4 – Search

Search should be immediately accessible.

Suggested layout:

```text
┌─────────────────────────────────┐
│ Search                          │
├─────────────────────────────────┤
│ [ Search everything...       ] │
│                                 │
│ Recent searches                 │
│                                 │
│ train                           │
│ medicines                       │
│                                 │
│ RESULTS                         │
│                                 │
│ Journey home                    │
│ December 5                      │
│                                 │
│ Book train tickets              │
│ November 5 · Completed          │
│                                 │
│ Pack luggage                    │
│ December 4                      │
│                                 │
└─────────────────────────────────┘
```

Requirements:

- Search across titles, descriptions, notes and checklist items.
- Highlight matching text where practical.
- Show dates and statuses.
- Allow filtering.
- Tapping a result opens the corresponding entry.
- Show the content fragment that matched the query when appropriate.

Avoid making search feel like a separate data-management system.

---

# 8. Screen 5 – Quick capture

This screen is extremely important.

It should be possible to capture an idea or obligation almost immediately.

Suggested layout:

```text
┌─────────────────────────────────┐
│ New entry                  Save │
├─────────────────────────────────┤
│                                 │
│ What do you need to remember?   │
│                                 │
│ Collect medicine from pharmacy  │
│                                 │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ ◷  Add date                     │
│                                 │
│ ≡  Add description              │
│                                 │
│ □  Add checklist                │
│                                 │
│ ♧  Add reminder                 │
│                                 │
│ ↻  Repeat                       │
│                                 │
└─────────────────────────────────┘
```

The title should receive focus automatically.

Requirements:

- Title is mandatory.
- Everything else is optional.
- No mandatory type selection.
- No mandatory date selection.
- Saving without a date creates an unscheduled entry.
- Save should be immediately accessible.
- Support keyboard submission where appropriate.
- Provide a clear dismissal action.
- Do not open a complicated full-screen form unnecessarily.

The screen should support both quick and expanded creation.

---

# 9. Screen 6 – Entry details

The entry details screen is the second most important screen.

It should feel like an editable personal document.

Suggested layout:

```text
┌─────────────────────────────────┐
│ ←                         ⋮     │
├─────────────────────────────────┤
│                                 │
│ Journey home                    │
│                                 │
│ December 5 · 10:30 PM           │
│                                 │
│ Pending                         │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ DESCRIPTION                     │
│                                 │
│ Going home for the holidays.    │
│ Need to carry my laptop and     │
│ important documents.            │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ PREPARATIONS                    │
│                                 │
│ ✓ Book train tickets            │
│ ○ Prepare luggage               │
│ ○ Check PNR                     │
│                                 │
│ [+ Add preparation]             │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ FOLLOW-UPS                      │
│                                 │
│ [+ Add follow-up]               │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ CHECKLIST                       │
│                                 │
│ No checklist                    │
│ [+ Add checklist]               │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ NOTES                           │
│                                 │
│ [+ Add note]                    │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ ACTIVITY HISTORY                │
│                                 │
│ Entry created                   │
│ Tickets booked                  │
│                                 │
└─────────────────────────────────┘
```

Adapt the layout to actual content.

Empty sections may be hidden or displayed as unobtrusive add actions.

### Interaction requirements

- Title can be edited inline.
- Description can be edited.
- Date and time can be changed.
- Status can be changed.
- Checklist items can be added.
- Preparations and follow-ups can be added.
- Notes can be appended.
- Related entries can be opened.
- Reminders can be managed.
- Recurrence can be managed.
- Activity history can be expanded.

Use progressive disclosure.

Do not display every possible property as a mandatory form field.

---

# 10. Screen 7 – Adding preparations and follow-ups

These actions should feel contextual.

When a user taps Add preparation from an existing entry, open a lightweight creation interface.

Suggested layout:

```text
┌─────────────────────────────────┐
│ New preparation                 │
├─────────────────────────────────┤
│                                 │
│ What needs to be done?          │
│                                 │
│ Pack luggage                    │
│                                 │
│ For                             │
│ Journey home                    │
│                                 │
│ Date                            │
│ December 4                      │
│                                 │
│ Description                     │
│ Optional                        │
│                                 │
│                          Save   │
└─────────────────────────────────┘
```

Requirements:

- Automatically establish the relationship.
- Show the originating entry.
- Use selected text as the initial title when conversion initiated the action.
- Allow date assignment.
- Allow no date.
- Return to the original entry after creation.
- Preserve source content and checklist references.
- No graph-edge configuration.

Follow-up creation should use the same layout with appropriate terminology.

---

# 11. Screen 8 – Checklist and content conversion

Long-pressing a checklist item or description paragraph should open a contextual menu.

Suggested actions:

```text
Create preparation
Create follow-up
Copy
Edit
Delete
```

When multiple checklist items are selected, allow them to be converted together into a single new entry.

When selecting a paragraph, allow the selected text to become the new entry's initial title.

Preserve the original content and maintain a link to the generated entry.

Show a subtle linked-entry affordance in the original content.

Do not make this interaction visually complicated.

---

# 12. Screen 9 – Recurring entry

Provide a simple recurrence configuration interface.

Suggested layout:

```text
Repeat

Does not repeat
Daily
Weekly
Monthly
Yearly
Custom

Starts
December 20, 2026

Ends
Never

Reminder
7 days before

Save
```

Avoid exposing complicated recurrence-rule syntax.

When editing an occurrence, present:

- This occurrence.
- This and future occurrences.
- Entire recurrence series.

Each generated occurrence should open the normal entry detail screen.

---

# 13. Screen 10 – Reminder configuration

Allow users to configure one or more reminders.

Suggested interface:

```text
Reminders

+ Add reminder

1 day before
At 9:00 AM

On the day
At 8:00 AM

Save
```

Support absolute and relative reminders.

Clearly distinguish reminder time from the entry's scheduled date.

---

# 14. Screen 11 – Settings and backup

Keep Settings simple.

Suggested sections:

**Appearance**
- Light theme.
- Dark theme.
- System default.

**Notifications**
- Default reminder preferences.
- Notification permissions.

**Backup and restore**
- Export JSON.
- Create complete backup.
- Save to Google Drive.
- Save to local storage.
- Restore backup.
- Last backup information.

**Data**
- Export.
- Restore.
- Storage information.

**About Memoise**
- Application version.
- Application name.
- Product description.
- Privacy information.

Do not introduce account settings or cloud-sync configuration.

---

# 15. Screen 12 – Home-screen widget

Implement a small Jetpack Glance widget.

Initial design:

A simple rounded square containing the Memoise plus icon.

Tapping it opens Quick Capture directly.

Also implement an application launcher shortcut for Quick Capture.

A larger widget showing upcoming entries can be considered as a future enhancement.

The small widget should be implemented first.

---

# 16. Navigation and interaction consistency

The following rules should be applied across all screens:

- Back navigation should preserve previous screen state.
- Returning from entry details should preserve timeline scroll position.
- Quick capture should be available from every primary destination.
- Related entries should be accessible from entry details.
- Search results should navigate to the original entry.
- Completing an entry should immediately update all relevant views.
- Assigning a date to an unscheduled entry should immediately place it in the timeline.
- Rescheduling an overdue entry should remove it from the overdue view when appropriate.
- No operation should create duplicate entries.
- Save changes immediately.

Use smooth, restrained transitions.

Avoid unnecessary animations that slow down ordinary interactions.

---

# 17. Responsive design and accessibility

The initial target is Android phones.

Nevertheless:

- Support different screen sizes.
- Support portrait and landscape.
- Use adaptive layouts for tablets where practical.
- Support Android system font scaling.
- Provide adequate touch targets.
- Support TalkBack.
- Maintain sufficient colour contrast.
- Avoid communicating status through colour alone.

The application must remain usable with large accessibility text settings.

---

# 18. Design implementation instructions

Use Jetpack Compose and Material 3.

Create reusable composables for:

- Timeline date header.
- Timeline entry row.
- Entry status indicator.
- Entry detail section.
- Checklist item.
- Relationship entry.
- Quick capture input.
- Search result.
- Reminder row.
- Recurrence selector.

Use a consistent spacing, typography, shape and colour system.

Keep composables focused and reusable.

Do not build giant composables containing all application logic.

Keep state management in ViewModels.

Implement previews for important components.

Use realistic sample data in previews so that the visual design can be reviewed before the complete database integration is finished.

The coding agent should produce a cohesive, functioning UI rather than a collection of disconnected mock screens.

---

# 19. Design acceptance criteria

The UI is acceptable when:

1. Opening Memoise immediately presents the chronological timeline.
2. Past and future entries are visible in the same continuous list.
3. Date separators are subtle and readable.
4. The timeline does not resemble a calendar.
5. Quick capture can be opened with one tap from the home-screen widget.
6. An entry can be created with only a title.
7. Entry details feel like an editable document.
8. Preparations and follow-ups are naturally accessible.
9. Checklist items and description content can be converted into entries.
10. Related entries are easy to navigate.
11. Unscheduled and Overdue are separate destinations.
12. Search is globally accessible.
13. Light and dark themes are polished.
14. The UI remains uncluttered even when an entry has many related entries.
15. The application feels like one cohesive personal ledger rather than several separate productivity tools.

---

# 20. Final design direction

Memoise should feel like a quiet, dependable place where the user can put things they don't want to forget.

The timeline is the spine of the application.

Entries are the individual records.

Preparations and follow-ups connect those records.

Checklists and descriptions provide detail.

Notes preserve context.

Search makes everything retrievable.

Unscheduled captures things that have not found their place yet.

Overdue brings unfinished obligations back to attention.

The design must consistently reinforce this mental model.

**Memoise – Personal Ledger**

*Everything has a place.*