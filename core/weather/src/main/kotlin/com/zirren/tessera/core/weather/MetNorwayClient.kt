package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * MET Norway Locationforecast 2.0 client: free, no API key, data under CC BY 4.0.
 *
 * Follows MET's terms of service:
 * - identifies itself with a User-Agent that includes a contact URL,
 * - sends at most 4 decimals of latitude/longitude,
 * - honours `Expires` and revalidates with `If-Modified-Since` (a 304 costs MET almost nothing).
 */
class MetNorwayClient(
    private val transport: HttpTransport,
    private val userAgent: String,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    init {
        require(userAgent.isNotBlank()) { "MET Norway requires an identifying User-Agent" }
    }

    fun urlFor(point: GeoPoint): String {
        val rounded = point.rounded(4)
        val query = buildList {
            add("lat=" + GeoPoint.plain(rounded.latitude, 4))
            add("lon=" + GeoPoint.plain(rounded.longitude, 4))
            rounded.altitudeMeters?.let { add("altitude=$it") }
        }
        return endpoint + "?" + query.joinToString("&")
    }

    fun fetch(point: GeoPoint, validator: CacheValidator?, now: Instant): FetchOutcome {
        val headers = buildMap {
            put("User-Agent", userAgent)
            put("Accept", "application/json")
            validator?.lastModified?.let { put("If-Modified-Since", it) }
        }
        val response = try {
            transport.get(urlFor(point), headers)
        } catch (e: IOException) {
            return FetchOutcome.NetworkError(e.message ?: e.javaClass.simpleName)
        }

        val expires = response.header("Expires")?.let(::parseHttpDate)?.takeIf { it.isAfter(now) }
            ?: now.plus(DEFAULT_LIFETIME)
        val lastModified = response.header("Last-Modified")

        return when (response.code) {
            200, 203 -> {
                val body = response.body
                if (body.isNullOrBlank()) return FetchOutcome.Failed(response.code, "Empty response")
                val forecast = try {
                    MetNorwayParser.parse(body)
                } catch (e: WeatherParseException) {
                    return FetchOutcome.Failed(response.code, "Unreadable forecast: ${e.message}")
                }
                FetchOutcome.Updated(
                    body = body,
                    forecast = forecast,
                    validator = CacheValidator(lastModified, expires),
                    // 203 = this API version is deprecated; still usable, but worth surfacing in diagnostics.
                    deprecated = response.code == 203,
                )
            }
            304 -> FetchOutcome.NotModified(CacheValidator(lastModified ?: validator?.lastModified, expires))
            429 -> FetchOutcome.Throttled(retryAt = parseRetryAfter(response.header("Retry-After"), now) ?: now.plus(THROTTLE_BACKOFF))
            403 -> FetchOutcome.Failed(403, "MET Norway refused the request (check User-Agent)")
            else -> FetchOutcome.Failed(response.code, "HTTP ${response.code}")
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.met.no/weatherapi/locationforecast/2.0/complete"
        val DEFAULT_LIFETIME: Duration = Duration.ofMinutes(30)
        val THROTTLE_BACKOFF: Duration = Duration.ofMinutes(15)

        fun parseHttpDate(text: String): Instant? = try {
            ZonedDateTime.parse(text.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        } catch (e: DateTimeParseException) {
            null
        }

        fun parseRetryAfter(text: String?, now: Instant): Instant? {
            val value = text?.trim().orEmpty()
            if (value.isEmpty()) return null
            value.toLongOrNull()?.let { seconds -> return now.plusSeconds(seconds.coerceIn(0, 24 * 3600)) }
            return parseHttpDate(value)
        }
    }
}

/** What we remember between requests so MET can answer "304 Not Modified". */
data class CacheValidator(val lastModified: String?, val expires: Instant)

sealed interface FetchOutcome {
    class Updated(
        val body: String,
        val forecast: Forecast,
        val validator: CacheValidator,
        val deprecated: Boolean,
    ) : FetchOutcome {
        override fun toString(): String = "Updated(steps=${forecast.steps.size}, expires=${validator.expires}, deprecated=$deprecated)"
    }

    data class NotModified(val validator: CacheValidator) : FetchOutcome
    data class Throttled(val retryAt: Instant) : FetchOutcome
    data class Failed(val httpCode: Int, val message: String) : FetchOutcome
    data class NetworkError(val message: String) : FetchOutcome
}
