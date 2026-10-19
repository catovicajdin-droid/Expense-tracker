package com.catovicajdin.expensetracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.catovicajdin.expensetracker.data.dao.BudgetDao
import com.catovicajdin.expensetracker.data.dao.CategoryDao
import com.catovicajdin.expensetracker.data.dao.RawNotificationDao
import com.catovicajdin.expensetracker.data.dao.TagDao
import com.catovicajdin.expensetracker.data.dao.TransactionDao
import com.catovicajdin.expensetracker.data.entity.BudgetAlertEntity
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TagEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity
import com.catovicajdin.expensetracker.data.entity.TransactionTagCrossRef

/** Bump with every new Migration; a backup from a newer schema than this cannot be restored. */
const val DATABASE_VERSION = 5

@Database(
    entities = [
        RawNotificationEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        CategoryBudgetEntity::class,
        BudgetAlertEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        const val FILE_NAME = "expense_tracker.db"

        /** Closes and forgets the open database, so a restore can replace its file. */
        fun close() = synchronized(this) {
            instance?.close()
            instance = null
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    FILE_NAME,
                )
                    .addCallback(SeedDefaultCategories)
                    // No destructive fallback from here on - schema changes now require a real
                    // Migration(oldVersion, newVersion) added below, so existing data survives updates.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { db ->
                        instance = db
                        // The category-color refresh that used to run here is now a one-off inside
                        // MIGRATION_4_5: re-applying it on every start would silently undo a color
                        // the user picked themselves, which became possible once categories were
                        // editable in-app.
                        CoroutineScope(Dispatchers.IO).launch {
                            // Data-only cleanup: tags entered with a leading "#" before entry
                            // stripped it end up double-hashed everywhere they're displayed.
                            db.tagDao().stripLeadingHashFromNames()
                        }
                    }
            }

        /** v1 -> v2: categories gained a colorHex column - backfill each seeded category's real color. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#607D8B'")
                val colors = mapOf(
                    "Phone Bill" to "#EF5350",
                    "Misc" to "#78909C",
                    "Food ordering" to "#FF7043",
                    "Subscriptions" to "#5C6BC0",
                    "Padel" to "#9CCC65",
                    "Groceries" to "#26A69A",
                    "Coffee" to "#8D6E63",
                    "Gas Bill" to "#42A5F5",
                    "Parents" to "#66BB6A",
                    "Donating" to "#EC407A",
                    "Bills" to "#AB47BC",
                    "Date nights" to "#D4E157",
                    "Pets" to "#FFA726",
                    "DM" to "#FFCA28",
                )
                colors.forEach { (name, color) ->
                    db.execSQL("UPDATE categories SET colorHex = ? WHERE name = ?", arrayOf(color, name))
                }
            }
        }

        /** v2 -> v3: added the three budget tables. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_budgets` (
                        `yearMonth` TEXT NOT NULL,
                        `totalBudget` REAL NOT NULL,
                        PRIMARY KEY(`yearMonth`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `category_budgets` (
                        `yearMonth` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        PRIMARY KEY(`yearMonth`, `categoryId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budget_alerts` (
                        `yearMonth` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `threshold` INTEGER NOT NULL,
                        PRIMARY KEY(`yearMonth`, `categoryId`, `threshold`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** v3 -> v4: transactions gained notes, plus a tags table and a transaction<->tag join table. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_tags` (
                        `transactionId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`, `tagId`),
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v4 -> v5: categories gained an `icon` column, so a category created or renamed in-app can
         * carry its own glyph instead of being looked up by name in CategoryIcons. Backfills the
         * seeded categories' icons, and takes over the color sync that used to re-run on every app
         * start (which would have overwritten user-picked colors now that they're editable).
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT '${CategoryEntity.DEFAULT_ICON}'"
                )
                CategoryIcons.byName.forEach { (name, icon) ->
                    db.execSQL("UPDATE categories SET icon = ? WHERE name = ?", arrayOf(icon, name))
                }
                CategoryColors.byName.forEach { (name, color) ->
                    db.execSQL("UPDATE categories SET colorHex = ? WHERE name = ?", arrayOf(color, name))
                }
            }
        }

        private data class CategorySeed(
            val name: String,
            val isQuickPick: Boolean,
            val sortOrder: Int,
        )

        private object SeedDefaultCategories : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val defaults = listOf(
                    CategorySeed("Groceries", true, 0),
                    CategorySeed("Bills", true, 1),
                    CategorySeed("Food", true, 2),
                    CategorySeed("Transport", false, 3),
                    CategorySeed("Subscriptions", false, 4),
                    CategorySeed("Misc", false, 5),
                )
                defaults.forEach { seed ->
                    val values = ContentValues().apply {
                        put("name", seed.name)
                        put("isQuickPick", seed.isQuickPick)
                        put("sortOrder", seed.sortOrder)
                        put("colorHex", CategoryColors.byName[seed.name])
                        put("icon", CategoryIcons.byName[seed.name] ?: CategoryEntity.DEFAULT_ICON)
                    }
                    db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
                }
            }
        }
    }
}
