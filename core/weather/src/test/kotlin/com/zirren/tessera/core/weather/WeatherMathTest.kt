package com.zirren.tessera.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FeelsLikeTest {

    @Test fun windChillWhenColdAndWindy() {
        // -10 °C with 30 km/h wind: Environment Canada's table says about -20.
        assertEquals(-19.5, FeelsLike.celsius(-10.0, 30.0 / 3.6, 80.0), 0.2)
    }

    @Test fun heatIndexWhenHotAndHumid() {
        // 32 °C (89.6 °F) at 70 %: NOAA's table gives ~105 °F, about 40.4 °C.
        assertEquals(40.4, FeelsLike.celsius(32.0, 1.0, 70.0), 0.3)
    }

    @Test fun mildWeatherFeelsLikeItIs() {
        assertEquals(15.0, FeelsLike.celsius(15.0, 6.0, 60.0), 1e-9)
        assertEquals(5.0, FeelsLike.celsius(5.0, 0.5, 60.0), 1e-9)
        assertEquals(30.0, FeelsLike.celsius(30.0, 2.0, 20.0), 1e-9)
    }

    @Test fun windChillNeverWarmsYouUp() {
        assertTrue(FeelsLike.celsius(10.0, 1.4, 50.0) <= 10.0)
    }
}

class SunCalculatorTest {

    private fun assertNear(expected: String, actual: Instant, zone: String) {
        val local = actual.atZone(ZoneId.of(zone)).toLocalTime()
        val want = java.time.LocalTime.parse(expected)
        val diff = Math.abs(Duration.between(want, local).toMinutes())
        assertTrue("expected ~$expected, got $local", diff <= 3)
    }

    @Test fun londonSummerSolstice() {
        val events = SunCalculator.events(LocalDate.of(2024, 6, 21), 51.5074, -0.1278) as SunEvents.RiseAndSet
        assertNear("04:43", events.sunrise, "Europe/London")
        assertNear("21:21", events.sunset, "Europe/London")
    }

    @Test fun stockholmWinterSolstice() {
        val events = SunCalculator.events(LocalDate.of(2024, 12, 21), 59.3293, 18.0686) as SunEvents.RiseAndSet
        assertNear("08:43", events.sunrise, "Europe/Stockholm")
        assertNear("14:48", events.sunset, "Europe/Stockholm")
    }

    @Test fun farFromGreenwichStillLandsOnTheRightLocalDay() {
        val tokyo = SunCalculator.events(LocalDate.of(2024, 6, 21), 35.6762, 139.6503) as SunEvents.RiseAndSet
        assertEquals(LocalDate.of(2024, 6, 21), tokyo.sunrise.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate())
        assertNear("04:25", tokyo.sunrise, "Asia/Tokyo")
        val la = SunCalculator.events(LocalDate.of(2024, 6, 21), 34.0522, -118.2437) as SunEvents.RiseAndSet
        assertEquals(LocalDate.of(2024, 6, 21), la.sunset.atZone(ZoneId.of("America/Los_Angeles")).toLocalDate())
        assertNear("20:08", la.sunset, "America/Los_Angeles")
    }

    @Test fun tromsoHasPolarNightAndMidnightSun() {
        assertEquals(SunEvents.PolarNight, SunCalculator.events(LocalDate.of(2026, 12, 21), 69.6492, 18.9553))
        assertEquals(SunEvents.PolarDay, SunCalculator.events(LocalDate.of(2026, 6, 21), 69.6492, 18.9553))
    }
}

class PrecipitationOutlookTest {
    private val start = Instant.parse("2026-10-15T08:00:00Z")
    private val now = start.plusSeconds(20 * 60)

    @Test fun dryForTheWholeHorizon() {
        assertEquals(PrecipitationOutlook.DryFor(12), PrecipitationOutlook.from(TestForecasts.hours(start, 30), now))
    }

    @Test fun rainStartsLater() {
        val steps = TestForecasts.hours(start, 30) { i -> if (i >= 6) Triple(9.0, "lightrain", 0.6) else Triple(10.0, "cloudy", 0.0) }
        assertEquals(PrecipitationOutlook.StartsAt(start.plusSeconds(6 * 3600), PrecipitationType.RAIN), PrecipitationOutlook.from(steps, now))
    }

    @Test fun snowingNowUntilItStops() {
        val steps = TestForecasts.hours(start, 30) { i -> if (i < 3) Triple(-2.0, "snow", 1.2) else Triple(-3.0, "cloudy", 0.0) }
        assertEquals(PrecipitationOutlook.Ongoing(PrecipitationType.SNOW, start.plusSeconds(3 * 3600), 12), PrecipitationOutlook.from(steps, now))
    }

    @Test fun traceAmountsDoNotCount() {
        val steps = TestForecasts.hours(start, 30) { Triple(10.0, "cloudy", 0.05) }
        assertEquals(PrecipitationOutlook.DryFor(12), PrecipitationOutlook.from(steps, now))
    }

    @Test fun pastHoursAreIgnored() {
        val steps = TestForecasts.hours(start, 30) { i -> if (i == 0) Triple(9.0, "rain", 2.0) else Triple(10.0, "cloudy", 0.0) }
        val later = start.plusSeconds(90 * 60)
        assertEquals(PrecipitationOutlook.DryFor(12), PrecipitationOutlook.from(steps, later))
    }
}
