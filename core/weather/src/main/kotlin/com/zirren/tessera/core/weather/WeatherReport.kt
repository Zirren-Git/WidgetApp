package com.zirren.tessera.core.weather

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/** Everything a widget or the forecast screen shows, derived from one [Forecast]. */
data class WeatherReport(
    val current: CurrentConditions?,
    /** Rest of today in the phone's time zone (MET forecasts start at the current hour). */
    val today: DaySummary?,
    val hourly: List<HourForecast>,
    /** Today first, then the following days. */
    val daily: List<DaySummary>,
    val outlook: PrecipitationOutlook?,
    val sun: SunEvents?,
    val modelUpdatedAt: Instant?,
)

data class CurrentConditions(
    val time: Instant,
    val temperature: Double,
    val feelsLike: Double,
    val symbol: WeatherSymbol?,
    val windSpeed: Double?,
    val windGust: Double?,
    val windFromDirection: Double?,
    val humidity: Double?,
    val uvIndex: Double?,
    val precipitationNextHour: Double?,
)

data class HourForecast(
    val time: Instant,
    val temperature: Double?,
    val symbol: WeatherSymbol?,
    val precipitation: Double?,
    val precipitationProbability: Double?,
)

data class DaySummary(
    val date: LocalDate,
    val high: Double?,
    val low: Double?,
    val symbol: WeatherSymbol?,
    val precipitation: Double?,
)

object WeatherReporter {

    fun report(
        forecast: Forecast,
        now: Instant,
        zone: ZoneId,
        hourlyCount: Int = 24,
        dayCount: Int = 9,
    ): WeatherReport {
        val steps = forecast.steps
        val currentStep = currentStep(steps, now)
        val days = dailySummaries(steps, now, zone, dayCount)
        val today = now.atZone(zone).toLocalDate()
        return WeatherReport(
            current = currentStep?.let(::currentConditions),
            today = days.firstOrNull { it.date == today },
            hourly = steps
                .filter { it.isHourly && (currentStep == null || !it.time.isBefore(currentStep.time)) }
                .filter { it.time.plus(Duration.ofHours(1)).isAfter(now) }
                .take(hourlyCount)
                .map { HourForecast(it.time, it.airTemperature, it.next1Hours?.symbol, it.next1Hours?.precipitationAmount, it.next1Hours?.probabilityOfPrecipitation) },
            daily = days,
            outlook = PrecipitationOutlook.from(steps, now),
            sun = SunCalculator.events(today, forecast.point.latitude, forecast.point.longitude),
            modelUpdatedAt = forecast.modelUpdatedAt,
        )
    }

    /**
     * The step covering "now". A forecast fetched hours ago still contains the current hour,
     * so cached data keeps showing the right hour instead of the hour it was fetched.
     */
    fun currentStep(steps: List<ForecastStep>, now: Instant): ForecastStep? {
        val started = steps.lastOrNull { !it.time.isAfter(now) }
        if (started != null && Duration.between(started.time, now) <= MAX_STEP_AGE) return started
        val upcoming = steps.firstOrNull { it.time.isAfter(now) }
        return upcoming?.takeIf { Duration.between(now, it.time) <= MAX_STEP_AGE }
    }

    private fun currentConditions(step: ForecastStep): CurrentConditions? {
        val temperature = step.airTemperature ?: return null
        return CurrentConditions(
            time = step.time,
            temperature = temperature,
            feelsLike = FeelsLike.celsius(temperature, step.windSpeed, step.relativeHumidity),
            symbol = step.next1Hours?.symbol ?: step.next6Hours?.symbol ?: step.next12Hours?.symbol,
            windSpeed = step.windSpeed,
            windGust = step.windGust,
            windFromDirection = step.windFromDirection,
            humidity = step.relativeHumidity,
            uvIndex = step.uvIndexClearSky,
            precipitationNextHour = step.next1Hours?.precipitationAmount,
        )
    }

    private fun dailySummaries(steps: List<ForecastStep>, now: Instant, zone: ZoneId, dayCount: Int): List<DaySummary> {
        val relevant = steps.filter { stepEnd(it).isAfter(now) }
        if (relevant.isEmpty()) return emptyList()
        return relevant
            .groupBy { it.time.atZone(zone).toLocalDate() }
            .toSortedMap()
            .entries
            .take(dayCount)
            .map { (date, daySteps) -> summarize(date, daySteps, zone) }
    }

    private fun summarize(date: LocalDate, daySteps: List<ForecastStep>, zone: ZoneId): DaySummary {
        val temperatures = buildList {
            for (step in daySteps) {
                step.airTemperature?.let(::add)
                if (!step.isHourly) {
                    // 6-hourly part: MET gives the period's min/max, which catch peaks between steps.
                    step.next6Hours?.airTemperatureMax?.let(::add)
                    step.next6Hours?.airTemperatureMin?.let(::add)
                }
            }
        }
        val precipitation = daySteps.mapNotNull { step ->
            if (step.isHourly) step.next1Hours?.precipitationAmount else step.next6Hours?.precipitationAmount
        }
        val noon = date.atTime(LocalTime.NOON).atZone(zone).toInstant()
        val symbolStep = daySteps
            .filter { it.next6Hours?.symbol != null }
            .minByOrNull { abs(Duration.between(it.time, noon).toMinutes()) }
        val symbol = symbolStep?.next6Hours?.symbol
            ?: daySteps.firstNotNullOfOrNull { it.next12Hours?.symbol }
            ?: daySteps.firstNotNullOfOrNull { it.next1Hours?.symbol }
        return DaySummary(
            date = date,
            high = temperatures.maxOrNull(),
            low = temperatures.minOrNull(),
            symbol = symbol,
            precipitation = if (precipitation.isEmpty()) null else precipitation.sum(),
        )
    }

    private fun stepEnd(step: ForecastStep): Instant =
        step.time.plus(if (step.isHourly) Duration.ofHours(1) else Duration.ofHours(6))

    private val MAX_STEP_AGE: Duration = Duration.ofMinutes(90)
}
