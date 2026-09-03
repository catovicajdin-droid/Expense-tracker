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
   "More…" which opens the app to the full category list for that one transaction.

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
  manual/one-tap rather than automatic.
- Android only — iOS does not allow any app to read another app's notification content.
- Coupled to the bank's exact notification wording; a change on their end routes to "Needs review"
  instead of silently mis-parsing (see `TransactionParser` and `NotificationRepository`).
