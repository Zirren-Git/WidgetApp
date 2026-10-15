package com.zirren.tessera.core.weather

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * "Feels like" temperature in °C.
 * - Cold and windy: wind chill (Environment Canada / US NWS JAG-TI formula), valid at ≤ 10 °C and wind > 4.8 km/h.
 * - Hot and humid: heat index (NOAA Rothfusz regression with NOAA's adjustments), valid from ~27 °C at ≥ 40 % humidity.
 * - Otherwise: the air temperature itself.
 */
object FeelsLike {

    fun celsius(airTemperature: Double, windSpeedMetersPerSecond: Double?, relativeHumidity: Double?): Double {
        val windKmh = (windSpeedMetersPerSecond ?: 0.0) * 3.6
        if (airTemperature <= 10.0 && windKmh > 4.8) {
            val v = windKmh.pow(0.16)
            val chill = 13.12 + 0.6215 * airTemperature - 11.37 * v + 0.3965 * airTemperature * v
            return minOf(chill, airTemperature)
        }
        if (airTemperature >= 26.7 && relativeHumidity != null && relativeHumidity >= 40.0) {
            return maxOf(heatIndex(airTemperature, relativeHumidity.coerceAtMost(100.0)), airTemperature)
        }
        return airTemperature
    }

    private fun heatIndex(celsius: Double, humidity: Double): Double {
        val t = celsius * 9.0 / 5.0 + 32.0
        val rh = humidity
        var hi = -42.379 + 2.04901523 * t + 10.14333127 * rh -
            0.22475541 * t * rh - 0.00683783 * t * t - 0.05481717 * rh * rh +
            0.00122874 * t * t * rh + 0.00085282 * t * rh * rh - 0.00000199 * t * t * rh * rh
        if (rh < 13 && t in 80.0..112.0) {
            hi -= ((13 - rh) / 4) * sqrt((17 - abs(t - 95)) / 17)
        } else if (rh > 85 && t in 80.0..87.0) {
            hi += ((rh - 85) / 10) * ((87 - t) / 5)
        }
        return (hi - 32.0) * 5.0 / 9.0
    }
}
