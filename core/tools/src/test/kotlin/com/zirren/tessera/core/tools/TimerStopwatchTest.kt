package com.zirren.tessera.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStopwatchTest {

    // ---- Timer tests ---------------------------------------------------------------------

    @Test fun timerInitialState() {
        val timer = TimerState(totalSeconds = 60)
        assertEquals("01:00", timer.formatted())
        assertFalse(timer.isRunning)
        assertFalse(timer.isFinished)
        assertEquals(0.0, timer.progress.toDouble(), 1e-4)
    }

    @Test fun timerCountdownAndFinish() {
        var timer = TimerState(totalSeconds = 10).start(nowEpochMillis = 1000L)
        assertTrue(timer.isRunning)

        // After 4 seconds
        timer = timer.tick(nowEpochMillis = 5000L)
        assertEquals("00:06", timer.formatted())
        assertEquals(0.4, timer.progress.toDouble(), 1e-2)
        assertFalse(timer.isFinished)

        // After 10 seconds (finished)
        timer = timer.tick(nowEpochMillis = 11000L)
        assertEquals("00:00", timer.formatted())
        assertFalse(timer.isRunning)
        assertTrue(timer.isFinished)
        assertEquals(1.0, timer.progress.toDouble(), 1e-4)
    }

    @Test fun timerPauseAndResume() {
        var timer = TimerState(totalSeconds = 30).start(nowEpochMillis = 0L)
        timer = timer.pause(nowEpochMillis = 10_000L)
        assertFalse(timer.isRunning)
        assertEquals(20_000L, timer.remainingMillis)
        assertEquals("00:20", timer.formatted())

        // Later resume
        timer = timer.start(nowEpochMillis = 50_000L)
        assertTrue(timer.isRunning)
        timer = timer.tick(nowEpochMillis = 55_000L)
        assertEquals("00:15", timer.formatted())
    }

    @Test fun timerAddSeconds() {
        var timer = TimerState(totalSeconds = 60).start(nowEpochMillis = 0L)
        timer = timer.addSeconds(60, nowEpochMillis = 0L)
        assertEquals(120L, timer.totalSeconds)
        assertEquals("02:00", timer.formatted())
    }

    // ---- Stopwatch tests -----------------------------------------------------------------

    @Test fun stopwatchElapsedAndPause() {
        var sw = StopwatchState()
        assertEquals(0L, sw.currentElapsed(nowEpochMillis = 0L))

        sw = sw.start(nowEpochMillis = 1000L)
        assertTrue(sw.isRunning)
        assertEquals(2500L, sw.currentElapsed(nowEpochMillis = 3500L))

        sw = sw.pause(nowEpochMillis = 4000L)
        assertFalse(sw.isRunning)
        assertEquals(3000L, sw.currentElapsed(nowEpochMillis = 10000L))

        // Resume
        sw = sw.start(nowEpochMillis = 12000L)
        assertEquals(4500L, sw.currentElapsed(nowEpochMillis = 13500L))
    }

    @Test fun stopwatchLaps() {
        var sw = StopwatchState().start(nowEpochMillis = 0L)
        // Lap 1 at 5s
        sw = sw.lap(nowEpochMillis = 5000L)
        assertEquals(1, sw.laps.size)
        assertEquals(5000L, sw.laps[0].lapMillis)
        assertEquals(5000L, sw.laps[0].totalMillis)

        // Lap 2 at 12s (lap duration = 7s)
        sw = sw.lap(nowEpochMillis = 12000L)
        assertEquals(2, sw.laps.size)
        assertEquals(7000L, sw.laps[0].lapMillis)
        assertEquals(12000L, sw.laps[0].totalMillis)
        assertEquals(5000L, sw.laps[1].lapMillis)
    }

    @Test fun stopwatchFormat() {
        assertEquals("00:00.00", StopwatchState.format(0L))
        assertEquals("00:05.25", StopwatchState.format(5250L))
        assertEquals("01:23.45", StopwatchState.format(83450L))
        assertEquals("1:00:00.00", StopwatchState.format(3600_000L))
    }
}
