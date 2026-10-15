package com.zirren.tessera.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RefreshPolicyTest {
    private val policy = RefreshPolicy()
    private val now = Instant.parse("2026-10-15T12:00:00Z")
    private val point = "58.2837,12.2886"

    private fun minutesAgo(m: Long) = now.minusSeconds(m * 60)
    private fun inMinutes(m: Long) = now.plusSeconds(m * 60)

    @Test fun firstEverRefreshFetches() {
        assertEquals(RefreshDecision.Fetch, policy.decide(RefreshState(), point, RefreshTrigger.SCHEDULED, now))
    }

    @Test fun neverBeforeExpiresEvenWhenTheUserAsks() {
        val state = RefreshState(lastSuccessAt = minutesAgo(10), expiresAt = inMinutes(20), lastAttemptAt = minutesAgo(10), pointKey = point)
        val decision = policy.decide(state, point, RefreshTrigger.USER, now)
        assertEquals(RefreshDecision.Wait(inMinutes(20), "Forecast is still current"), decision)
    }

    @Test fun scheduledRefreshWaitsForTheHourlyInterval() {
        val state = RefreshState(lastSuccessAt = minutesAgo(40), expiresAt = minutesAgo(5), lastAttemptAt = minutesAgo(40), pointKey = point)
        assertEquals(RefreshDecision.Wait(inMinutes(20), "Next scheduled refresh"), policy.decide(state, point, RefreshTrigger.SCHEDULED, now))
        assertEquals(RefreshDecision.Fetch, policy.decide(state, point, RefreshTrigger.APP_OPENED, now))
    }

    @Test fun newPlaceFetchesImmediately() {
        val state = RefreshState(lastSuccessAt = minutesAgo(5), expiresAt = inMinutes(25), lastAttemptAt = minutesAgo(5), pointKey = point)
        assertEquals(RefreshDecision.Fetch, policy.decide(state, "59.3293,18.0686", RefreshTrigger.LOCATION_CHANGED, now))
    }

    @Test fun backsOffAfterFailures() {
        val failing = RefreshState(lastSuccessAt = minutesAgo(200), expiresAt = minutesAgo(170), lastAttemptAt = minutesAgo(10), consecutiveFailures = 2, pointKey = point)
        assertEquals(RefreshDecision.Wait(inMinutes(5), "Retrying after an error"), policy.decide(failing, point, RefreshTrigger.SCHEDULED, now))
        val manyFailures = failing.copy(consecutiveFailures = 9, lastAttemptAt = minutesAgo(61))
        assertEquals(RefreshDecision.Fetch, policy.decide(manyFailures, point, RefreshTrigger.SCHEDULED, now))
    }

    @Test fun neverLoadedPlaceStillBacksOff() {
        val offline = RefreshState(lastAttemptAt = minutesAgo(2), consecutiveFailures = 1, pointKey = point)
        assertEquals(RefreshDecision.Wait(inMinutes(3), "Retrying after an error"), policy.decide(offline, point, RefreshTrigger.APP_OPENED, now))
    }

    @Test fun throttlingAndDebounceWin() {
        val throttled = RefreshState(throttledUntil = inMinutes(3))
        assertTrue(policy.decide(throttled, point, RefreshTrigger.USER, now) is RefreshDecision.Wait)
        val justTried = RefreshState(lastAttemptAt = now.minusSeconds(20))
        assertEquals(RefreshDecision.Wait(now.plusSeconds(40), "Just tried"), policy.decide(justTried, point, RefreshTrigger.USER, now))
    }
}

class FreshnessTest {
    private val now = Instant.parse("2026-10-15T12:00:00Z")

    @Test fun ages() {
        assertEquals(Freshness.FRESH, Freshness.of(now.minusSeconds(2 * 3600), now))
        assertEquals(Freshness.AGING, Freshness.of(now.minusSeconds(4 * 3600), now))
        assertEquals(Freshness.STALE, Freshness.of(now.minusSeconds(20 * 3600), now))
        assertEquals(Freshness.EXPIRED, Freshness.of(now.minusSeconds(50 * 3600), now))
    }
}
