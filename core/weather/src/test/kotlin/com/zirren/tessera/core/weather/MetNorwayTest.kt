package com.zirren.tessera.core.weather

import com.zirren.tessera.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Instant

class MetNorwayParserTest {

    private val forecast = MetNorwayParser.parse(TestForecasts.metJson)

    @Test fun readsPointAndModelTime() {
        assertEquals(GeoPoint(58.2837, 12.2886, 45), forecast.point)
        assertEquals(Instant.parse("2026-10-15T07:31:52Z"), forecast.modelUpdatedAt)
    }

    @Test fun skipsBrokenStepsAndSortsByTime() {
        assertEquals(3, forecast.steps.size)
        assertEquals(
            listOf("2026-10-15T08:00:00Z", "2026-10-15T09:00:00Z", "2026-10-18T12:00:00Z").map(Instant::parse),
            forecast.steps.map { it.time },
        )
    }

    @Test fun readsCompleteEndpointDetails() {
        val step = forecast.steps[1]
        assertEquals(9.8, step.airTemperature!!, 1e-9)
        assertEquals(9.7, step.windGust!!, 1e-9)
        assertEquals(7.1, step.dewPoint!!, 1e-9)
        assertEquals(0.6, step.uvIndexClearSky!!, 1e-9)
        val hour = step.next1Hours!!
        assertEquals("lightrain", hour.symbol!!.code)
        assertEquals(0.4, hour.precipitationAmount!!, 1e-9)
        assertEquals(71.0, hour.probabilityOfPrecipitation!!, 1e-9)
        assertEquals(11.2, step.next6Hours!!.airTemperatureMax!!, 1e-9)
        assertTrue(step.isHourly)
    }

    @Test fun sixHourlyStepsHaveNoHourlyPeriod() {
        val step = forecast.steps.last()
        assertNull(step.next1Hours)
        assertEquals(PrecipitationType.SLEET, step.next6Hours!!.symbol!!.precipitation)
        // Unknown future symbol codes become null instead of failing.
        assertNull(step.next12Hours!!.symbol)
    }

    @Test fun rejectsNonForecastBodies() {
        for (body in listOf("<html>Down for maintenance</html>", "[]", """{"properties":{}}""")) {
            try {
                MetNorwayParser.parse(body)
                fail("Expected failure for $body")
            } catch (expected: WeatherParseException) {
            }
        }
    }
}

class MetNorwayClientTest {

    private val now = Instant.parse("2026-10-15T08:10:00Z")
    private val point = GeoPoint(58.283651, 12.288642, 45)

    private class FakeTransport(val respond: (String, Map<String, String>) -> HttpResponse) : HttpTransport {
        var lastUrl: String? = null
        var lastHeaders: Map<String, String> = emptyMap()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastHeaders = headers
            return respond(url, headers)
        }
    }

    private fun client(transport: HttpTransport) = MetNorwayClient(transport, "Tessera/0.1.0 github.com/Zirren-Git/WidgetApp")

    @Test fun urlUsesAtMostFourDecimalsAndAltitude() {
        assertEquals(
            "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=58.2837&lon=12.2886&altitude=45",
            client(FakeTransport { _, _ -> HttpResponse(200, emptyMap(), null) }).urlFor(point),
        )
    }

    @Test fun okResponseIsParsedWithCacheValidators() {
        val transport = FakeTransport { _, _ ->
            HttpResponse(
                200,
                mapOf("Expires" to listOf("Thu, 15 Oct 2026 08:41:13 GMT"), "Last-Modified" to listOf("Thu, 15 Oct 2026 07:31:52 GMT")),
                TestForecasts.metJson,
            )
        }
        val outcome = client(transport).fetch(point, null, now)
        assertTrue(outcome is FetchOutcome.Updated)
        outcome as FetchOutcome.Updated
        assertEquals(3, outcome.forecast.steps.size)
        assertEquals(Instant.parse("2026-10-15T08:41:13Z"), outcome.validator.expires)
        assertEquals("Thu, 15 Oct 2026 07:31:52 GMT", outcome.validator.lastModified)
        assertEquals("Tessera/0.1.0 github.com/Zirren-Git/WidgetApp", transport.lastHeaders["User-Agent"])
        assertNull(transport.lastHeaders["If-Modified-Since"])
    }

    @Test fun revalidationSendsIfModifiedSinceAndHandles304() {
        val transport = FakeTransport { _, _ ->
            HttpResponse(304, mapOf("expires" to listOf("Thu, 15 Oct 2026 09:10:00 GMT")), null)
        }
        val validator = CacheValidator("Thu, 15 Oct 2026 07:31:52 GMT", now.minusSeconds(60))
        val outcome = client(transport).fetch(point, validator, now)
        assertEquals("Thu, 15 Oct 2026 07:31:52 GMT", transport.lastHeaders["If-Modified-Since"])
        assertEquals(
            FetchOutcome.NotModified(CacheValidator("Thu, 15 Oct 2026 07:31:52 GMT", Instant.parse("2026-10-15T09:10:00Z"))),
            outcome,
        )
    }

    @Test fun missingOrPastExpiresFallsBackToThirtyMinutes() {
        val transport = FakeTransport { _, _ -> HttpResponse(304, mapOf("Expires" to listOf("Thu, 01 Jan 2026 00:00:00 GMT")), null) }
        val outcome = client(transport).fetch(point, CacheValidator("x", now), now) as FetchOutcome.NotModified
        assertEquals(now.plus(MetNorwayClient.DEFAULT_LIFETIME), outcome.validator.expires)
    }

    @Test fun throttlingHonoursRetryAfter() {
        val transport = FakeTransport { _, _ -> HttpResponse(429, mapOf("Retry-After" to listOf("120")), null) }
        assertEquals(FetchOutcome.Throttled(now.plusSeconds(120)), client(transport).fetch(point, null, now))
    }

    @Test fun deprecatedApiStillDeliversData() {
        val transport = FakeTransport { _, _ -> HttpResponse(203, emptyMap(), TestForecasts.metJson) }
        val outcome = client(transport).fetch(point, null, now) as FetchOutcome.Updated
        assertTrue(outcome.deprecated)
    }

    @Test fun errorsBecomeOutcomesNotExceptions() {
        val down = FakeTransport { _, _ -> throw IOException("timeout") }
        assertEquals(FetchOutcome.NetworkError("timeout"), client(down).fetch(point, null, now))

        val broken = FakeTransport { _, _ -> HttpResponse(200, emptyMap(), "<html>oops</html>") }
        val failed = client(broken).fetch(point, null, now)
        assertTrue(failed is FetchOutcome.Failed)

        val server = FakeTransport { _, _ -> HttpResponse(500, emptyMap(), "") }
        assertEquals(FetchOutcome.Failed(500, "HTTP 500"), client(server).fetch(point, null, now))
    }

    @Test fun httpDatesParse() {
        assertEquals(Instant.parse("2026-10-15T08:41:13Z"), MetNorwayClient.parseHttpDate("Thu, 15 Oct 2026 08:41:13 GMT"))
        assertNull(MetNorwayClient.parseHttpDate("yesterday"))
        assertNotNull(MetNorwayClient.parseRetryAfter("Thu, 15 Oct 2026 08:41:13 GMT", now))
    }

    @Test fun requiresUserAgent() {
        val error = runCatching { MetNorwayClient(FakeTransport { _, _ -> HttpResponse(200, emptyMap(), null) }, " ") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
