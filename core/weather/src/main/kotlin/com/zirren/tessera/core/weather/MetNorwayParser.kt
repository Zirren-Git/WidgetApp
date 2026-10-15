package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

/**
 * Reads MET Norway Locationforecast 2.0 JSON (`compact` or `complete`).
 * Uses the JSON tree API so a malformed time step is skipped instead of failing the whole forecast.
 */
object MetNorwayParser {

    @Throws(WeatherParseException::class)
    fun parse(body: String): Forecast {
        val root = try {
            Json.parseToJsonElement(body) as? JsonObject
        } catch (e: IllegalArgumentException) {
            throw WeatherParseException("Not JSON", e)
        } ?: throw WeatherParseException("Expected a JSON object")

        val coordinates = root.obj("geometry")?.get("coordinates") as? JsonArray
        val longitude = coordinates?.getOrNull(0)?.number()
        val latitude = coordinates?.getOrNull(1)?.number()
        val altitude = coordinates?.getOrNull(2)?.number()
        if (latitude == null || longitude == null) throw WeatherParseException("Missing coordinates")
        val point = try {
            GeoPoint(latitude, longitude, altitude?.roundToInt())
        } catch (e: IllegalArgumentException) {
            throw WeatherParseException("Coordinates out of range", e)
        }

        val properties = root.obj("properties") ?: throw WeatherParseException("Missing properties")
        val updatedAt = properties.obj("meta")?.string("updated_at")?.let(::instantOrNull)
        val series = properties["timeseries"] as? JsonArray ?: throw WeatherParseException("Missing timeseries")

        val steps = series.mapNotNull { (it as? JsonObject)?.let(::parseStep) }.sortedBy { it.time }
        return Forecast(point, updatedAt, steps)
    }

    private fun parseStep(step: JsonObject): ForecastStep? {
        val time = step.string("time")?.let(::instantOrNull) ?: return null
        val data = step.obj("data") ?: return null
        val instant = data.obj("instant")?.obj("details")
        return ForecastStep(
            time = time,
            airTemperature = instant?.double("air_temperature"),
            windSpeed = instant?.double("wind_speed"),
            windGust = instant?.double("wind_speed_of_gust"),
            windFromDirection = instant?.double("wind_from_direction"),
            relativeHumidity = instant?.double("relative_humidity"),
            cloudAreaFraction = instant?.double("cloud_area_fraction"),
            dewPoint = instant?.double("dew_point_temperature"),
            uvIndexClearSky = instant?.double("ultraviolet_index_clear_sky"),
            next1Hours = data.obj("next_1_hours")?.let(::parsePeriod),
            next6Hours = data.obj("next_6_hours")?.let(::parsePeriod),
            next12Hours = data.obj("next_12_hours")?.let(::parsePeriod),
        )
    }

    private fun parsePeriod(period: JsonObject): PeriodForecast {
        val details = period.obj("details")
        return PeriodForecast(
            symbol = WeatherSymbol.parse(period.obj("summary")?.string("symbol_code")),
            precipitationAmount = details?.double("precipitation_amount"),
            probabilityOfPrecipitation = details?.double("probability_of_precipitation"),
            airTemperatureMax = details?.double("air_temperature_max"),
            airTemperatureMin = details?.double("air_temperature_min"),
        )
    }

    private fun instantOrNull(text: String): Instant? = try {
        Instant.parse(text)
    } catch (e: DateTimeParseException) {
        null
    }

    private fun JsonObject.obj(name: String): JsonObject? = this[name] as? JsonObject
    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.double(name: String): Double? = this[name]?.number()
    private fun JsonElement.number(): Double? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
}
