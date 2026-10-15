package com.zirren.tessera.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchTest {

    private val body = """
        {
          "results": [
            {
              "id": 2692475, "name": "Trollhättan", "latitude": 58.28365, "longitude": 12.28864,
              "elevation": 45.0, "feature_code": "PPLA2", "country_code": "SE", "timezone": "Europe/Stockholm",
              "country": "Sweden", "admin1": "Västra Götaland"
            },
            { "id": 1, "name": "Broken", "latitude": "not a number", "longitude": 1.0 },
            { "id": 2, "name": "Nowhere", "latitude": 95.0, "longitude": 1.0 }
          ],
          "generationtime_ms": 0.7
        }
    """.trimIndent()

    @Test fun parsesPlacesAndSkipsBadEntries() {
        val places = PlaceSearch.parse(body)
        assertEquals(1, places.size)
        val place = places.single()
        assertEquals("Trollhättan, Västra Götaland, Sweden", place.longLabel)
        assertEquals("SE", place.countryCode)
        assertEquals("Europe/Stockholm", place.timeZoneId)
        assertEquals(45, place.point.altitudeMeters)
    }

    @Test fun noResultsKeyMeansNoMatches() {
        assertTrue(PlaceSearch.parse("""{"generationtime_ms":0.3}""").isEmpty())
    }

    @Test fun encodesQueryAndSkipsTooShortQueries() {
        var requested: String? = null
        val search = PlaceSearch({ url, _ -> requested = url; HttpResponse(200, emptyMap(), body) }, "Tessera/test")

        assertEquals(PlaceSearchOutcome.Found(emptyList()), search.search(" t "))
        assertEquals(null, requested)

        val outcome = search.search("Trollhättan", languageCode = "sv", count = 5)
        assertEquals(
            "https://geocoding-api.open-meteo.com/v1/search?name=Trollh%C3%A4ttan&count=5&language=sv&format=json",
            requested,
        )
        assertTrue(outcome is PlaceSearchOutcome.Found && outcome.places.size == 1)
    }

    @Test fun httpErrorsAreReported() {
        val search = PlaceSearch({ _, _ -> HttpResponse(503, emptyMap(), "") }, "Tessera/test")
        assertEquals(PlaceSearchOutcome.Failed("HTTP 503"), search.search("Oslo"))
    }
}
