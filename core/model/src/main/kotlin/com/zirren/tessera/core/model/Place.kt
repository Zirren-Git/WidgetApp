package com.zirren.tessera.core.model

/** A named place, from a city search or from "Use my location". */
data class Place(
    val name: String,
    val point: GeoPoint,
    /** First-level region, e.g. "Västra Götaland". */
    val region: String? = null,
    val country: String? = null,
    /** ISO 3166-1 alpha-2, upper case, e.g. "SE". */
    val countryCode: String? = null,
    /** IANA zone, e.g. "Europe/Stockholm". */
    val timeZoneId: String? = null,
) {
    /** What beginners recognise: "Trollhättan, Västra Götaland". */
    val label: String
        get() = listOfNotNull(name, region?.takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) })
            .joinToString(", ")

    /** Longer form for search results: "Trollhättan, Västra Götaland, Sweden". */
    val longLabel: String
        get() = listOfNotNull(label, country?.takeIf { it.isNotBlank() }).joinToString(", ")
}
