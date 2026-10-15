package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import java.time.Instant

/** A parsed MET Norway Locationforecast. Units: °C, m/s, mm, %. */
data class Forecast(
    /** The point MET actually forecast for (it snaps to its model grid). */
    val point: GeoPoint,
    /** When the weather model behind this forecast ran. */
    val modelUpdatedAt: Instant?,
    /** Sorted by time. Hourly for roughly the first 2–3 days, then every 6 hours. */
    val steps: List<ForecastStep>,
)

data class ForecastStep(
    val time: Instant,
    val airTemperature: Double?,
    val windSpeed: Double?,
    val windGust: Double?,
    val windFromDirection: Double?,
    val relativeHumidity: Double?,
    val cloudAreaFraction: Double?,
    val dewPoint: Double?,
    val uvIndexClearSky: Double?,
    val next1Hours: PeriodForecast?,
    val next6Hours: PeriodForecast?,
    val next12Hours: PeriodForecast?,
) {
    /** True in the hourly part of the series. */
    val isHourly: Boolean get() = next1Hours != null
}

data class PeriodForecast(
    val symbol: WeatherSymbol?,
    val precipitationAmount: Double?,
    val probabilityOfPrecipitation: Double?,
    val airTemperatureMax: Double?,
    val airTemperatureMin: Double?,
)

class WeatherParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
