package com.zirren.tessera.core.calendar

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Turns raw calendar instances into what a widget lists, plus when that list next changes. */
object AgendaEngine {

    fun build(
        instances: List<EventInstance>,
        calendars: List<CalendarInfo>?,
        settings: AgendaSettings,
        now: Instant,
        zone: ZoneId,
    ): Agenda {
        val today = now.atZone(zone).toLocalDate()
        val lastDayShown = today.plusDays(settings.daysAhead.toLong() - 1)
        val visibleCalendarIds = calendars?.filter { it.isVisible }?.map { it.id }?.toSet()

        val candidates = instances.asSequence()
            .filter { instance ->
                when {
                    settings.calendarIds != null -> instance.calendarId in settings.calendarIds
                    visibleCalendarIds != null -> instance.calendarId in visibleCalendarIds
                    else -> true
                }
            }
            .filter { !(settings.hideDeclined && it.attendance == AttendanceStatus.DECLINED) }
            .filter { settings.includeAllDay || !it.allDay }
            .filter { it.localEnd(zone).isAfter(now) }
            .filter { !it.firstDay(zone).isAfter(lastDayShown) }
            .toList()
            .let { if (settings.mergeDuplicates) dedupe(it) else it }

        val items = candidates.map { instance ->
            val first = instance.firstDay(zone)
            val last = instance.lastDay(zone)
            val listedDay = maxOf(first, today)
            val showTitle = settings.privacy == TitlePrivacy.SHOW_TITLES
            AgendaItem(
                instance = instance,
                day = listedDay,
                title = if (!showTitle) settings.busyLabel else instance.title?.trim()?.takeIf { it.isNotEmpty() } ?: settings.untitledLabel,
                location = if (showTitle) instance.location?.trim()?.takeIf { it.isNotEmpty() } else null,
                isOngoing = !instance.localStart(zone).isAfter(now) && instance.localEnd(zone).isAfter(now),
                spanDays = (ChronoUnit.DAYS.between(first, last) + 1).toInt(),
                dayIndex = (ChronoUnit.DAYS.between(first, listedDay) + 1).toInt(),
            )
        }.sortedWith(
            compareBy<AgendaItem> { it.day }
                .thenBy { !it.instance.allDay }
                .thenBy { it.instance.begin }
                .thenBy { it.title.lowercase() },
        )

        val days = items.groupBy { it.day }.toSortedMap().map { (date, dayItems) -> AgendaDay(date, dayItems) }

        val timed = items.filter { !it.instance.allDay }.sortedBy { it.instance.begin }
        val nextUp = timed.firstOrNull { it.instance.end.isAfter(now) }
            ?: items.firstOrNull { it.instance.allDay && it.day == today }

        return Agenda(days, nextUp, now, zone, today, settings.daysAhead)
    }

    /**
     * The next moment the agenda would look different: an event starting or ending, midnight,
     * or (when [relativeMinutes] is on) the next 5-minute step of "in 25 min".
     * Never earlier than 30 seconds from [now].
     */
    fun nextChange(agenda: Agenda, now: Instant, relativeMinutes: Boolean = false): Instant {
        val zone = agenda.zone
        val candidates = mutableListOf<Instant>()
        candidates += now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        for (item in agenda.items) {
            val start = item.instance.localStart(zone)
            val end = item.instance.localEnd(zone)
            if (start.isAfter(now)) candidates += start
            if (end.isAfter(now)) candidates += end
        }
        if (relativeMinutes) {
            agenda.nextUp?.takeIf { !it.instance.allDay && it.instance.begin.isAfter(now) }?.let { next ->
                relativeStepBoundary(next.instance.begin, now)?.let(candidates::add)
            }
        }
        val earliest = candidates.minOrNull() ?: now.plus(Duration.ofHours(1))
        return maxOf(earliest, now.plusSeconds(30))
    }

    /** "in 25 min" is shown in 5-minute steps (rounded up); returns when the shown number next drops. */
    internal fun relativeStepBoundary(start: Instant, now: Instant): Instant? {
        val secondsLeft = Duration.between(now, start).seconds
        if (secondsLeft <= 0 || secondsLeft > 60 * 60) return null
        val shownMinutes = roundUpToFive(secondsLeft)
        val nextShown = shownMinutes - 5
        return if (nextShown <= 0) start else start.minusSeconds(nextShown * 60L)
    }

    internal fun roundUpToFive(secondsLeft: Long): Long {
        val minutes = (secondsLeft + 59) / 60
        return ((minutes + 4) / 5) * 5
    }

    private fun dedupe(instances: List<EventInstance>): List<EventInstance> {
        val seen = HashSet<String>()
        return instances
            .sortedBy { it.calendarId }
            .filter { seen.add(listOf(it.title?.trim()?.lowercase(), it.begin.toEpochMilli(), it.end.toEpochMilli(), it.allDay).joinToString("|")) }
    }

    /** Days this agenda covers, for queries: from today to the last day shown, plus a margin for all-day events stored in UTC. */
    fun queryWindow(now: Instant, zone: ZoneId, daysAhead: Int): Pair<Instant, Instant> {
        val today: LocalDate = now.atZone(zone).toLocalDate()
        val from = today.minusDays(1).atStartOfDay(zone).toInstant()
        val to = today.plusDays(daysAhead.toLong() + 1).atStartOfDay(zone).toInstant()
        return from to to
    }
}
