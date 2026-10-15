package com.zirren.tessera.core.weather

import java.time.Duration
import java.time.Instant

/** How trustworthy cached weather is, which decides how a widget presents it. */
enum class Freshness {
    /** Shown normally. */
    FRESH,

    /** Shown, with a quiet "Updated 5 h ago". */
    AGING,

    /** Shown dimmed, marked as offline. */
    STALE,

    /** Too old to show at all. */
    EXPIRED;

    companion object {
        val AGING_AFTER: Duration = Duration.ofHours(3)
        val STALE_AFTER: Duration = Duration.ofHours(12)
        val EXPIRED_AFTER: Duration = Duration.ofHours(48)

        fun of(fetchedAt: Instant, now: Instant): Freshness {
            val age = Duration.between(fetchedAt, now)
            return when {
                age < AGING_AFTER -> FRESH
                age < STALE_AFTER -> AGING
                age < EXPIRED_AFTER -> STALE
                else -> EXPIRED
            }
        }
    }
}

/** Everything the policy needs to know about past attempts. Persisted by the app. */
data class RefreshState(
    val lastSuccessAt: Instant? = null,
    val expiresAt: Instant? = null,
    val lastAttemptAt: Instant? = null,
    val consecutiveFailures: Int = 0,
    val throttledUntil: Instant? = null,
    /** [com.zirren.tessera.core.model.GeoPoint.key] of the point the cache belongs to. */
    val pointKey: String? = null,
)

enum class RefreshTrigger { SCHEDULED, USER, LOCATION_CHANGED, APP_OPENED }

sealed interface RefreshDecision {
    data object Fetch : RefreshDecision
    data class Wait(val until: Instant, val reason: String) : RefreshDecision
}

/**
 * When to ask MET Norway again. Polite by design: never before `Expires`, backs off
 * after failures, and a manual "refresh" on fresh data just re-renders from cache.
 */
class RefreshPolicy(
    private val interval: Duration = Duration.ofMinutes(60),
    private val debounce: Duration = Duration.ofMinutes(1),
    private val backoff: List<Duration> = listOf(5L, 15L, 30L, 60L).map(Duration::ofMinutes),
) {
    fun decide(state: RefreshState, pointKey: String, trigger: RefreshTrigger, now: Instant): RefreshDecision {
        state.throttledUntil?.let { if (now.isBefore(it)) return RefreshDecision.Wait(it, "MET Norway asked us to slow down") }

        state.lastAttemptAt?.let { last ->
            val earliest = last.plus(debounce)
            if (now.isBefore(earliest)) return RefreshDecision.Wait(earliest, "Just tried")
        }

        // A different place is a different forecast: failures and expiry of the old one don't apply.
        if (state.pointKey != pointKey) return RefreshDecision.Fetch

        if (state.consecutiveFailures > 0 && state.lastAttemptAt != null) {
            val wait = backoff[(state.consecutiveFailures - 1).coerceIn(0, backoff.lastIndex)]
            val retryAt = state.lastAttemptAt.plus(wait)
            if (now.isBefore(retryAt)) return RefreshDecision.Wait(retryAt, "Retrying after an error")
            return RefreshDecision.Fetch
        }

        if (state.lastSuccessAt == null) return RefreshDecision.Fetch

        val expires = state.expiresAt ?: state.lastSuccessAt.plus(MetNorwayClient.DEFAULT_LIFETIME)
        if (now.isBefore(expires)) return RefreshDecision.Wait(expires, "Forecast is still current")

        if (trigger == RefreshTrigger.SCHEDULED) {
            val due = state.lastSuccessAt.plus(interval)
            if (now.isBefore(due)) return RefreshDecision.Wait(due, "Next scheduled refresh")
        }
        return RefreshDecision.Fetch
    }
}
