package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import com.zirren.tessera.core.model.Place
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * City search through Open-Meteo's geocoding API (place data from GeoNames, CC BY 4.0).
 * Free for non-commercial use, which fits an open-source app with no revenue.
 * Lets people pick a place without granting location permission.
 */
class PlaceSearch(
    private val transport: HttpTransport,
    private val userAgent: String,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    fun search(query: String, languageCode: String = "en", count: Int = 8): PlaceSearchOutcome {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return PlaceSearchOutcome.Found(emptyList())

        val url = endpoint +
            "?name=" + URLEncoder.encode(trimmed, "UTF-8") +
            "&count=" + count.coerceIn(1, 20) +
            "&language=" + URLEncoder.encode(languageCode.ifBlank { "en" }, "UTF-8") +
            "&format=json"

        val response = try {
            transport.get(url, mapOf("User-Agent" to userAgent, "Accept" to "application/json"))
        } catch (e: IOException) {
            return PlaceSearchOutcome.Failed(e.message ?: "No connection")
        }
        if (response.code != 200) return PlaceSearchOutcome.Failed("HTTP ${response.code}")
        val body = response.body ?: return PlaceSearchOutcome.Failed("Empty response")
        return try {
            PlaceSearchOutcome.Found(parse(body))
        } catch (e: WeatherParseException) {
            PlaceSearchOutcome.Failed(e.message ?: "Unreadable response")
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://geocoding-api.open-meteo.com/v1/search"
        const val MIN_QUERY_LENGTH = 2

        @Throws(WeatherParseException::class)
        fun parse(body: String): List<Place> {
            val root = try {
                Json.parseToJsonElement(body) as? JsonObject
            } catch (e: IllegalArgumentException) {
                throw WeatherParseException("Not JSON", e)
            } ?: throw WeatherParseException("Expected a JSON object")

            // No matches: Open-Meteo omits "results" entirely.
            val results = root["results"] as? JsonArray ?: return emptyList()
            return results.mapNotNull { element ->
                val item = element as? JsonObject ?: return@mapNotNull null
                val name = item.text("name") ?: return@mapNotNull null
                val latitude = item.number("latitude") ?: return@mapNotNull null
                val longitude = item.number("longitude") ?: return@mapNotNull null
                if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@mapNotNull null
                Place(
                    name = name,
                    point = GeoPoint(latitude, longitude, item.number("elevation")?.roundToInt()),
                    region = item.text("admin1"),
                    country = item.text("country"),
                    countryCode = item.text("country_code")?.uppercase(),
                    timeZoneId = item.text("timezone"),
                )
            }
        }

        private fun JsonObject.text(name: String): String? =
            (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        private fun JsonObject.number(name: String): Double? =
            (this[name] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
    }
}

sealed interface PlaceSearchOutcome {
    data class Found(val places: List<Place>) : PlaceSearchOutcome
    data class Failed(val message: String) : PlaceSearchOutcome
}
