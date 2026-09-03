package com.catovicajdin.expensetracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.catovicajdin.expensetracker.data.dao.BudgetDao
import com.catovicajdin.expensetracker.data.dao.CategoryDao
import com.catovicajdin.expensetracker.data.dao.RawNotificationDao
import com.catovicajdin.expensetracker.data.dao.TransactionDao
import com.catovicajdin.expensetracker.data.entity.BudgetAlertEntity
import com.catovicajdin.expensetracker.data.entity.CategoryBudgetEntity
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.MonthlyBudgetEntity
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity

@Database(
    entities = [
        RawNotificationEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        CategoryBudgetEntity::class,
        BudgetAlertEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db",
                )
                    .addCallback(SeedDefaultCategories)
                    // No destructive fallback from here on - schema changes now require a real
                    // Migration(oldVersion, newVersion) added below, so existing data survives updates.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
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

        private data class CategorySeed(
            val name: String,
            val isQuickPick: Boolean,
            val sortOrder: Int,
            val colorHex: String,
        )

        private object SeedDefaultCategories : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val defaults = listOf(
                    CategorySeed("Phone Bill", false, 0, "#EF5350"),
                    CategorySeed("Misc", false, 1, "#78909C"),
                    CategorySeed("Food ordering", true, 2, "#FF7043"),
                    CategorySeed("Subscriptions", false, 3, "#5C6BC0"),
                    CategorySeed("Padel", false, 4, "#9CCC65"),
                    CategorySeed("Groceries", true, 5, "#26A69A"),
                    CategorySeed("Coffee", true, 6, "#8D6E63"),
                    CategorySeed("Gas Bill", false, 7, "#42A5F5"),
                    CategorySeed("Parents", false, 8, "#66BB6A"),
                    CategorySeed("Donating", false, 9, "#EC407A"),
                    CategorySeed("Bills", false, 10, "#AB47BC"),
                    CategorySeed("Date nights", false, 11, "#D4E157"),
                    CategorySeed("Pets", false, 12, "#FFA726"),
                    CategorySeed("DM", false, 13, "#FFCA28"),
                )
                defaults.forEach { seed ->
                    val values = ContentValues().apply {
                        put("name", seed.name)
                        put("isQuickPick", seed.isQuickPick)
                        put("sortOrder", seed.sortOrder)
                        put("colorHex", seed.colorHex)
                    }
                    db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
                }
            }
        }
    }
}
