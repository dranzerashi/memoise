# Memoise features

This document records current product behavior. User-facing explanations will be added in the app’s Help section later.

## Recurring entries

- A dated entry can repeat daily, weekly, monthly, or yearly.
- Memoise stores occurrences as independent entries and keeps a rolling window of up to 12 months of future dates.
- Each occurrence links to its source through the same entry relationship graph used by preparations, follow-ups, and related entries. Open an occurrence and use **Source: _entry title_** under **Repeat** to navigate to the series source. The source lists its generated entries under **Occurrences**.
- Stopping a series preserves its source and all occurrences already created. No further occurrences are generated.
- Completing or cancelling an occurrence affects only that entry; the series continues.
- **Skip this one** marks a future occurrence cancelled and leaves the other repeats unchanged.
- Editing an occurrence’s title, date, status, notes, or relationships does not change other occurrences. Editing the source does not rewrite occurrences already created.
- If the source entry is deleted, the oldest remaining occurrence becomes the source and the active series continues. If there are no remaining occurrences, the series stops.

## Reminder inheritance

- A reminder can use a custom date/time or a relative pattern: one week before, three days before, one day before, or on the day, at a chosen time.
- Relative reminders follow the entry’s scheduled date. A relative pattern can be saved on an active series even if its reminder time for the first occurrence has passed; it applies to future occurrences.
- Recurring occurrences inherit the source’s reminder patterns. A custom reminder on the source is carried forward using its day offset and time of day.
- Changing, disabling, or removing a source reminder updates, disables, or removes its inherited copies on occurrences. Editing or toggling an occurrence’s reminder detaches it so it can be managed independently.
- Stopping a series leaves reminders on existing occurrences in place.
- Notification delivery is approximate and requires Android notification permission.
