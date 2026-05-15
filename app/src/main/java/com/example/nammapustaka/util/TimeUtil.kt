package com.example.nammapustaka.util

import java.util.Calendar

object TimeUtil {
    fun currentMonthRangeMillis(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endExclusive = cal.timeInMillis
        return start to endExclusive
    }

    data class MonthRange(val start: Long, val endExclusive: Long, val label: String)

    fun monthRangeMillis(monthsAgo: Int): MonthRange {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        val label =
            "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())} ${cal.get(Calendar.YEAR)}"
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endExclusive = cal.timeInMillis
        return MonthRange(start, endExclusive, label)
    }

    fun addDaysMillis(base: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = base
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }

    fun defaultDueMillis(issueMillis: Long, loanDays: Int = 14): Long = addDaysMillis(issueMillis, loanDays)
}
