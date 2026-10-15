package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.DateTimeText
import com.zirren.tessera.core.model.UnitPreferences
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** The words used for weather. English defaults; Android builds these from string and plural resources. */
class WeatherPhrases(
    val dryForHours: (hours: Int) -> String = { "Dry for the next $it hours" },
    val startsAt: String = "%1\$s from %2\$s",
    val until: String = "%1\$s until %2\$s",
    val forHours: (type: String, hours: Int) -> String = { type, hours -> "$type for the next $hours hours" },
    val rain: String = "Rain",
    val sleet: String = "Sleet",
    val snow: String = "Snow",
    val highLow: String = "H %1\$s · L %2\$s",
    val feelsLike: String = "Feels like %s",
    val compass: List<String> = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"),
)

/** Turns numbers into widget text, in the user's units. */
class WeatherText(
    private val units: UnitPreferences,
    private val dateTime: DateTimeText,
    private val zone: ZoneId,
    private val phrases: WeatherPhrases = WeatherPhrases(),
    /** For decimal separators: "1.2 mm" vs "1,2 mm". */
    private val locale: Locale = Locale.getDefault(),
) {
    /** "12°", "−3°" (typographic minus, never "-0°"). */
    fun temperature(celsius: Double): String {
        val value = units.temperature.fromCelsius(celsius).roundToInt()
        return (if (value < 0) "\u2212${-value}" else "$value") + "°"
    }

    fun highLow(day: DaySummary): String? {
        val high = day.high ?: return null
        val low = day.low ?: return null
        return phrases.highLow.format(temperature(high), temperature(low))
    }

    fun feelsLike(celsius: Double): String = phrases.feelsLike.format(temperature(celsius))

    /** "4 m/s SW". Direction is where the wind blows from. */
    fun wind(metersPerSecond: Double, fromDegrees: Double?): String {
        val speed = units.windSpeed.fromMetersPerSecond(metersPerSecond).roundToInt()
        val direction = fromDegrees?.let { " " + compass(it) }.orEmpty()
        return "$speed ${units.windSpeed.symbol}$direction"
    }

    fun compass(degrees: Double): String {
        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return phrases.compass[index]
    }

    fun precipitation(millimeters: Double): String {
        val value = units.precipitation.fromMillimeters(millimeters)
        val text = if (value < 10) String.format(locale, "%.1f", value) else value.roundToInt().toString()
        return "$text ${units.precipitation.symbol}"
    }

    fun outlook(outlook: PrecipitationOutlook): String = when (outlook) {
        is PrecipitationOutlook.DryFor -> phrases.dryForHours(outlook.hours)
        is PrecipitationOutlook.StartsAt -> phrases.startsAt.format(typeWord(outlook.type), clock(outlook.time))
        is PrecipitationOutlook.Ongoing ->
            if (outlook.until != null) phrases.until.format(typeWord(outlook.type), clock(outlook.until))
            else phrases.forHours(typeWord(outlook.type), outlook.horizonHours)
    }

    private fun typeWord(type: PrecipitationType) = when (type) {
        PrecipitationType.RAIN -> phrases.rain
        PrecipitationType.SLEET -> phrases.sleet
        PrecipitationType.SNOW -> phrases.snow
    }

    private fun clock(instant: Instant) = dateTime.time(instant.atZone(zone).toLocalTime())
}
