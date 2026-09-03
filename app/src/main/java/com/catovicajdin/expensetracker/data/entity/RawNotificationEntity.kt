package com.catovicajdin.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Every notification from the bank's package lands here first, unconditionally - parsing never gates this write. */
@Entity(tableName = "raw_notifications")
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    /** PARSED | NEEDS_REVIEW | IGNORED */
    val parseStatus: String,
    val parserVersion: Int,
    val failureReason: String? = null,
)
