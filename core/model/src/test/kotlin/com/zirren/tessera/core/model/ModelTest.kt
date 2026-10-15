package com.zirren.tessera.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class GeoPointTest {
    private val trollhattan = GeoPoint(58.283651, 12.288642, 45)
    private val goteborg = GeoPoint(57.70887, 11.97456)

    @Test fun roundsToFourDecimalsForMetNorway() {
        assertEquals(GeoPoint(58.2837, 12.2886, 45), trollhattan.rounded(4))
    }

    @Test fun roundsToTwoDecimalsForPrivacy() {
        assertEquals(GeoPoint(58.28, 12.29, 45), trollhattan.rounded(2))
    }

    @Test fun keyIsPlainDecimalText() {
        assertEquals("58.2837,12.2886", trollhattan.key())
        assertEquals("-33.8688,151.2093", GeoPoint(-33.86882, 151.20929).key())
        assertEquals("0,0", GeoPoint(0.0, 0.0).key())
    }

    @Test fun distanceTrollhattanToGoteborgIsAbout67Km() {
        val km = trollhattan.distanceKmTo(goteborg)
        assertTrue("was $km", km in 60.0..72.0)
        assertEquals(0.0, trollhattan.distanceKmTo(trollhattan), 1e-9)
    }

    @Test fun rejectsImpossibleCoordinates() {
        val error = runCatching { GeoPoint(91.0, 0.0) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}

class PlaceTest {
    @Test fun labelSkipsRegionThatRepeatsTheName() {
        val point = GeoPoint(59.33, 18.07)
        assertEquals("Stockholm", Place("Stockholm", point, region = "Stockholm").label)
        assertEquals("Trollhättan, Västra Götaland", Place("Trollhättan", point, region = "Västra Götaland").label)
        assertEquals(
            "Trollhättan, Västra Götaland, Sweden",
            Place("Trollhättan", point, region = "Västra Götaland", country = "Sweden").longLabel,
        )
    }
}

class UnitsTest {
    @Test fun swedenGetsCelsiusAndMetresPerSecond() {
        val units = UnitPreferences.defaultsFor("se")
        assertEquals(TemperatureUnit.CELSIUS, units.temperature)
        assertEquals(WindSpeedUnit.METERS_PER_SECOND, units.windSpeed)
        assertEquals(PrecipitationUnit.MILLIMETERS, units.precipitation)
    }

    @Test fun unitedStatesGetsFahrenheitMphAndInches() {
        assertEquals(
            UnitPreferences(TemperatureUnit.FAHRENHEIT, WindSpeedUnit.MILES_PER_HOUR, PrecipitationUnit.INCHES),
            UnitPreferences.defaultsFor("US"),
        )
    }

    @Test fun britainGetsCelsiusButMph() {
        val units = UnitPreferences.defaultsFor("GB")
        assertEquals(TemperatureUnit.CELSIUS, units.temperature)
        assertEquals(WindSpeedUnit.MILES_PER_HOUR, units.windSpeed)
    }

    @Test fun unknownRegionFallsBackToMetricKmh() {
        assertEquals(
            UnitPreferences(TemperatureUnit.CELSIUS, WindSpeedUnit.KILOMETERS_PER_HOUR, PrecipitationUnit.MILLIMETERS),
            UnitPreferences.defaultsFor(null),
        )
    }

    @Test fun conversions() {
        assertEquals(32.0, TemperatureUnit.FAHRENHEIT.fromCelsius(0.0), 1e-9)
        assertEquals(-40.0, TemperatureUnit.FAHRENHEIT.fromCelsius(-40.0), 1e-9)
        assertEquals(36.0, WindSpeedUnit.KILOMETERS_PER_HOUR.fromMetersPerSecond(10.0), 1e-9)
        assertEquals(22.369, WindSpeedUnit.MILES_PER_HOUR.fromMetersPerSecond(10.0), 1e-3)
        assertEquals(1.0, PrecipitationUnit.INCHES.fromMillimeters(25.4), 1e-9)
    }
}

class AgeTest {
    private val now = Instant.parse("2026-10-15T12:00:00Z")

    @Test fun bucketsAndEnglishText() {
        val phrases = AgePhrases()
        assertEquals("just now", phrases.format(Age.between(now.minusSeconds(20), now)))
        assertEquals("25 min ago", phrases.format(Age.between(now.minusSeconds(25 * 60), now)))
        assertEquals("3 h ago", phrases.format(Age.between(now.minusSeconds(3 * 3600 + 59 * 60), now)))
        assertEquals("47 h ago", phrases.format(Age.between(now.minusSeconds(47 * 3600), now)))
        assertEquals("2 days ago", phrases.format(Age.between(now.minusSeconds(49 * 3600), now)))
    }
}

class DateTimeTextTest {
    @Test fun twentyFourAndTwelveHourClocks() {
        val date = LocalDate.of(2026, 10, 16)
        val h24 = PatternDateTimeText(use24Hour = true)
        val h12 = PatternDateTimeText(locale = java.util.Locale.US, use24Hour = false)
        assertEquals("09:30", h24.time(LocalTime.of(9, 30)))
        assertEquals("9:30 AM", h12.time(LocalTime.of(9, 30)))
        assertEquals("14", h24.hour(LocalTime.of(14, 0)))
        assertEquals("2 PM", h12.hour(LocalTime.of(14, 0)))
        assertEquals("Friday 16 October", h24.longDate(date))
        assertEquals("Fri", h24.shortWeekday(date))
    }
}
