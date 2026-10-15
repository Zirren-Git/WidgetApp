package com.zirren.tessera.core.tools

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

data class DateDifference(
    val start: LocalDate,
    val end: LocalDate,
    val totalDays: Long,
    val businessDays: Long,
    val weekendDays: Long,
    val weeks: Long,
    val remainderDays: Long,
    val years: Int,
    val months: Int,
    val days: Int,
    val isPast: Boolean,
)

object DateSpanCalculator {

    fun difference(start: LocalDate, end: LocalDate): DateDifference {
        val isPast = end.isBefore(start)
        val earlier = if (isPast) end else start
        val later = if (isPast) start else end

        val totalDays = ChronoUnit.DAYS.between(earlier, later)
        val weeks = totalDays / 7L
        val remainderDays = totalDays % 7L

        val period = Period.between(earlier, later)

        var bDays = 0L
        var wDays = 0L
        var cur = earlier
        while (cur.isBefore(later)) {
            val dow = cur.dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                wDays++
            } else {
                bDays++
            }
            cur = cur.plusDays(1)
        }

        return DateDifference(
            start = start,
            end = end,
            totalDays = totalDays,
            businessDays = bDays,
            weekendDays = wDays,
            weeks = weeks,
            remainderDays = remainderDays,
            years = period.years,
            months = period.months,
            days = period.days,
            isPast = isPast,
        )
    }

    fun addDays(start: LocalDate, days: Long): LocalDate = start.plusDays(days)
}
