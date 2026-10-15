package com.zirren.tessera.widget

import android.content.Context
import com.zirren.tessera.core.calendar.Agenda
import com.zirren.tessera.core.calendar.AgendaEngine
import com.zirren.tessera.data.Texts
import com.zirren.tessera.data.WeatherSnapshot
import com.zirren.tessera.graph
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Everything the widgets draw, loaded once per redraw. */
class WidgetModel(
    val now: Instant,
    val zone: ZoneId,
    val weather: WeatherSnapshot?,
    val weatherConfigured: Boolean,
    val agenda: Agenda?,
    val calendarAllowed: Boolean,
    val showCalendarPrompt: Boolean,
    val texts: Texts,
) {
    /** Next moment any widget would look different. Clocks are excluded: TextClock ticks on its own. */
    fun nextChange(): Instant {
        val nextHour = now.atZone(zone).truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant()
        val agendaChange = agenda?.let { AgendaEngine.nextChange(it, now) } ?: nextHour
        return minOf(nextHour, agendaChange)
    }

    companion object {
        fun load(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): WidgetModel {
            val graph = context.graph
            val settings = graph.settings
            val texts = Texts.forContext(context, settings)
            val calendarAllowed = graph.calendar.hasPermission()
            val agenda = if (calendarAllowed) {
                val agendaSettings = settings.agendaSettings(texts.busyLabel, texts.untitledLabel)
                val (from, to) = AgendaEngine.queryWindow(now, zone, agendaSettings.daysAhead)
                AgendaEngine.build(graph.calendar.instances(from, to), graph.calendar.calendars(), agendaSettings, now, zone)
            } else {
                null
            }
            return WidgetModel(
                now = now,
                zone = zone,
                weather = graph.weather.snapshot(now, zone),
                weatherConfigured = settings.activePlace() != null,
                agenda = agenda,
                calendarAllowed = calendarAllowed,
                showCalendarPrompt = !calendarAllowed && !settings.calendarPromptDismissed,
                texts = texts,
            )
        }
    }
}
