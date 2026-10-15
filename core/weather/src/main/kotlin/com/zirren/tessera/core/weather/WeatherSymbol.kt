package com.zirren.tessera.core.weather

enum class Sky { CLEAR, FAIR, PARTLY_CLOUDY, CLOUDY, FOG }

enum class PrecipitationType { RAIN, SLEET, SNOW }

enum class Intensity { LIGHT, MODERATE, HEAVY }

enum class DayPhase { DAY, NIGHT, POLAR_TWILIGHT, NONE }

/** Tessera's own icon set. Several MET symbols share one icon. */
enum class WeatherIcon {
    CLEAR_DAY, CLEAR_NIGHT, PARTLY_CLOUDY_DAY, PARTLY_CLOUDY_NIGHT, CLOUDY, FOG,
    DRIZZLE, RAIN, HEAVY_RAIN, SHOWERS_DAY, SHOWERS_NIGHT, SLEET, SNOW, THUNDER,
}

/**
 * A MET Norway weather symbol such as `lightrainshowers_day` or `heavysnowandthunder`.
 * The code is parsed into parts, so the app never needs a table of all ~80 codes.
 */
data class WeatherSymbol(
    val code: String,
    /** Set for dry symbols. */
    val sky: Sky?,
    /** Set for wet symbols. */
    val precipitation: PrecipitationType?,
    val intensity: Intensity?,
    val showers: Boolean,
    val thunder: Boolean,
    val phase: DayPhase,
) {
    val isWet: Boolean get() = precipitation != null

    /** Stable key for translations, e.g. `light_rain_showers_and_thunder`. */
    val labelKey: String
        get() = words().joinToString("_") { it.lowercase() }

    /** English label in MET Norway's wording, e.g. "Light rain showers and thunder". */
    val label: String
        get() = words().joinToString(" ").replaceFirstChar { it.uppercase() }

    val icon: WeatherIcon
        get() {
            val night = phase == DayPhase.NIGHT
            return when {
                thunder -> WeatherIcon.THUNDER
                precipitation == PrecipitationType.SNOW -> WeatherIcon.SNOW
                precipitation == PrecipitationType.SLEET -> WeatherIcon.SLEET
                precipitation == PrecipitationType.RAIN && showers ->
                    if (night) WeatherIcon.SHOWERS_NIGHT else WeatherIcon.SHOWERS_DAY
                precipitation == PrecipitationType.RAIN -> when (intensity) {
                    Intensity.LIGHT -> WeatherIcon.DRIZZLE
                    Intensity.HEAVY -> WeatherIcon.HEAVY_RAIN
                    else -> WeatherIcon.RAIN
                }
                sky == Sky.CLEAR -> if (night) WeatherIcon.CLEAR_NIGHT else WeatherIcon.CLEAR_DAY
                sky == Sky.FAIR || sky == Sky.PARTLY_CLOUDY ->
                    if (night) WeatherIcon.PARTLY_CLOUDY_NIGHT else WeatherIcon.PARTLY_CLOUDY_DAY
                sky == Sky.FOG -> WeatherIcon.FOG
                else -> WeatherIcon.CLOUDY
            }
        }

    private fun words(): List<String> {
        if (sky != null) {
            return when (sky) {
                Sky.CLEAR -> listOf("clear", "sky")
                Sky.FAIR -> listOf("fair")
                Sky.PARTLY_CLOUDY -> listOf("partly", "cloudy")
                Sky.CLOUDY -> listOf("cloudy")
                Sky.FOG -> listOf("fog")
            }
        }
        return buildList {
            when (intensity) {
                Intensity.LIGHT -> add("light")
                Intensity.HEAVY -> add("heavy")
                else -> Unit
            }
            add(precipitation!!.name.lowercase())
            if (showers) add("showers")
            if (thunder) { add("and"); add("thunder") }
        }
    }

    companion object {
        /** Returns null for codes Tessera doesn't recognise, never throws. */
        fun parse(code: String?): WeatherSymbol? {
            val normalized = code?.trim()?.lowercase().orEmpty()
            if (normalized.isEmpty()) return null

            val (base, phase) = splitPhase(normalized)
            dry(base)?.let { sky ->
                return WeatherSymbol(normalized, sky, null, null, showers = false, thunder = false, phase = phase)
            }

            var rest = fixKnownMisspellings(base)
            val intensity = when {
                rest.startsWith("light") -> Intensity.LIGHT.also { rest = rest.removePrefix("light") }
                rest.startsWith("heavy") -> Intensity.HEAVY.also { rest = rest.removePrefix("heavy") }
                else -> Intensity.MODERATE
            }
            val type = PrecipitationType.entries.firstOrNull { rest.startsWith(it.name.lowercase()) } ?: return null
            rest = rest.removePrefix(type.name.lowercase())

            val (showers, thunder) = when (rest) {
                "" -> false to false
                "showers" -> true to false
                "andthunder" -> false to true
                "showersandthunder" -> true to true
                else -> return null
            }
            return WeatherSymbol(normalized, null, type, intensity, showers, thunder, phase)
        }

        private fun splitPhase(code: String): Pair<String, DayPhase> = when {
            code.endsWith("_day") -> code.removeSuffix("_day") to DayPhase.DAY
            code.endsWith("_night") -> code.removeSuffix("_night") to DayPhase.NIGHT
            code.endsWith("_polartwilight") -> code.removeSuffix("_polartwilight") to DayPhase.POLAR_TWILIGHT
            else -> code to DayPhase.NONE
        }

        private fun dry(base: String): Sky? = when (base) {
            "clearsky" -> Sky.CLEAR
            "fair" -> Sky.FAIR
            "partlycloudy" -> Sky.PARTLY_CLOUDY
            "cloudy" -> Sky.CLOUDY
            "fog" -> Sky.FOG
            else -> null
        }

        /**
         * MET Norway keeps two historic misspellings for compatibility:
         * `lightssleetshowersandthunder` and `lightssnowshowersandthunder`.
         */
        private fun fixKnownMisspellings(base: String): String {
            if (!base.startsWith("lights")) return base
            val afterLights = base.removePrefix("lights")
            return if (afterLights.startsWith("sleet") || afterLights.startsWith("snow")) "light$afterLights" else base
        }
    }
}
