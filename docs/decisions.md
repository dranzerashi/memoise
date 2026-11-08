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

## D-005 — Recurrence source links

Represent source-to-occurrence links in the existing entry relationship graph using the recurrence relationship type.

## D-006 — Skipping one occurrence

Use the occurrence’s existing cancelled status; keep the entry and its history, and let the rest of the series continue.

## D-007 — Recurrence exceptions

Store skipped and moved occurrence dates under their existing series so generation cannot recreate an overridden date; keep occurrence links in the existing relationship graph.
