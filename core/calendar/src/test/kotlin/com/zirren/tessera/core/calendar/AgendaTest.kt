package com.zirren.tessera.core.calendar

import com.zirren.tessera.core.model.PatternDateTimeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun at(text: String): Instant = Instant.parse(text)

private fun event(
    id: Long,
    title: String?,
    begin: String,
    end: String,
    calendar: Long = 1,
    allDay: Boolean = false,
    attendance: AttendanceStatus = AttendanceStatus.ACCEPTED,
    location: String? = null,
) = EventInstance(id, calendar, title, location, at(begin), at(end), allDay, 0xFF3366CC.toInt(), attendance)

class AgendaEngineTest {
    private val zone = ZoneId.of("Europe/Stockholm") // UTC+2 in mid-October
    private val now = at("2026-10-15T08:00:00Z")      // 10:00 local, Thursday
    private val today = LocalDate.of(2026, 10, 15)

    private val calendars = listOf(
        CalendarInfo(1, "Personal", "me@example.org", 0xFF0B8043.toInt(), isVisible = true, isPrimary = true),
        CalendarInfo(2, "Work", "me@work.example", 0xFF3F51B5.toInt(), isVisible = true),
        CalendarInfo(3, "Holidays", null, 0xFF795548.toInt(), isVisible = false),
    )

    private val instances = listOf(
        event(1, "Standup", "2026-10-15T08:30:00Z", "2026-10-15T08:45:00Z", calendar = 2),
        event(2, "Lunch with Sara", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z", location = "Café Tösse"),
        event(3, "Focus block", "2026-10-15T07:30:00Z", "2026-10-15T09:00:00Z", calendar = 2),
        event(4, "Gym", "2026-10-15T06:00:00Z", "2026-10-15T07:00:00Z"),
        event(5, "Optional sync", "2026-10-15T12:00:00Z", "2026-10-15T13:00:00Z", calendar = 2, attendance = AttendanceStatus.DECLINED),
        event(6, "Name day", "2026-10-15T00:00:00Z", "2026-10-16T00:00:00Z", allDay = true),
        event(7, "Bank holiday", "2026-10-16T00:00:00Z", "2026-10-17T00:00:00Z", calendar = 3, allDay = true),
        event(8, "Dentist", "2026-10-16T07:00:00Z", "2026-10-16T07:45:00Z"),
        event(9, "Trip to Oslo", "2026-10-14T00:00:00Z", "2026-10-17T00:00:00Z", allDay = true),
        event(10, "Standup", "2026-10-15T08:30:00Z", "2026-10-15T08:45:00Z", calendar = 1),
        event(11, "Far away", "2026-10-30T07:00:00Z", "2026-10-30T08:00:00Z"),
    )

    private fun build(settings: AgendaSettings = AgendaSettings()) =
        AgendaEngine.build(instances, calendars, settings, now, zone)

    @Test fun groupsByDayWithAllDayFirst() {
        val agenda = build()
        assertEquals(listOf(today, today.plusDays(1)), agenda.days.map { it.date })
        assertEquals(
            listOf("Trip to Oslo", "Name day", "Focus block", "Standup", "Lunch with Sara"),
            agenda.todayItems.map { it.title },
        )
        assertEquals(listOf("Dentist"), agenda.days[1].items.map { it.title })
    }

    @Test fun dropsEndedDeclinedHiddenAndOutOfRange() {
        val titles = build().items.map { it.title }
        assertFalse("Gym" in titles)
        assertFalse("Optional sync" in titles)
        assertFalse("Bank holiday" in titles)
        assertFalse("Far away" in titles)
    }

    @Test fun mergesTheSameMeetingFromTwoCalendars() {
        assertEquals(1, build().items.count { it.title == "Standup" })
        assertEquals(2, build(AgendaSettings(mergeDuplicates = false)).items.count { it.title == "Standup" })
    }

    @Test fun nextUpIsTheOngoingMeeting() {
        val next = build().nextUp!!
        assertEquals("Focus block", next.title)
        assertTrue(next.isOngoing)
    }

    @Test fun multiDayAllDayEventKnowsWhichDayItIs() {
        val trip = build().todayItems.first { it.title == "Trip to Oslo" }
        assertEquals(3, trip.spanDays)
        assertEquals(2, trip.dayIndex)
        assertTrue(trip.isOngoing)
    }

    @Test fun allDayEventsStayOnTheirDateWestOfGreenwich() {
        val la = ZoneId.of("America/Los_Angeles")
        val afternoonInLa = at("2026-10-15T20:00:00Z") // 13:00 PDT on the 15th
        // Stored as 2026-10-16T00:00Z, which is 17:00 on the 15th in Los Angeles.
        val tomorrowAllDay = event(1, "Pumpkin patch", "2026-10-16T00:00:00Z", "2026-10-17T00:00:00Z", allDay = true)
        val agenda = AgendaEngine.build(listOf(tomorrowAllDay), null, AgendaSettings(), afternoonInLa, la)
        val item = agenda.items.single()
        assertEquals(LocalDate.of(2026, 10, 16), item.day)
        assertFalse(item.isOngoing)
        assertNull(agenda.nextUp) // not today, and nothing timed
    }

    @Test fun privacyModeHidesTitlesAndPlaces() {
        val agenda = build(AgendaSettings(privacy = TitlePrivacy.BUSY_ONLY))
        assertTrue(agenda.items.all { it.title == "Busy" && it.location == null })
    }

    @Test fun untitledEventsGetAReadableTitle() {
        val agenda = AgendaEngine.build(listOf(event(1, "  ", "2026-10-15T09:00:00Z", "2026-10-15T10:00:00Z")), null, AgendaSettings(), now, zone)
        assertEquals("(No title)", agenda.items.single().title)
    }

    @Test fun settingsFilterCalendarsAndAllDay() {
        val personalOnly = build(AgendaSettings(calendarIds = setOf(1L), includeAllDay = false))
        assertEquals(listOf("Standup", "Lunch with Sara", "Dentist"), personalOnly.items.map { it.title })
        val oneDay = build(AgendaSettings(daysAhead = 1))
        assertEquals(listOf(today), oneDay.days.map { it.date })
    }

    @Test fun nextChangeIsTheNextStartOrEnd() {
        assertEquals(at("2026-10-15T08:30:00Z"), AgendaEngine.nextChange(build(), now))
    }

    @Test fun nextChangeFallsBackToMidnight() {
        val empty = AgendaEngine.build(emptyList(), calendars, AgendaSettings(), now, zone)
        assertEquals(at("2026-10-15T22:00:00Z"), AgendaEngine.nextChange(empty, now))
    }

    @Test fun relativeMinutesStepInFives() {
        val soon = event(1, "Call", "2026-10-15T08:23:00Z", "2026-10-15T08:50:00Z")
        val agenda = AgendaEngine.build(listOf(soon), null, AgendaSettings(), now, zone)
        // Shown as "in 25 min" until it becomes "in 20 min", 20 minutes before the start.
        assertEquals(at("2026-10-15T08:03:00Z"), AgendaEngine.nextChange(agenda, now, relativeMinutes = true))
        assertEquals(at("2026-10-15T08:23:00Z"), AgendaEngine.nextChange(agenda, now, relativeMinutes = false))
        assertEquals(25L, AgendaEngine.roundUpToFive(23 * 60))
        assertEquals(5L, AgendaEngine.roundUpToFive(30))
        assertNull(AgendaEngine.relativeStepBoundary(now.plusSeconds(2 * 3600), now))
        assertEquals(now.plusSeconds(240), AgendaEngine.relativeStepBoundary(now.plusSeconds(240), now))
    }

    @Test fun queryWindowAddsAMarginForUtcAllDayEvents() {
        val (from, to) = AgendaEngine.queryWindow(now, zone, 7)
        assertEquals(at("2026-10-13T22:00:00Z"), from)
        assertEquals(at("2026-10-22T22:00:00Z"), to)
    }
}

class AgendaTextTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val now = at("2026-10-15T08:00:00Z")
    private val text = AgendaText(PatternDateTimeText())
    private val agenda = AgendaEngine.build(
        listOf(
            event(1, "Focus block", "2026-10-15T07:30:00Z", "2026-10-15T09:00:00Z"),
            event(2, "Lunch", "2026-10-15T10:00:00Z", "2026-10-15T11:00:00Z"),
            event(3, "Late show", "2026-10-15T20:00:00Z", "2026-10-15T23:00:00Z"),
            event(4, "Dentist", "2026-10-16T07:00:00Z", "2026-10-16T07:45:00Z"),
            event(5, "Trip", "2026-10-14T00:00:00Z", "2026-10-17T00:00:00Z", allDay = true),
            event(6, "Book club", "2026-10-19T17:00:00Z", "2026-10-19T19:00:00Z"),
        ),
        null, AgendaSettings(), now, zone,
    )

    private fun item(title: String) = agenda.items.first { it.title == title }

    @Test fun dayHeaders() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals("Today", text.dayHeader(today, today))
        assertEquals("Tomorrow", text.dayHeader(today.plusDays(1), today))
        assertEquals("Saturday 17 October", text.dayHeader(today.plusDays(2), today))
    }

    @Test fun timeRanges() {
        assertEquals("12:00\u201313:00", text.timeRange(item("Lunch"), zone))
        assertEquals("Day 2 of 3", text.timeRange(item("Trip"), zone))
        // 22:00–01:00 local crosses midnight.
        assertEquals("22:00\u2013Fri 01:00", text.timeRange(item("Late show"), zone))
    }

    @Test fun whenLabels() {
        assertEquals("Now · until 11:00", text.whenLabel(item("Focus block"), now, zone))
        assertEquals("12:00", text.whenLabel(item("Lunch"), now, zone))
        assertEquals("Tomorrow 09:00", text.whenLabel(item("Dentist"), now, zone))
        assertEquals("Mon 19:00", text.whenLabel(item("Book club"), now, zone))
        assertEquals("Day 2 of 3", text.whenLabel(item("Trip"), now, zone))
    }

    @Test fun relativeLabelWithinTheHour() {
        val soon = AgendaEngine.build(listOf(event(1, "Call", "2026-10-15T08:23:00Z", "2026-10-15T08:50:00Z")), null, AgendaSettings(), now, zone)
        assertEquals("in 25 min", text.whenLabel(soon.items.single(), now, zone, relativeMinutes = true))
        assertEquals("10:23", text.whenLabel(soon.items.single(), now, zone, relativeMinutes = false))
    }

    @Test fun emptyLines() {
        val empty = AgendaEngine.build(emptyList(), null, AgendaSettings(), now, zone)
        assertEquals("Nothing in the next 7 days", text.emptyLine(empty))
        assertEquals("Nothing else today", text.emptyLine(agenda))
    }
}
