package com.catovicajdin.expensetracker.notifications

import android.content.Context
import com.catovicajdin.expensetracker.Constants

/**
 * A bank whose app's push notifications the listener can read. [id] is what's persisted in
 * preferences, so it must never change; [packageName] is what raw_notifications rows record.
 */
enum class Bank(
    val id: String,
    val displayName: String,
    val appName: String,
    val packageName: String,
    val parser: BankParser,
    /**
     * How far back an identical notification counts as a re-delivery rather than a new transaction.
     * Intesa's body carries the resulting balance, so identical text over a week really is the same
     * transaction. Raiffeisen and UniCredit report no balance - two 3.00 BAM coffees at the same
     * place would read identically - so for them only a near-simultaneous repeat (Android
     * redelivering, a group summary) is treated as a duplicate.
     */
    val dedupWindowMillis: Long,
) {
    INTESA(
        id = "intesa",
        displayName = "Intesa Sanpaolo",
        appName = "Intesa Mobile",
        packageName = Constants.INTESA_PACKAGE_NAME,
        parser = IntesaParser,
        dedupWindowMillis = 7L * 24 * 60 * 60 * 1000,
    ),
    UNICREDIT(
        id = "unicredit",
        displayName = "UniCredit",
        appName = "m-ba",
        packageName = Constants.UNICREDIT_PACKAGE_NAME,
        parser = UniCreditParser,
        dedupWindowMillis = 2L * 60 * 1000,
    ),
    RAIFFEISEN(
        id = "raiffeisen",
        displayName = "Raiffeisen",
        appName = "RMB",
        packageName = Constants.RAIFFEISEN_PACKAGE_NAME,
        parser = RaiffeisenParser,
        dedupWindowMillis = 2L * 60 * 1000,
    );

    companion object {
        fun fromPackage(packageName: String): Bank? = entries.find { it.packageName == packageName }
    }
}

/**
 * Which banks the listener reads. Defaults to Intesa alone, so installs from before multi-bank
 * support keep behaving exactly as they did.
 */
object BankPreferences {
    private const val PREFS_NAME = "banks"
    private const val KEY_ENABLED = "enabled_bank_ids"
    private val DEFAULT = setOf(Bank.INTESA)

    fun enabled(context: Context): Set<Bank> {
        val ids = prefs(context).getStringSet(KEY_ENABLED, null) ?: return DEFAULT
        return Bank.entries.filter { it.id in ids }.toSet()
    }

    fun setEnabled(context: Context, banks: Set<Bank>) {
        prefs(context).edit().putStringSet(KEY_ENABLED, banks.map { it.id }.toSet()).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
