package com.zirren.tessera.core.weather

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

sealed interface SunEvents {
    data class RiseAndSet(val sunrise: Instant, val sunset: Instant) : SunEvents

    /** Midnight sun: the sun stays up all day. */
    data object PolarDay : SunEvents

    /** The sun never rises. */
    data object PolarNight : SunEvents
}

/**
 * Sunrise and sunset computed on the phone (NOAA solar calculator equations), so they
 * need no network and no API. Accurate to about a minute outside the polar regions.
 * Uses the standard −0.833° solar altitude (refraction plus the sun's radius).
 */
object SunCalculator {

    /** Sun events for the calendar [date] at a place. Longitude is positive east. */
    fun events(date: LocalDate, latitude: Double, longitude: Double): SunEvents {
        val julianDay = julianDay(date)
        val rise = eventMinutesUtc(true, julianDay, latitude, longitude) ?: return polarState(julianDay, latitude)
        val set = eventMinutesUtc(false, julianDay, latitude, longitude) ?: return polarState(julianDay, latitude)
        val midnightUtc = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        return SunEvents.RiseAndSet(
            sunrise = midnightUtc.plusSeconds((rise * 60).toLong()),
            sunset = midnightUtc.plusSeconds((set * 60).toLong()),
        )
    }

    /** Minutes after 00:00 UTC on the date (may be negative or above 1440 far from Greenwich). */
    private fun eventMinutesUtc(rise: Boolean, julianDay: Double, latitude: Double, longitude: Double): Double? {
        val first = minutesUtc(rise, julianDay, latitude, longitude) ?: return null
        // Second pass at the approximate event time sharpens the result.
        return minutesUtc(rise, julianDay + first / 1440.0, latitude, longitude)
    }

    private fun minutesUtc(rise: Boolean, julianDay: Double, latitude: Double, longitude: Double): Double? {
        val t = julianCentury(julianDay)
        val equationOfTime = equationOfTimeMinutes(t)
        val declination = declinationDegrees(t)
        val hourAngle = hourAngleRadians(latitude, declination) ?: return null
        val delta = longitude + Math.toDegrees(if (rise) hourAngle else -hourAngle)
        return 720.0 - 4.0 * delta - equationOfTime
    }

    private fun polarState(julianDay: Double, latitude: Double): SunEvents {
        val declination = declinationDegrees(julianCentury(julianDay))
        // Sun above the horizon at local noon (with refraction) means it never sets.
        val noonAltitude = 90.0 - kotlin.math.abs(latitude - declination)
        return if (noonAltitude > -0.833) SunEvents.PolarDay else SunEvents.PolarNight
    }

    private fun hourAngleRadians(latitude: Double, declination: Double): Double? {
        val lat = Math.toRadians(latitude)
        val dec = Math.toRadians(declination)
        val cosHourAngle = cos(Math.toRadians(90.833)) / (cos(lat) * cos(dec)) - tan(lat) * tan(dec)
        if (cosHourAngle < -1.0 || cosHourAngle > 1.0) return null
        return acos(cosHourAngle)
    }

    private fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.dayOfMonth + b - 1524.5
    }

    private fun julianCentury(julianDay: Double) = (julianDay - 2451545.0) / 36525.0

    private fun geometricMeanLongitude(t: Double): Double {
        var l0 = 280.46646 + t * (36000.76983 + t * 0.0003032)
        l0 %= 360.0
        if (l0 < 0) l0 += 360.0
        return l0
    }

    private fun geometricMeanAnomaly(t: Double) = 357.52911 + t * (35999.05029 - 0.0001537 * t)

    private fun orbitEccentricity(t: Double) = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

    private fun equationOfCenter(t: Double): Double {
        val m = Math.toRadians(geometricMeanAnomaly(t))
        return sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * m) * (0.019993 - 0.000101 * t) +
            sin(3 * m) * 0.000289
    }

    private fun apparentLongitude(t: Double): Double {
        val trueLongitude = geometricMeanLongitude(t) + equationOfCenter(t)
        val omega = 125.04 - 1934.136 * t
        return trueLongitude - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
    }

    private fun obliquityCorrected(t: Double): Double {
        val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
        val meanObliquity = 23.0 + (26.0 + seconds / 60.0) / 60.0
        val omega = 125.04 - 1934.136 * t
        return meanObliquity + 0.00256 * cos(Math.toRadians(omega))
    }

    private fun declinationDegrees(t: Double): Double {
        val sinDeclination = sin(Math.toRadians(obliquityCorrected(t))) * sin(Math.toRadians(apparentLongitude(t)))
        return Math.toDegrees(asin(sinDeclination))
    }

    private fun equationOfTimeMinutes(t: Double): Double {
        val epsilon = Math.toRadians(obliquityCorrected(t))
        val l0 = Math.toRadians(geometricMeanLongitude(t))
        val e = orbitEccentricity(t)
        val m = Math.toRadians(geometricMeanAnomaly(t))
        var y = tan(epsilon / 2.0)
        y *= y
        val eTime = y * sin(2.0 * l0) - 2.0 * e * sin(m) + 4.0 * e * y * sin(m) * cos(2.0 * l0) -
            0.5 * y * y * sin(4.0 * l0) - 1.25 * e * e * sin(2.0 * m)
        return Math.toDegrees(eTime) * 4.0
    }
}
