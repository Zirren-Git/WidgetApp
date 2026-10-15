package com.zirren.tessera.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A position on Earth. Altitude improves temperature accuracy in hilly places. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Int? = null,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }

    /**
     * Rounds both coordinates to [decimals] places.
     * Two decimals is about 1 km (what Tessera keeps for "Use my location");
     * four is about 11 m (the most MET Norway accepts).
     */
    fun rounded(decimals: Int): GeoPoint {
        require(decimals in 0..8) { "decimals must be 0..8" }
        return copy(latitude = round(latitude, decimals), longitude = round(longitude, decimals))
    }

    /** Great-circle distance in kilometres (haversine). */
    fun distanceKmTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
    }

    /** "58.2837,12.2886" — stable key for caches and change detection. */
    fun key(decimals: Int = 4): String =
        plain(latitude, decimals) + "," + plain(longitude, decimals)

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0088

        private fun round(value: Double, decimals: Int): Double =
            BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()

        /** Plain decimal text without exponent or trailing zeros: 12.3, not 1.23E1 or 12.3000. */
        fun plain(value: Double, decimals: Int): String =
            BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
