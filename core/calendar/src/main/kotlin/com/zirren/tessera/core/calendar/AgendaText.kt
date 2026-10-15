package com.zirren.tessera.core.calendar

import com.zirren.tessera.core.model.DateTimeText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The words used for the agenda. English defaults; Android can build this from string resources. */
data class AgendaPhrases(
    val today: String = "Today",
    val tomorrow: String = "Tomorrow",
    val allDay: String = "All day",
    val now: String = "Now",
    val untilTime: String = "until %s",
    val inMinutes: String = "in %d min",
    val dayOfSpan: String = "Day %1\$d of %2\$d",
    val nothingToday: String = "Nothing else today",
    val nothingAhead: String = "Nothing in the next %d days",
    val nothingTomorrow: String = "Nothing tomorrow",
)

/** Formats agenda rows. Absolute times by default, because they are never out of date. */
class AgendaText(
    private val dateTime: DateTimeText,
    private val phrases: AgendaPhrases = AgendaPhrases(),
) {
    /** "Today", "Tomorrow", or "Friday 17 October". */
    fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
        today -> phrases.today
        today.plusDays(1) -> phrases.tomorrow
        else -> dateTime.longDate(date)
    }

    /** The time column of a row: "10:00–10:15", "All day", "Day 2 of 3", "22:00–Sat 01:00". */
    fun timeRange(item: AgendaItem, zone: ZoneId): String {
        val instance = item.instance
        if (instance.allDay) {
            return if (item.spanDays > 1) phrases.dayOfSpan.format(item.dayIndex, item.spanDays) else phrases.allDay
        }
        val start = instance.begin.atZone(zone)
        val end = instance.end.atZone(zone)
        val startDay = start.toLocalDate()
        val endDay = instance.lastDay(zone)
        return when {
            item.day == startDay && endDay == startDay ->
                dateTime.time(start.toLocalTime()) + "\u2013" + dateTime.time(end.toLocalTime())
            item.day == startDay ->
                dateTime.time(start.toLocalTime()) + "\u2013" + dateTime.shortWeekday(endDay) + " " + dateTime.time(end.toLocalTime())
            else -> {
                val label = phrases.dayOfSpan.format(item.dayIndex, item.spanDays)
                if (item.day == endDay) label + " · " + phrases.untilTime.format(dateTime.time(end.toLocalTime())) else label
            }
        }
    }

    /**
     * Short "when" for a next-event line.
     * Ongoing: "Now · until 10:15". Today: "10:00". Tomorrow: "Tomorrow 10:00". This week: "Fri 10:00". Later: "17 Oct 10:00".
     * With [relativeMinutes], events within the hour read "in 25 min" (5-minute steps; see AgendaEngine.nextChange).
     */
    fun whenLabel(item: AgendaItem, now: Instant, zone: ZoneId, relativeMinutes: Boolean = false): String {
        val instance = item.instance
        val today = now.atZone(zone).toLocalDate()
        if (item.isOngoing) {
            return if (instance.allDay) {
                if (item.spanDays > 1) phrases.dayOfSpan.format(item.dayIndex, item.spanDays) else phrases.allDay
            } else {
                phrases.now + " · " + phrases.untilTime.format(dateTime.time(instance.end.atZone(zone).toLocalTime()))
            }
        }
        if (instance.allDay) {
            return when (item.day) {
                today -> phrases.allDay
                today.plusDays(1) -> phrases.tomorrow
                else -> dateTime.shortWeekday(item.day)
            }
        }
        val secondsLeft = Duration.between(now, instance.begin).seconds
        if (relativeMinutes && secondsLeft in 1..3600) {
            return phrases.inMinutes.format(AgendaEngine.roundUpToFive(secondsLeft))
        }
        val start = instance.begin.atZone(zone)
        val time = dateTime.time(start.toLocalTime())
        val day = start.toLocalDate()
        return when {
            day == today -> time
            day == today.plusDays(1) -> phrases.tomorrow + " " + time
            day.isBefore(today.plusDays(7)) -> dateTime.shortWeekday(day) + " " + time
            else -> dateTime.shortDate(day) + " " + time
        }
    }

    /** Empty-state line, e.g. "Nothing else today" or "Nothing in the next 7 days". */
    fun emptyLine(agenda: Agenda): String =
        if (agenda.isEmpty) phrases.nothingAhead.format(agenda.daysAhead) else phrases.nothingToday
}
