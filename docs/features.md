# Memoise features

This document records current product behavior. User-facing explanations will be added in the app’s Help section later.

## Recurring entries

- A dated entry can repeat daily, weekly, monthly, or yearly.
- Starting a repeat opens one setup dialog for frequency and optional end date. Occurrences are generated only after **Start repeating** is chosen.
- Memoise stores occurrences as independent entries and keeps a rolling window of up to 12 months of future dates.
- A series can have an end date. After it, Memoise adds no new repeats; entries already in the ledger stay, even when scheduled after that date. Choosing **Never** removes the end date. Changing an existing end date is applied with **Apply changes**.
- Each occurrence links to its source through the same entry relationship graph used by preparations, follow-ups, and related entries. Open an occurrence and use **Source: _entry title_** under **Repeat** to navigate to the series source. The source lists its generated entries under **Occurrences**.
- Stopping a series preserves its source and all occurrences already created. No further occurrences are generated.
- Completing or cancelling an occurrence affects only that entry; the series continues.
- **Skip this one** marks a future occurrence cancelled and leaves the other repeats unchanged.
- Skipped entries and one-off date changes are kept as exceptions to the series, so Memoise will not recreate the original occurrence after it is skipped, moved, or removed. Moving an occurrence back to its original date clears that exception.
- Repeat exceptions are included in both JSON exports and full database backups.
- Editing an occurrence’s title, date, status, notes, or relationships does not change other occurrences. Editing the source does not rewrite occurrences already created.
- When the first entry is removed, the user can remove only that entry (the oldest remaining occurrence becomes the source) or remove all repeats and their saved details.

## Reminder inheritance

- A reminder can use a custom date/time or a relative pattern: one week before, three days before, one day before, or on the day, at a chosen time.
- Relative reminders follow the entry’s scheduled date. A relative pattern can be saved on an active series even if its reminder time for the first occurrence has passed; it applies to future occurrences.
- Recurring occurrences inherit the source’s reminder patterns. A custom reminder on the source is carried forward using its day offset and time of day.
- Changing, disabling, or removing a source reminder updates, disables, or removes its inherited copies on occurrences. Editing or toggling an occurrence’s reminder detaches it so it can be managed independently.
- Stopping a series leaves reminders on existing occurrences in place.
- Notification delivery is approximate and requires Android notification permission.

## Accessibility

- Symbol-only controls have spoken labels, and screen readers can invoke the existing description and checklist actions without relying on a long press.
- Section titles are exposed as headings. Checklist controls announce the item they change, and date, status, and search filters announce their purpose and current value.
- Custom clickable rows have at least a 48 dp touch area. Status is also described with text or a completion mark, not colour alone.
- Text uses scalable units. Search filters and the entry status/date-meaning pair stack at large system text sizes or on narrow screens.
- Android’s high-contrast-text setting selects a stronger light palette; the normal almanac palette remains in use otherwise. Dark-theme text colors already have strong contrast.

## Appearance

- Choose **System default**, **Light**, or **Dark** in Settings. The choice is saved on this device and applied across the app. System default follows Android’s current appearance setting.

## Help

- Settings includes a short Help screen explaining date meanings, repeat setup and removal, reminder inheritance, and backups in plain language.

## Search

- The Search screen keeps the five most recent submitted searches on this device. Select one to search again, or clear the list.
- Matching words are emphasized in entry titles and in the matching content excerpt.

## About

- Settings shows the Memoise name, short product description, app version, and a note that the ledger is stored locally unless the user exports a backup.
