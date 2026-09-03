# Expense Tracker

Android app that auto-tracks Intesa Sanpaolo BiH card transactions by reading the bank's own
push notifications on-device, then lets you categorize each one with a single tap — no bank
credentials, no scraping, no server involved.

## How it works

1. `IntesaNotificationListenerService` listens for notifications posted by the Intesa Mobile app.
2. Every notification is written to the local `raw_notifications` table **unconditionally** — before
   any parsing happens — so a failed or changed format never means a lost transaction.
3. `TransactionParser` tries to extract amount/balance from "Nova transakcija" notifications. On a
   match, a `transactions` row is created. On a mismatch (wrong wording, unexpected currency, bad
   number), the raw row is flagged `NEEDS_REVIEW` with the reason attached, visible in the app's
   "Needs review" screen.
4. On a successful parse, a notification pops up with one-tap buttons for your top categories, plus
   "More…" which opens the app to the full category list for that one transaction. If a past
   transaction had the exact same amount and was already categorized, that category is suggested
   first (marked with a ★), both in the notification and at the top of the in-app category list.
5. The transaction list supports combinable filters (category, date range, min/max amount) — all
   selected filters apply together (AND), not one-at-a-time. Tapping a transaction's category icon
   opens a dropdown to reassign it.
6. The **Budget** screen (from the transaction list) tracks a monthly overall budget plus per-category
   budgets, auto-calculated against real spend as transactions come in - browse other months with the
   Prev/Next controls. A category with no budget set yet, when a prior month had one, offers a
   "use last month's" shortcut. A "Where your money goes" breakdown ranks categories by spend with
   percentages. Crossing 50%/75% of the overall or any category budget fires a one-time-per-month
   notification (`BudgetAlerts`).
7. The **"+ Add"** button on the transaction list opens a dialog for manually-entered transactions
   (cash, anything the bank never pushes a notification for) - they flow through the same pipeline
   as auto-tracked ones (same table, same budget/alert logic), just tagged as coming from `"manual"`
   instead of the bank's package.
8. Every transaction (auto-tracked or manual) has an **"Edit"** action - change its date via a
   calendar picker (keeping the existing time of day), and attach/detach tags via toggleable chips,
   with a comma-separated field for adding several new tags at once. The "+ Add" dialog for manual
   entries has the same tag picker built in, so tags can be assigned at creation time instead of a
   follow-up edit. Tapping the × on a tag chip deletes that tag globally (with a confirmation, since
   it removes it from every transaction that has it) - `TagEntity`/`transaction_tags` use
   `ON DELETE CASCADE`, so no separate cleanup is needed.  A "Delete" action removes the transaction
   itself (with a confirmation prompt); the originating `raw_notifications` row stays for
   auto-tracked ones. The filter bar supports multi-tag filtering with a toggleable ANY/ALL match
   mode, combining with the category/date/amount filters the same AND way.
   (Note: `TransactionEntity.notes` and the schema column both still exist but are intentionally
   unused - tags replaced free-text notes, and dropping the column would need a riskier
   table-recreation migration for no real benefit.)
9. The transaction list opens on a summary card: this month's total spend plus a two-bar
   comparison against last month (`MonthComparisonBarChart`), both custom-drawn on `Canvas` rather
   than a third-party charting library - the app already hit two version-mismatch build breaks
   this session from external Compose APIs, so new chart code stays dependency-free. The Budget
   screen's category breakdown is a donut chart with a legend (`CategoryDonutChart`) instead of a
   plain progress-bar list. The 14 category colors were regenerated and validated against the
   dataviz skill's CVD/contrast checks (`scripts/validate_palette.js`, adjacent-pair mode, dark
   surface) - lightness band and chroma floor both pass; the one remaining borderline pair is
   covered by the fact that category color is never shown without its name and letter avatar
   alongside it. New installs get the palette from `CategoryColors.kt`; existing installs get it
   backfilled at startup by name (a plain data UPDATE, not a schema migration).

## Before you build this

- **Confirm the live notification text matches the parser.** The sample used to write
  `TransactionParser` came from the bank app's own in-app "Notifications Overview" history screen,
  not necessarily the exact text of the system-level push notification the phone receives — those
  can differ (e.g. truncation). Trigger a real transaction, check what actually lands in the
  notification shade, and compare it against the regex in `TransactionParser.kt` before relying on
  this. A mismatch isn't fatal — it'll just land in "Needs review" — but is worth confirming early.

## Setup

1. Open the project root in Android Studio (this repo doesn't commit a Gradle wrapper jar —
   Android Studio will offer to generate one, or run `gradle wrapper --gradle-version 8.7` once you
   have Gradle installed locally).
2. Run the app, grant the notification permission prompt (Android 13+).
3. From the in-app onboarding screen, grant **Notification Access** (this is a special permission
   only settable from system Settings — the app deep-links you there).
4. Trigger a card transaction and confirm it shows up categorized end to end.

## Known limitations (by design, for now)

- No merchant name — the notification only carries amount + running balance, so categorization is
  manual/one-tap rather than automatic (eased by the amount-match suggestion above).
- Android only — iOS does not allow any app to read another app's notification content.
- Coupled to the bank's exact notification wording; a change on their end routes to "Needs review"
  instead of silently mis-parsing (see `TransactionParser` and `NotificationRepository`).
- The database no longer uses `fallbackToDestructiveMigration()` — any schema change from here on
  (version 3 onward) needs a real `Migration(oldVersion, newVersion)` added to `AppDatabase`'s
  builder, or the app will crash on update instead of silently wiping data. Versions 1-2 were wiped
  during early development before this switch; that's already reflected in the version 3 baseline.
