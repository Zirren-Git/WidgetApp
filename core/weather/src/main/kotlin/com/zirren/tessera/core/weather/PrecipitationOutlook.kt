package com.zirren.tessera.core.weather

import java.time.Duration
import java.time.Instant

/** The one line a widget can say about rain: "Rain from 14:00", "Snow until 16:00", "Dry for the next 12 hours". */
sealed interface PrecipitationOutlook {
    data class DryFor(val hours: Int) : PrecipitationOutlook
    data class StartsAt(val time: Instant, val type: PrecipitationType) : PrecipitationOutlook

    /** [until] is the first dry hour, or null if it stays wet beyond the horizon. */
    data class Ongoing(val type: PrecipitationType, val until: Instant?, val horizonHours: Int) : PrecipitationOutlook

    companion object {
        /** Below this, MET's hourly amount is just noise. */
        const val WET_THRESHOLD_MM = 0.1

        fun from(steps: List<ForecastStep>, now: Instant, horizonHours: Int = 12): PrecipitationOutlook? {
            val hours = steps
                .filter { it.isHourly && it.time.plus(Duration.ofHours(1)).isAfter(now) }
                .take(horizonHours)
            if (hours.isEmpty()) return null

            val firstWet = hours.indexOfFirst(::isWet)
            if (firstWet < 0) return DryFor(hours.size)

            val type = typeOf(hours[firstWet])
            if (firstWet == 0) {
                val firstDry = hours.indexOfFirst { !isWet(it) }
                return Ongoing(type, if (firstDry < 0) null else hours[firstDry].time, hours.size)
            }
            return StartsAt(hours[firstWet].time, type)
        }

        fun isWet(step: ForecastStep): Boolean {
            val hour = step.next1Hours ?: return false
            val amount = hour.precipitationAmount
            return if (amount != null) amount >= WET_THRESHOLD_MM else hour.symbol?.isWet == true
        }

        private fun typeOf(step: ForecastStep): PrecipitationType =
            step.next1Hours?.symbol?.precipitation
                ?: step.next6Hours?.symbol?.precipitation
                ?: when {
                    (step.airTemperature ?: 5.0) <= 0.0 -> PrecipitationType.SNOW
                    else -> PrecipitationType.RAIN
                }
    }
}
