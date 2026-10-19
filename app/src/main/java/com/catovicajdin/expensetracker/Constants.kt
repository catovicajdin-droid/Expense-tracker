package com.catovicajdin.expensetracker

object Constants {

    /** Confirmed against the Play Store listing for the Intesa Mobile BiH app. */
    const val INTESA_PACKAGE_NAME = "org.ping.intesasanpaolo.elba.mobile"

    /** UniCredit Bank d.d. Mostar's "m-ba" app, as linked from unicredit.ba. */
    const val UNICREDIT_PACKAGE_NAME = "hr.asseco.android.mbhZaba"

    /** Raiffeisen Bank BiH's "RMB Raiffeisen Mobile Banking" app, per its Play Store listing. */
    const val RAIFFEISEN_PACKAGE_NAME = "ba.raiffeisenbank.mobile.prod"

    const val NOTIFICATION_TITLE_TRANSACTION = "Nova transakcija"

    const val CHANNEL_ID_CATEGORIZE = "categorize_transaction"
    const val CHANNEL_ID_BUDGET_ALERT = "budget_alert"

    /** Bump this whenever a bank parser's regex changes, so needs-review rows are traceable to a format break. */
    const val PARSER_VERSION = 3
}
