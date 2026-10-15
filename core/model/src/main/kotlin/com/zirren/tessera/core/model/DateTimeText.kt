package com.zirren.tessera.core.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats dates and times the way the user's phone does.
 * On Android this is backed by the system's locale-aware patterns and the 12/24-hour setting;
 * [PatternDateTimeText] is the plain-JVM version used in tests.
 */
interface DateTimeText {
    /** "09:30" or "9:30 AM". */
    fun time(time: LocalTime): String

    /** Hour only, for hourly forecasts: "14" or "2 PM". */
    fun hour(time: LocalTime): String

    /** "Friday". */
    fun weekday(date: LocalDate): String

    /** "Fri". */
    fun shortWeekday(date: LocalDate): String

    /** "17 Oct" (or "Oct 17", depending on locale). */
    fun shortDate(date: LocalDate): String

    /** "Friday 17 October". */
    fun longDate(date: LocalDate): String
}

class PatternDateTimeText(
    locale: Locale = Locale.UK,
    use24Hour: Boolean = true,
    shortDatePattern: String = "d MMM",
    longDatePattern: String = "EEEE d MMMM",
) : DateTimeText {
    private val timeFormat = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
    private val hourFormat = DateTimeFormatter.ofPattern(if (use24Hour) "HH" else "h a", locale)
    private val weekdayFormat = DateTimeFormatter.ofPattern("EEEE", locale)
    private val shortWeekdayFormat = DateTimeFormatter.ofPattern("EEE", locale)
    private val shortDateFormat = DateTimeFormatter.ofPattern(shortDatePattern, locale)
    private val longDateFormat = DateTimeFormatter.ofPattern(longDatePattern, locale)

    override fun time(time: LocalTime): String = timeFormat.format(time)
    override fun hour(time: LocalTime): String = hourFormat.format(time)
    override fun weekday(date: LocalDate): String = weekdayFormat.format(date)
    override fun shortWeekday(date: LocalDate): String = shortWeekdayFormat.format(date)
    override fun shortDate(date: LocalDate): String = shortDateFormat.format(date)
    override fun longDate(date: LocalDate): String = longDateFormat.format(date)
}
