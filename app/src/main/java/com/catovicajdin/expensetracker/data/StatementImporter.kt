package com.catovicajdin.expensetracker.data

import android.content.Context
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.data.entity.TransactionTagCrossRef
import org.json.JSONObject

/**
 * Loads a month of transactions transcribed from a bank statement, for spending that predates the
 * app or arrived while the notification listener was off.
 *
 * Each statement is bundled as an asset and identified by its own source string, which is both the
 * ledger's "Statement" label and the guard against importing the same month twice - the whole run
 * is skipped if any row from that source already exists.
 */
object StatementImporter {

    data class Result(val imported: Int, val alreadyPresent: Boolean)

    /**
     * Categories and tags are resolved by name, case-insensitively so a statement naming "Pharmacy"
     * attaches to an existing "pharmacy" rather than quietly creating a near-duplicate beside it -
     * tags are matched exactly and uniquely in SQL, so the folding has to happen here.
     */
    suspend fun import(context: Context, assetName: String): Result {
        val db = AppDatabase.get(context)
        val json = JSONObject(context.assets.open(assetName).bufferedReader().use { it.readText() })
        val source = json.getString("source")
        val currency = json.optString("currency", "BAM")

        if (db.rawNotificationDao().countForPackage(source) > 0) {
            return Result(imported = 0, alreadyPresent = true)
        }

        val categoriesByName = db.categoryDao().allOnce().associateBy { it.name.lowercase() }
        val tagsByName = db.tagDao().allOnce().associateBy { it.name.lowercase() }.toMutableMap()

        val entries = json.getJSONArray("transactions")
        var imported = 0
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val categoryName = entry.getString("category")
            val categoryId = categoriesByName[categoryName.lowercase()]?.id
                ?: db.categoryDao().create(
                    name = categoryName,
                    icon = CategoryEntity.DEFAULT_ICON,
                    colorHex = CategoryEntity.DEFAULT_COLOR,
                )

            // The statement line is kept verbatim, the same way a notification's text is, so an
            // imported row can still be traced back to what it came from.
            val rawId = db.rawNotificationDao().insert(
                RawNotificationEntity(
                    packageName = source,
                    title = entry.optString("date"),
                    body = entry.optString("description"),
                    postedAt = entry.getLong("postedAt"),
                    parseStatus = "PARSED",
                    parserVersion = 0,
                )
            )
            val transactionId = db.transactionDao().insert(
                TransactionEntity(
                    rawNotificationId = rawId,
                    amount = entry.getDouble("amount"),
                    currency = currency,
                    availableBalance = 0.0,
                    postedAt = entry.getLong("postedAt"),
                    categoryId = categoryId,
                )
            )

            val tags = entry.optJSONArray("tags")
            for (t in 0 until (tags?.length() ?: 0)) {
                val name = tags!!.getString(t)
                val tagId = tagsByName[name.lowercase()]?.id
                    ?: db.tagDao().getOrCreate(name).also { newId ->
                        tagsByName[name.lowercase()] = com.catovicajdin.expensetracker.data.entity.TagEntity(newId, name)
                    }
                db.tagDao().addTagToTransaction(TransactionTagCrossRef(transactionId, tagId))
            }
            imported++
        }
        return Result(imported = imported, alreadyPresent = false)
    }
}
