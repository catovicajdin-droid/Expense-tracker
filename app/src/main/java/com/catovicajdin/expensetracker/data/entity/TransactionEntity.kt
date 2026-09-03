package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RawNotificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawNotificationId"],
        )
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawNotificationId: Long,
    val amount: Double,
    val currency: String,
    val availableBalance: Double,
    val postedAt: Long,
    val categoryId: Long? = null,
    val notes: String? = null,
)
