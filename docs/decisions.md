# Decisions Log

Append new decisions at the end; do not rewrite earlier entries.

## D-001 — One linked entry per description paragraph

Keep one linked entry per description paragraph for now. Revisit whether a paragraph should support multiple linked entries later. Checklist items remain single-entry conversions.

## D-002 — Approximate reminder delivery

Use WorkManager for the initial custom date/time reminders. Revisit exact alarms and relative reminders later.

## D-003 — Recurrence occurrence generation

Store each occurrence as an independent ledger entry and generate dates through a rolling 12-month window. Stopping a series preserves entries already created.

## D-004 — Recurring reminder inheritance

Apply the parent’s reminder timing pattern to future occurrences. Keep each occurrence’s reminders independently editable.
