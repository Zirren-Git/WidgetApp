package com.zirren.tessera.core.model

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F");

    fun fromCelsius(celsius: Double): Double = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    }
}

enum class WindSpeedUnit(val symbol: String) {
    METERS_PER_SECOND("m/s"),
    KILOMETERS_PER_HOUR("km/h"),
    MILES_PER_HOUR("mph"),
    KNOTS("kn");

    fun fromMetersPerSecond(metersPerSecond: Double): Double = when (this) {
        METERS_PER_SECOND -> metersPerSecond
        KILOMETERS_PER_HOUR -> metersPerSecond * 3.6
        MILES_PER_HOUR -> metersPerSecond * 2.236936292
        KNOTS -> metersPerSecond * 1.943844492
    }
}

enum class PrecipitationUnit(val symbol: String) {
    MILLIMETERS("mm"),
    INCHES("in");

    fun fromMillimeters(millimeters: Double): Double = when (this) {
        MILLIMETERS -> millimeters
        INCHES -> millimeters / 25.4
    }
}

data class UnitPreferences(
    val temperature: TemperatureUnit,
    val windSpeed: WindSpeedUnit,
    val precipitation: PrecipitationUnit,
) {
    companion object {
        private val FAHRENHEIT_REGIONS = setOf(
            "US", "PR", "GU", "VI", "AS", "MP", "UM", "BS", "BZ", "KY", "LR", "PW", "FM", "MH",
        )
        private val MPH_REGIONS = FAHRENHEIT_REGIONS + setOf("GB", "IM", "JE", "GG")

        // Where national weather services give wind in metres per second.
        private val METERS_PER_SECOND_REGIONS = setOf(
            "SE", "NO", "FI", "DK", "IS", "FO", "GL", "AX", "SJ",
            "EE", "LV", "LT", "RU", "BY", "UA", "KZ",
            "JP", "KR", "CN", "TW",
        )

        /**
         * Sensible defaults for a phone set to [countryCode] (ISO alpha-2).
         * A phone in Sweden gets °C and m/s, the US gets °F, mph and inches, the UK gets °C and mph.
         */
        fun defaultsFor(countryCode: String?): UnitPreferences {
            val region = countryCode?.trim()?.uppercase().orEmpty()
            return UnitPreferences(
                temperature = if (region in FAHRENHEIT_REGIONS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS,
                windSpeed = when (region) {
                    in MPH_REGIONS -> WindSpeedUnit.MILES_PER_HOUR
                    in METERS_PER_SECOND_REGIONS -> WindSpeedUnit.METERS_PER_SECOND
                    else -> WindSpeedUnit.KILOMETERS_PER_HOUR
                },
                precipitation = if (region in FAHRENHEIT_REGIONS) PrecipitationUnit.INCHES else PrecipitationUnit.MILLIMETERS,
            )
        }
    }
}
