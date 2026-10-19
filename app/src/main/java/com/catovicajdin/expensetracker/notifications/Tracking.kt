package com.catovicajdin.expensetracker.notifications

import android.content.Context

/** How a user chose to track: from bank notifications, or by entering transactions themselves. */
enum class TrackingMode { AUTOMATIC, MANUAL }

/**
 * Whether the first-run welcome has been finished, and which way the user chose to track. In manual
 * mode the listener reads nothing, even with notification access still granted.
 */
object TrackingPreferences {
    private const val PREFS_NAME = "welcome"
    private const val KEY_DONE = "done"
    private const val KEY_MODE = "tracking_mode"

    fun isDone(context: Context): Boolean = prefs(context).getBoolean(KEY_DONE, false)

    fun mode(context: Context): TrackingMode =
        prefs(context).getString(KEY_MODE, null)?.let { runCatching { TrackingMode.valueOf(it) }.getOrNull() }
            ?: TrackingMode.AUTOMATIC

    fun finish(context: Context, mode: TrackingMode) {
        prefs(context).edit().putBoolean(KEY_DONE, true).putString(KEY_MODE, mode.name).apply()
    }

    fun setMode(context: Context, mode: TrackingMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
