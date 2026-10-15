package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import com.zirren.tessera.core.model.PatternDateTimeText
import com.zirren.tessera.core.model.PrecipitationUnit
import com.zirren.tessera.core.model.TemperatureUnit
import com.zirren.tessera.core.model.UnitPreferences
import com.zirren.tessera.core.model.WindSpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeatherReporterTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val start = Instant.parse("2026-10-15T06:00:00Z") // 08:00 in Stockholm

    private fun forecast(): Forecast {
        // 40 hourly steps (to 22:00 UTC the next day), then 6-hourly.
        val hourly = TestForecasts.hours(start, 40) { i ->
            val temp = 8.0 + (if (i in 4..8) i - 2 else 0) // warmest at i = 8 (14:00Z, 16:00 local): 14 °C
            Triple(temp, if (i == 3) "lightrain" else "partlycloudy_day", if (i == 3) 0.4 else 0.0)
        }
        val sixHourly = listOf(
            TestForecasts.sixHourly(Instant.parse("2026-10-17T00:00:00Z"), 4.0, max = 5.0, min = 2.5),
            TestForecasts.sixHourly(Instant.parse("2026-10-17T06:00:00Z"), 6.0, max = 11.0, min = 5.5, precipitation = 2.0),
            TestForecasts.sixHourly(Instant.parse("2026-10-17T12:00:00Z"), 10.0, max = 10.5, min = 7.0, symbol = "rain"),
            TestForecasts.sixHourly(Instant.parse("2026-10-17T18:00:00Z"), 7.0, max = 7.5, min = 3.0),
        )
        return Forecast(GeoPoint(58.2837, 12.2886), start, hourly + sixHourly)
    }

    @Test fun currentConditionsUseTheStepCoveringNow() {
        val now = start.plusSeconds(3 * 3600 + 15 * 60) // 11:15 local, inside the 09:00Z step
        val report = WeatherReporter.report(forecast(), now, zone)
        val current = report.current!!
        assertEquals(start.plusSeconds(3 * 3600), current.time)
        assertEquals("lightrain", current.symbol!!.code)
        assertEquals(0.4, current.precipitationNextHour!!, 1e-9)
    }

    @Test fun cachedForecastStillAnswersForLaterHours() {
        val fiveHoursLater = start.plusSeconds(5 * 3600 + 5 * 60)
        val report = WeatherReporter.report(forecast(), fiveHoursLater, zone)
        assertEquals(start.plusSeconds(5 * 3600), report.current!!.time)
        assertEquals(start.plusSeconds(5 * 3600), report.hourly.first().time)
    }

    @Test fun todayHighAndLowCoverTheRestOfTheLocalDay() {
        val report = WeatherReporter.report(forecast(), start.plusSeconds(60), zone)
        val today = report.today!!
        assertEquals(LocalDate.of(2026, 10, 15), today.date)
        assertEquals(14.0, today.high!!, 1e-9)
        assertEquals(8.0, today.low!!, 1e-9)
        assertEquals(0.4, today.precipitation!!, 1e-9)
    }

    @Test fun sixHourlyDaysUsePeriodMinMax() {
        val report = WeatherReporter.report(forecast(), start.plusSeconds(60), zone)
        val saturday = report.daily.first { it.date == LocalDate.of(2026, 10, 17) }
        assertEquals(11.0, saturday.high!!, 1e-9)
        assertEquals(2.5, saturday.low!!, 1e-9)
        assertEquals(2.0, saturday.precipitation!!, 1e-9)
        // Symbol from the period nearest local noon (12:00Z = 14:00 local).
        assertEquals("rain", saturday.symbol!!.code)
    }

    @Test fun reportIncludesSunTimesAndOutlook() {
        val report = WeatherReporter.report(forecast(), start.plusSeconds(60), zone)
        assertTrue(report.sun is SunEvents.RiseAndSet)
        assertEquals(PrecipitationOutlook.StartsAt(start.plusSeconds(3 * 3600), PrecipitationType.RAIN), report.outlook)
    }

    @Test fun forecastFromLongAgoHasNoCurrentConditions() {
        val muchLater = Instant.parse("2026-10-25T12:00:00Z")
        val report = WeatherReporter.report(forecast(), muchLater, zone)
        assertNull(report.current)
        assertTrue(report.hourly.isEmpty())
    }
}

class WeatherTextTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val metric = WeatherText(
        UnitPreferences(TemperatureUnit.CELSIUS, WindSpeedUnit.METERS_PER_SECOND, PrecipitationUnit.MILLIMETERS),
        PatternDateTimeText(),
        zone,
        locale = java.util.Locale.UK,
    )
    private val us = WeatherText(UnitPreferences.defaultsFor("US"), PatternDateTimeText(java.util.Locale.US, use24Hour = false), ZoneId.of("America/New_York"), locale = java.util.Locale.US)

    @Test fun temperaturesUseTypographicMinusAndNoNegativeZero() {
        assertEquals("12°", metric.temperature(11.6))
        assertEquals("\u22123°", metric.temperature(-3.4))
        assertEquals("0°", metric.temperature(-0.3))
        assertEquals("53°", us.temperature(11.6))
    }

    @Test fun windAndCompass() {
        assertEquals("5 m/s SW", metric.wind(5.1, 214.3))
        assertEquals("11 mph N", us.wind(5.1, 359.0))
        assertEquals("N", metric.compass(-10.0))
        assertEquals("E", metric.compass(80.0))
    }

    @Test fun outlookSentences() {
        val at = Instant.parse("2026-10-15T12:00:00Z")
        assertEquals("Rain from 14:00", metric.outlook(PrecipitationOutlook.StartsAt(at, PrecipitationType.RAIN)))
        assertEquals("Snow until 14:00", metric.outlook(PrecipitationOutlook.Ongoing(PrecipitationType.SNOW, at, 12)))
        assertEquals("Sleet for the next 12 hours", metric.outlook(PrecipitationOutlook.Ongoing(PrecipitationType.SLEET, null, 12)))
        assertEquals("Dry for the next 12 hours", metric.outlook(PrecipitationOutlook.DryFor(12)))
        assertEquals("Rain from 8:00 AM", us.outlook(PrecipitationOutlook.StartsAt(at, PrecipitationType.RAIN)))
    }

    @Test fun highLowAndPrecipitation() {
        assertEquals("H 14° · L 8°", metric.highLow(DaySummary(LocalDate.of(2026, 10, 15), 14.2, 7.9, null, null)))
        assertEquals("1.2 mm", metric.precipitation(1.24))
        assertEquals("0.1 in", us.precipitation(2.5))
    }
}
