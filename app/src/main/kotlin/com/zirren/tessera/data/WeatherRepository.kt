package com.zirren.tessera.data

import com.zirren.tessera.core.model.Place
import com.zirren.tessera.core.weather.CacheValidator
import com.zirren.tessera.core.weather.FetchOutcome
import com.zirren.tessera.core.weather.Freshness
import com.zirren.tessera.core.weather.MetNorwayClient
import com.zirren.tessera.core.weather.RefreshDecision
import com.zirren.tessera.core.weather.RefreshPolicy
import com.zirren.tessera.core.weather.RefreshState
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.core.weather.WeatherReport
import com.zirren.tessera.core.weather.WeatherReporter
import java.time.Instant
import java.time.ZoneId

/** Cached weather, ready to draw. */
data class WeatherSnapshot(
    val place: Place,
    val report: WeatherReport,
    val fetchedAt: Instant,
    val freshness: Freshness,
)

/**
 * Fetches weather politely (see [RefreshPolicy]) and serves it from cache.
 * Blocking: call [refresh] from a background thread.
 */
class WeatherRepository(
    private val settings: Settings,
    private val store: WeatherStore,
    private val client: MetNorwayClient,
    private val policy: RefreshPolicy = RefreshPolicy(),
) {
    sealed interface Result {
        data object Updated : Result
        data class UpToDate(val until: Instant) : Result
        data class Waiting(val until: Instant, val reason: String) : Result
        data object NoPlace : Result
        data class Failed(val message: String) : Result
    }

    @Synchronized
    fun refresh(trigger: RefreshTrigger, now: Instant = Instant.now()): Result {
        val place = settings.activePlace() ?: return Result.NoPlace
        val point = place.point.rounded(4)
        val key = point.key()
        val saved = settings.refreshState
        val hasCacheForPoint = saved.pointKey == key && store.hasData()
        // If the cache file is gone, past success means nothing: fetch again.
        val state = if (hasCacheForPoint || saved.pointKey != key) saved else saved.copy(lastSuccessAt = null, expiresAt = null)

        when (val decision = policy.decide(state, key, trigger, now)) {
            is RefreshDecision.Wait ->
                return if (hasCacheForPoint) Result.UpToDate(decision.until) else Result.Waiting(decision.until, decision.reason)
            RefreshDecision.Fetch -> Unit
        }

        val validator = if (hasCacheForPoint) settings.lastModified?.let { CacheValidator(it, state.expiresAt ?: now) } else null
        // A new place starts with a clean slate; failures for the old one don't carry over.
        val base = if (state.pointKey == key) state else RefreshState(pointKey = key)
        settings.refreshState = base.copy(lastAttemptAt = now)

        return when (val outcome = client.fetch(point, validator, now)) {
            is FetchOutcome.Updated -> {
                store.save(outcome.body)
                settings.refreshState = RefreshState(lastSuccessAt = now, expiresAt = outcome.validator.expires, lastAttemptAt = now, pointKey = key)
                settings.lastModified = outcome.validator.lastModified
                settings.lastWeatherError = if (outcome.deprecated) DEPRECATED_NOTE else null
                Result.Updated
            }
            is FetchOutcome.NotModified -> {
                settings.refreshState = RefreshState(lastSuccessAt = now, expiresAt = outcome.validator.expires, lastAttemptAt = now, pointKey = key)
                settings.lastModified = outcome.validator.lastModified
                settings.lastWeatherError = null
                Result.UpToDate(outcome.validator.expires)
            }
            is FetchOutcome.Throttled -> fail(base, now, "The weather service asked Tessera to slow down", outcome.retryAt)
            is FetchOutcome.Failed -> fail(base, now, outcome.message)
            is FetchOutcome.NetworkError -> fail(base, now, "No connection (${outcome.message})")
        }
    }

    private fun fail(base: RefreshState, now: Instant, message: String, throttledUntil: Instant? = null): Result {
        settings.refreshState = base.copy(
            lastAttemptAt = now,
            consecutiveFailures = base.consecutiveFailures + 1,
            throttledUntil = throttledUntil,
        )
        settings.lastWeatherError = message
        return Result.Failed(message)
    }

    /** What to draw right now, or null if there is nothing trustworthy for the current place. */
    fun snapshot(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): WeatherSnapshot? {
        val place = settings.activePlace() ?: return null
        val state = settings.refreshState
        if (state.pointKey != place.point.rounded(4).key()) return null
        val fetchedAt = state.lastSuccessAt ?: return null
        val freshness = Freshness.of(fetchedAt, now)
        if (freshness == Freshness.EXPIRED) return null
        val forecast = store.load() ?: return null
        return WeatherSnapshot(place, WeatherReporter.report(forecast, now, zone), fetchedAt, freshness)
    }

    private companion object {
        const val DEPRECATED_NOTE = "MET Norway reports this API version as deprecated; an app update will be needed"
    }
}
