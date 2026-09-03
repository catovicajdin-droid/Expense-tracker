package com.catovicajdin.expensetracker

object Constants {

    /**
     * Best-effort default, NOT verified against the real app — confirm on your own device
     * (Settings -> Apps -> Intesa Mobile -> App info -> Advanced, or
     * `adb shell dumpsys package packages | grep -i intesa` while the phone is connected)
     * and update this constant if it doesn't match.
     */
    const val INTESA_PACKAGE_NAME = "org.ping.intesasanpaolo.elba.mobile"

    const val NOTIFICATION_TITLE_TRANSACTION = "Nova transakcija"

    const val CHANNEL_ID_CATEGORIZE = "categorize_transaction"

    /** Bump this whenever TransactionParser's regex changes, so needs-review rows are traceable to a format break. */
    const val PARSER_VERSION = 1
}
