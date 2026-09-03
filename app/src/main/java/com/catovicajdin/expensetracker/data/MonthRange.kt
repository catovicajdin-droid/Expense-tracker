package com.catovicajdin.expensetracker.data

import java.time.YearMonth
import java.time.ZoneId

/** yearMonth keys are ISO "YYYY-MM" strings (YearMonth's own toString/parse format). */
object MonthRange {
    fun current(): String = YearMonth.now().toString()

    fun millisRange(yearMonth: String): Pair<Long, Long> {
        val ym = YearMonth.parse(yearMonth)
        val zone = ZoneId.systemDefault()
        val from = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
        return from to to
    }

    fun previous(yearMonth: String): String = YearMonth.parse(yearMonth).minusMonths(1).toString()

    fun next(yearMonth: String): String = YearMonth.parse(yearMonth).plusMonths(1).toString()

    fun displayLabel(yearMonth: String): String {
        val ym = YearMonth.parse(yearMonth)
        val monthName = ym.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$monthName ${ym.year}"
    }
}
