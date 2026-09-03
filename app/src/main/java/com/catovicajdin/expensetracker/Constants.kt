package com.catovicajdin.expensetracker

object Constants {

    /** Confirmed against the Play Store listing for the Intesa Mobile BiH app. */
    const val INTESA_PACKAGE_NAME = "org.ping.intesasanpaolo.elba.mobile"

    const val NOTIFICATION_TITLE_TRANSACTION = "Nova transakcija"

    const val CHANNEL_ID_CATEGORIZE = "categorize_transaction"

    /** Bump this whenever TransactionParser's regex changes, so needs-review rows are traceable to a format break. */
    const val PARSER_VERSION = 1
}
