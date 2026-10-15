package com.zirren.tessera.core.weather

import java.time.Duration
import java.time.Instant

/** Builders for forecast data in tests. */
internal object TestForecasts {

    fun hour(
        time: Instant,
        temperature: Double = 10.0,
        symbol: String = "cloudy",
        precipitation: Double = 0.0,
        wind: Double = 3.0,
        humidity: Double = 70.0,
    ) = ForecastStep(
        time = time,
        airTemperature = temperature,
        windSpeed = wind,
        windGust = null,
        windFromDirection = 225.0,
        relativeHumidity = humidity,
        cloudAreaFraction = 80.0,
        dewPoint = null,
        uvIndexClearSky = null,
        next1Hours = PeriodForecast(WeatherSymbol.parse(symbol), precipitation, null, null, null),
        next6Hours = PeriodForecast(WeatherSymbol.parse(symbol), precipitation * 6, null, temperature + 1, temperature - 1),
        next12Hours = PeriodForecast(WeatherSymbol.parse(symbol), null, null, null, null),
    )

    fun sixHourly(time: Instant, temperature: Double, max: Double, min: Double, symbol: String = "partlycloudy_day", precipitation: Double = 0.0) =
        ForecastStep(
            time = time,
            airTemperature = temperature,
            windSpeed = 4.0,
            windGust = null,
            windFromDirection = 200.0,
            relativeHumidity = 75.0,
            cloudAreaFraction = 50.0,
            dewPoint = null,
            uvIndexClearSky = null,
            next1Hours = null,
            next6Hours = PeriodForecast(WeatherSymbol.parse(symbol), precipitation, null, max, min),
            next12Hours = PeriodForecast(WeatherSymbol.parse(symbol), null, null, null, null),
        )

    /** [count] hourly steps from [start], with per-hour overrides. */
    fun hours(start: Instant, count: Int, spec: (index: Int) -> Triple<Double, String, Double> = { Triple(10.0, "cloudy", 0.0) }): List<ForecastStep> =
        (0 until count).map { i ->
            val (temp, symbol, precip) = spec(i)
            hour(start.plus(Duration.ofHours(i.toLong())), temp, symbol, precip)
        }

    /** A realistic MET Norway "complete" response: two hourly steps, one 6-hourly, one broken step. */
    val metJson = """
        {
          "type": "Feature",
          "geometry": { "type": "Point", "coordinates": [12.2886, 58.2837, 45] },
          "properties": {
            "meta": {
              "updated_at": "2026-10-15T07:31:52Z",
              "units": { "air_temperature": "celsius", "precipitation_amount": "mm", "wind_speed": "m/s" }
            },
            "timeseries": [
              {
                "time": "2026-10-15T09:00:00Z",
                "data": {
                  "instant": { "details": {
                    "air_pressure_at_sea_level": 1011.9, "air_temperature": 9.8, "cloud_area_fraction": 100.0,
                    "dew_point_temperature": 7.1, "relative_humidity": 83.4, "ultraviolet_index_clear_sky": 0.6,
                    "wind_from_direction": 214.3, "wind_speed": 5.1, "wind_speed_of_gust": 9.7
                  } },
                  "next_12_hours": { "summary": { "symbol_code": "rain" }, "details": { "probability_of_precipitation": 92.0 } },
                  "next_1_hours": { "summary": { "symbol_code": "lightrain" }, "details": {
                    "precipitation_amount": 0.4, "probability_of_precipitation": 71.0
                  } },
                  "next_6_hours": { "summary": { "symbol_code": "rain" }, "details": {
                    "air_temperature_max": 11.2, "air_temperature_min": 9.6, "precipitation_amount": 3.9
                  } }
                }
              },
              {
                "time": "2026-10-15T08:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 9.4, "wind_speed": 4.6, "relative_humidity": 85.0 } },
                  "next_1_hours": { "summary": { "symbol_code": "cloudy" }, "details": { "precipitation_amount": 0.0 } }
                }
              },
              { "time": "not-a-time", "data": {} },
              {
                "time": "2026-10-18T12:00:00Z",
                "data": {
                  "instant": { "details": { "air_temperature": 7.0 } },
                  "next_6_hours": { "summary": { "symbol_code": "lightssleetshowersandthunder_day" }, "details": {
                    "air_temperature_max": 8.1, "air_temperature_min": 5.2, "precipitation_amount": 1.5
                  } },
                  "next_12_hours": { "summary": { "symbol_code": "some_future_symbol" } }
                }
              }
            ]
          }
        }
    """.trimIndent()
}
