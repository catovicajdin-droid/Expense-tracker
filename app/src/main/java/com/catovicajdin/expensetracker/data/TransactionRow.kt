package com.catovicajdin.expensetracker.data

import androidx.room.Embedded
import com.catovicajdin.expensetracker.data.entity.TransactionEntity

/** A transaction alongside the package name of the notification it came from ("manual" for hand-entered rows). */
data class TransactionRow(
    @Embedded val transaction: TransactionEntity,
    val source: String,
)
