package com.zirren.tessera.core.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** A calendar on the phone (Google, Exchange, local, CalDAV via DAVx⁵…). */
data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String?,
    /** ARGB. */
    val color: Int,
    /** Whether the user shows it in their calendar app. */
    val isVisible: Boolean,
    val isPrimary: Boolean = false,
)

enum class AttendanceStatus { NONE, ACCEPTED, DECLINED, INVITED, TENTATIVE }

enum class Availability { BUSY, FREE, TENTATIVE }

/** One occurrence of an event, as read from `CalendarContract.Instances`. */
data class EventInstance(
    val eventId: Long,
    val calendarId: Long,
    val title: String?,
    val location: String?,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    /** ARGB display colour (event colour, else calendar colour). */
    val color: Int? = null,
    val attendance: AttendanceStatus = AttendanceStatus.NONE,
    val availability: Availability = Availability.BUSY,
) {
    /**
     * First local day the event touches.
     * All-day events are stored at UTC midnight and must be read as plain dates;
     * reading them in the local zone puts them on the wrong day west of Greenwich.
     */
    fun firstDay(zone: ZoneId): LocalDate =
        if (allDay) begin.atZone(ZoneOffset.UTC).toLocalDate() else begin.atZone(zone).toLocalDate()

    /** Last local day the event touches (inclusive). An event ending exactly at midnight doesn't touch the next day. */
    fun lastDay(zone: ZoneId): LocalDate {
        val first = firstDay(zone)
        val last = if (allDay) {
            end.atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
        } else {
            end.minusNanos(1).atZone(zone).toLocalDate()
        }
        return if (last.isBefore(first)) first else last
    }

    /** When the event really starts in local time (all-day: local midnight of its first day). */
    fun localStart(zone: ZoneId): Instant =
        if (allDay) firstDay(zone).atStartOfDay(zone).toInstant() else begin

    /** When the event really ends in local time (all-day: local midnight after its last day). */
    fun localEnd(zone: ZoneId): Instant =
        if (allDay) lastDay(zone).plusDays(1).atStartOfDay(zone).toInstant() else maxOf(end, begin)
}

enum class TitlePrivacy {
    SHOW_TITLES,

    /** Widgets say "Busy" instead of the event title and hide the location. */
    BUSY_ONLY,
}

data class AgendaSettings(
    /** Calendars to include; null means every calendar the user shows in their calendar app. */
    val calendarIds: Set<Long>? = null,
    val includeAllDay: Boolean = true,
    val hideDeclined: Boolean = true,
    val privacy: TitlePrivacy = TitlePrivacy.SHOW_TITLES,
    val daysAhead: Int = 7,
    /** The same meeting often sits in two calendars (work invite + personal copy); show it once. */
    val mergeDuplicates: Boolean = true,
    val busyLabel: String = "Busy",
    val untitledLabel: String = "(No title)",
) {
    init {
        require(daysAhead in 1..31) { "daysAhead must be 1..31" }
    }
}

/** One row in an agenda. */
data class AgendaItem(
    val instance: EventInstance,
    /** The day this row is listed under. */
    val day: LocalDate,
    /** After privacy and untitled handling. */
    val title: String,
    val location: String?,
    val isOngoing: Boolean,
    /** 1 for single-day events. */
    val spanDays: Int,
    /** 1-based position inside a multi-day span ("Day 2 of 3"). */
    val dayIndex: Int,
)

data class AgendaDay(val date: LocalDate, val items: List<AgendaItem>)

data class Agenda(
    val days: List<AgendaDay>,
    /** What a "next event" line shows: the ongoing or next timed event, else today's first all-day event. */
    val nextUp: AgendaItem?,
    val generatedAt: Instant,
    val zone: ZoneId,
    val today: LocalDate,
    val daysAhead: Int,
) {
    val isEmpty: Boolean get() = days.isEmpty()
    val items: List<AgendaItem> get() = days.flatMap { it.items }
    val todayItems: List<AgendaItem> get() = days.firstOrNull { it.date == today }?.items.orEmpty()
}
