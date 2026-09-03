package com.catovicajdin.expensetracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.catovicajdin.expensetracker.data.dao.CategoryDao
import com.catovicajdin.expensetracker.data.dao.RawNotificationDao
import com.catovicajdin.expensetracker.data.dao.TransactionDao
import com.catovicajdin.expensetracker.data.entity.CategoryEntity
import com.catovicajdin.expensetracker.data.entity.RawNotificationEntity
import com.catovicajdin.expensetracker.data.entity.TransactionEntity

@Database(
    entities = [RawNotificationEntity::class, TransactionEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db",
                ).addCallback(SeedDefaultCategories).build().also { instance = it }
            }

        private object SeedDefaultCategories : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val defaults = listOf(
                    Triple("Groceries", true, 0),
                    Triple("Transport", true, 1),
                    Triple("Dining", true, 2),
                    Triple("Bills", false, 3),
                    Triple("Other", false, 4),
                )
                defaults.forEach { (name, quickPick, order) ->
                    val values = ContentValues().apply {
                        put("name", name)
                        put("isQuickPick", quickPick)
                        put("sortOrder", order)
                    }
                    db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
                }
            }
        }
    }
}
