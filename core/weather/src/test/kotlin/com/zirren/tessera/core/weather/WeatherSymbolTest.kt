package com.zirren.tessera.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSymbolTest {

    @Test fun drySymbolsWithDayPhase() {
        val symbol = WeatherSymbol.parse("clearsky_night")!!
        assertEquals(Sky.CLEAR, symbol.sky)
        assertEquals(DayPhase.NIGHT, symbol.phase)
        assertFalse(symbol.isWet)
        assertEquals("Clear sky", symbol.label)
        assertEquals("clear_sky", symbol.labelKey)
        assertEquals(WeatherIcon.CLEAR_NIGHT, symbol.icon)
    }

    @Test fun wetSymbolIsParsedIntoParts() {
        val symbol = WeatherSymbol.parse("heavyrainshowersandthunder_day")!!
        assertEquals(PrecipitationType.RAIN, symbol.precipitation)
        assertEquals(Intensity.HEAVY, symbol.intensity)
        assertTrue(symbol.showers)
        assertTrue(symbol.thunder)
        assertEquals("Heavy rain showers and thunder", symbol.label)
        assertEquals("heavy_rain_showers_and_thunder", symbol.labelKey)
        assertEquals(WeatherIcon.THUNDER, symbol.icon)
    }

    @Test fun labelsMatchMetNorwayWording() {
        val expected = mapOf(
            "fair_day" to "Fair",
            "partlycloudy_polartwilight" to "Partly cloudy",
            "cloudy" to "Cloudy",
            "fog" to "Fog",
            "lightrain" to "Light rain",
            "rain" to "Rain",
            "sleetshowers_night" to "Sleet showers",
            "lightsnowandthunder" to "Light snow and thunder",
            "heavysnow" to "Heavy snow",
        )
        expected.forEach { (code, label) -> assertEquals(code, label, WeatherSymbol.parse(code)?.label) }
    }

    @Test fun knownMetMisspellingsAreUnderstood() {
        val sleet = WeatherSymbol.parse("lightssleetshowersandthunder_day")!!
        assertEquals(PrecipitationType.SLEET, sleet.precipitation)
        assertEquals(Intensity.LIGHT, sleet.intensity)
        assertTrue(sleet.showers && sleet.thunder)

        val snow = WeatherSymbol.parse("lightssnowshowersandthunder_night")!!
        assertEquals(PrecipitationType.SNOW, snow.precipitation)
        assertEquals(Intensity.LIGHT, snow.intensity)

        // The correctly spelled codes must not be "fixed" into something else.
        assertEquals(PrecipitationType.SNOW, WeatherSymbol.parse("lightsnow")!!.precipitation)
        assertEquals(PrecipitationType.SLEET, WeatherSymbol.parse("lightsleet")!!.precipitation)
    }

    @Test fun iconsCollapseManyCodes() {
        assertEquals(WeatherIcon.DRIZZLE, WeatherSymbol.parse("lightrain")!!.icon)
        assertEquals(WeatherIcon.RAIN, WeatherSymbol.parse("rain")!!.icon)
        assertEquals(WeatherIcon.HEAVY_RAIN, WeatherSymbol.parse("heavyrain")!!.icon)
        assertEquals(WeatherIcon.SHOWERS_NIGHT, WeatherSymbol.parse("rainshowers_night")!!.icon)
        assertEquals(WeatherIcon.SHOWERS_DAY, WeatherSymbol.parse("lightrainshowers_polartwilight")!!.icon)
        assertEquals(WeatherIcon.PARTLY_CLOUDY_DAY, WeatherSymbol.parse("fair_day")!!.icon)
        assertEquals(WeatherIcon.PARTLY_CLOUDY_NIGHT, WeatherSymbol.parse("partlycloudy_night")!!.icon)
        assertEquals(WeatherIcon.SNOW, WeatherSymbol.parse("heavysnowshowers_day")!!.icon)
        assertEquals(WeatherIcon.SLEET, WeatherSymbol.parse("sleet")!!.icon)
        assertEquals(WeatherIcon.FOG, WeatherSymbol.parse("fog")!!.icon)
    }

    @Test fun unknownOrEmptyCodesReturnNull() {
        assertNull(WeatherSymbol.parse(null))
        assertNull(WeatherSymbol.parse(""))
        assertNull(WeatherSymbol.parse("some_future_symbol"))
        assertNull(WeatherSymbol.parse("rainbows"))
        assertNull(WeatherSymbol.parse("hail"))
    }

    @Test fun parsingIsCaseAndWhitespaceTolerant() {
        assertEquals(PrecipitationType.RAIN, WeatherSymbol.parse("  LightRain ")!!.precipitation)
    }
}
