package com.catovicajdin.expensetracker.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.catovicajdin.expensetracker.data.AppDatabase
import com.catovicajdin.expensetracker.data.DATABASE_VERSION
import com.catovicajdin.expensetracker.ui.components.sourceLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Getting data out of the app and back in, all through files the user picks - nothing leaves the
 * phone unless they send the file somewhere themselves.
 *
 * The CSV is for reading in a spreadsheet and is one-way. The backup is the whole database file
 * (categories, budgets, tags, imports, raw notifications), so restoring it gives back exactly what
 * was there - including on a fresh install, or in the other build of the app.
 */
object DataTransfer {

    /** Writes every transaction as CSV and returns how many rows went out. */
    suspend fun exportCsv(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val rows = AppDatabase.get(context).transactionDao().exportRows()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the file")
        stream.bufferedWriter().use { out ->
            // A byte-order mark so Excel reads č/ć/š/ž as UTF-8 instead of mangling them.
            out.write("﻿")
            out.write("Date,Amount,Currency,Category,Tags,Source,Details\r\n")
            rows.forEach { row ->
                val cells = listOf(
                    dateFormat.format(Date(row.postedAt)),
                    "%.2f".format(Locale.ROOT, row.amount),
                    row.currency,
                    row.category.orEmpty(),
                    row.tags.orEmpty(),
                    sourceLabel(row.source),
                    row.details,
                )
                out.write(cells.joinToString(",") { csvCell(it) })
                out.write("\r\n")
            }
        }
        rows.size
    }

    /** Copies the database file out, after folding the write-ahead log into it so nothing recent is missed. */
    suspend fun backup(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open the file")
        stream.use { out -> context.getDatabasePath(AppDatabase.FILE_NAME).inputStream().use { it.copyTo(out) } }
    }

    sealed class RestoreCheck {
        /** The file is a backup this version of the app can open; [transactionCount] is what it holds. */
        data class Ok(val staged: File, val transactionCount: Int) : RestoreCheck()
        data class Rejected(val reason: String) : RestoreCheck()
    }

    /**
     * Copies the picked file aside and checks it before anything is replaced: it must be one of this
     * app's databases, and not from a newer version of the app than this one (an older one is fine,
     * the usual migrations bring it up to date when it is opened).
     */
    suspend fun checkBackup(context: Context, uri: Uri): RestoreCheck = withContext(Dispatchers.IO) {
        val staged = File(context.cacheDir, "restore.db")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext RestoreCheck.Rejected("Could not open the file.")
            input.use { inp -> staged.outputStream().use { inp.copyTo(it) } }
            SQLiteDatabase.openDatabase(staged.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                if (db.version > DATABASE_VERSION) {
                    return@withContext RestoreCheck.Rejected(
                        "This backup is from a newer version of the app. Update the app, then restore it.",
                    )
                }
                val count = db.rawQuery("SELECT COUNT(*) FROM transactions", null).use { c ->
                    c.moveToFirst()
                    c.getInt(0)
                }
                RestoreCheck.Ok(staged, count)
            }
        } catch (e: Exception) {
            staged.delete()
            RestoreCheck.Rejected("This file isn't an Expense Tracker backup.")
        }
    }

    /**
     * Swaps the checked backup in for the current database and restarts the app, since every open
     * screen and the notification listener still hold the old database.
     */
    fun restore(context: Context, staged: File) {
        AppDatabase.close()
        val target = context.getDatabasePath(AppDatabase.FILE_NAME)
        File(target.path + "-wal").delete()
        File(target.path + "-shm").delete()
        staged.copyTo(target, overwrite = true)
        staged.delete()

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(Intent.makeRestartActivityTask(launch.component))
        exitProcess(0)
    }

    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
