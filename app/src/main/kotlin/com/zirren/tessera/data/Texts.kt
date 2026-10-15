package com.zirren.tessera.data

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.text.format.DateFormat
import com.zirren.tessera.R
import com.zirren.tessera.core.calendar.AgendaPhrases
import com.zirren.tessera.core.calendar.AgendaText
import com.zirren.tessera.core.model.AgePhrases
import com.zirren.tessera.core.model.DateTimeText
import com.zirren.tessera.core.model.UnitPreferences
import com.zirren.tessera.core.weather.WeatherPhrases
import com.zirren.tessera.core.weather.WeatherText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Dates and times exactly as the phone writes them: its language, its region's order
 * ("17 Oct" vs "Oct 17") and its 12/24-hour setting. Not thread-safe; make one per use.
 */
class AndroidDateTimeText(context: Context) : DateTimeText {
    val locale: Locale = context.resources.configuration.locales[0]
    private val use24Hour = DateFormat.is24HourFormat(context)
    private val zone = ZoneId.systemDefault()

    private val timeFormat = format(if (use24Hour) "Hm" else "hm")
    private val hourFormat = format(if (use24Hour) "H" else "ha")
    private val weekdayFormat = format("EEEE")
    private val shortWeekdayFormat = format("EEE")
    private val shortDateFormat = format("dMMM")
    private val longDateFormat = format("EEEEdMMMM")

    override fun time(time: LocalTime): String = render(timeFormat, LocalDate.now(zone).atTime(time))
    override fun hour(time: LocalTime): String = render(hourFormat, LocalDate.now(zone).atTime(time))
    override fun weekday(date: LocalDate): String = render(weekdayFormat, date.atStartOfDay())
    override fun shortWeekday(date: LocalDate): String = render(shortWeekdayFormat, date.atStartOfDay())
    override fun shortDate(date: LocalDate): String = render(shortDateFormat, date.atStartOfDay())
    override fun longDate(date: LocalDate): String = render(longDateFormat, date.atStartOfDay())

    /** Pattern for TextClock, which formats live in the launcher. */
    fun pattern(skeleton: String): String = DateFormat.getBestDateTimePattern(locale, skeleton)

    private fun format(skeleton: String) = SimpleDateFormat(pattern(skeleton), locale)

    private fun render(format: SimpleDateFormat, dateTime: LocalDateTime): String =
        format.format(Date.from(dateTime.atZone(zone).toInstant()))
}

/** Every widget word, built from string and plural resources so Tessera can be translated. */
class Texts(context: Context, units: UnitPreferences) {
    private val res = context.resources
    val dateTime = AndroidDateTimeText(context)
    val age = agePhrases(context)
    val agenda = AgendaText(
        dateTime,
        AgendaPhrases(
            today = context.getString(R.string.agenda_today),
            tomorrow = context.getString(R.string.agenda_tomorrow),
            allDay = context.getString(R.string.agenda_all_day),
            now = context.getString(R.string.agenda_now),
            untilTime = context.getString(R.string.agenda_until),
            inMinutes = context.getString(R.string.agenda_in_minutes),
            dayOfSpan = context.getString(R.string.agenda_day_of_span),
            nothingToday = context.getString(R.string.agenda_nothing_today),
            nothingAhead = { days -> res.getQuantityString(R.plurals.agenda_nothing_ahead, days, days) },
            nothingTomorrow = context.getString(R.string.agenda_nothing_tomorrow),
        ),
    )
    val weather = WeatherText(
        units,
        dateTime,
        ZoneId.systemDefault(),
        WeatherPhrases(
            dryForHours = { hours -> res.getQuantityString(R.plurals.weather_dry_for, hours, hours) },
            startsAt = context.getString(R.string.weather_starts_at),
            until = context.getString(R.string.weather_until),
            forHours = { type, hours -> res.getQuantityString(R.plurals.weather_for_hours, hours, type, hours) },
            rain = context.getString(R.string.weather_rain),
            sleet = context.getString(R.string.weather_sleet),
            snow = context.getString(R.string.weather_snow),
            highLow = context.getString(R.string.weather_high_low),
            feelsLike = context.getString(R.string.weather_feels_like),
            compass = context.resources.getStringArray(R.array.compass_points).toList(),
        ),
        dateTime.locale,
    )
    val busyLabel: String = context.getString(R.string.agenda_busy)
    val untitledLabel: String = context.getString(R.string.agenda_untitled)

    companion object {
        fun agePhrases(context: Context): AgePhrases {
            val res = context.resources
            return AgePhrases(
                justNow = res.getString(R.string.age_just_now),
                minutesAgo = { minutes -> res.getString(R.string.age_minutes, minutes) },
                hoursAgo = { hours -> res.getString(R.string.age_hours, hours) },
                daysAgo = { days -> res.getQuantityString(R.plurals.age_days, days, days) },
            )
        }

        /** The phone's region decides default units: a Swedish phone gets °C and m/s. */
        fun regionOf(context: Context): String? = context.resources.configuration.locales[0].country.takeIf { it.isNotBlank() }

        fun forContext(context: Context, settings: Settings): Texts = Texts(context, settings.units(regionOf(context)))
    }
}
