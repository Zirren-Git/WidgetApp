package com.zirren.tessera.core.tools

/**
 * State machine for the in-app timer.
 * Keeps timestamps so countdowns stay exact even if the app pauses or the device clock updates.
 */
data class TimerState(
    val totalSeconds: Long = 300L, // 5 min default
    val remainingMillis: Long = totalSeconds * 1000L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val targetEpochMillis: Long? = null,
) {
    val progress: Float
        get() = if (totalSeconds <= 0) 1f else ((totalSeconds * 1000L - remainingMillis).toFloat() / (totalSeconds * 1000L)).coerceIn(0f, 1f)

    /** Formatted time for display: "MM:SS" or "H:MM:SS". */
    fun formatted(): String {
        val secondsTotal = (remainingMillis + 999L) / 1000L // round up so 0.1s shows 1s
        val hours = secondsTotal / 3600L
        val minutes = (secondsTotal % 3600L) / 60L
        val seconds = secondsTotal % 60L
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun start(nowEpochMillis: Long): TimerState {
        if (remainingMillis <= 0L) return this
        return copy(
            isRunning = true,
            isFinished = false,
            targetEpochMillis = nowEpochMillis + remainingMillis,
        )
    }

    fun pause(nowEpochMillis: Long): TimerState {
        if (!isRunning) return this
        val left = (targetEpochMillis ?: (nowEpochMillis + remainingMillis)) - nowEpochMillis
        val clamped = left.coerceAtLeast(0L)
        return copy(
            isRunning = false,
            remainingMillis = clamped,
            isFinished = clamped == 0L,
            targetEpochMillis = null,
        )
    }

    fun tick(nowEpochMillis: Long): TimerState {
        if (!isRunning || targetEpochMillis == null) return this
        val left = targetEpochMillis - nowEpochMillis
        return if (left <= 0L) {
            copy(
                remainingMillis = 0L,
                isRunning = false,
                isFinished = true,
                targetEpochMillis = null,
            )
        } else {
            copy(remainingMillis = left)
        }
    }

    fun reset(): TimerState = copy(
        remainingMillis = totalSeconds * 1000L,
        isRunning = false,
        isFinished = false,
        targetEpochMillis = null,
    )

    fun setDuration(seconds: Long): TimerState {
        val clamped = seconds.coerceIn(1L, 359_999L) // up to 99h 59m 59s
        return TimerState(
            totalSeconds = clamped,
            remainingMillis = clamped * 1000L,
            isRunning = false,
            isFinished = false,
            targetEpochMillis = null,
        )
    }

    fun addSeconds(seconds: Long, nowEpochMillis: Long): TimerState {
        val newTotal = (totalSeconds + seconds).coerceIn(1L, 359_999L)
        val newRemaining = (remainingMillis + seconds * 1000L).coerceIn(0L, newTotal * 1000L)
        val newTarget = if (isRunning) nowEpochMillis + newRemaining else null
        return copy(
            totalSeconds = newTotal,
            remainingMillis = newRemaining,
            targetEpochMillis = newTarget,
            isFinished = false,
        )
    }
}

data class Lap(
    val index: Int,
    val lapMillis: Long,
    val totalMillis: Long,
)

/**
 * State machine for the in-app stopwatch with lap tracking.
 */
data class StopwatchState(
    val isRunning: Boolean = false,
    val accumulatedMillis: Long = 0L,
    val startEpochMillis: Long? = null,
    val laps: List<Lap> = emptyList(),
) {
    fun currentElapsed(nowEpochMillis: Long): Long =
        if (isRunning && startEpochMillis != null) {
            accumulatedMillis + (nowEpochMillis - startEpochMillis).coerceAtLeast(0L)
        } else {
            accumulatedMillis
        }

    fun start(nowEpochMillis: Long): StopwatchState =
        if (isRunning) this else copy(isRunning = true, startEpochMillis = nowEpochMillis)

    fun pause(nowEpochMillis: Long): StopwatchState {
        if (!isRunning || startEpochMillis == null) return this
        val elapsed = accumulatedMillis + (nowEpochMillis - startEpochMillis).coerceAtLeast(0L)
        return copy(
            isRunning = false,
            accumulatedMillis = elapsed,
            startEpochMillis = null,
        )
    }

    fun lap(nowEpochMillis: Long): StopwatchState {
        if (!isRunning) return this
        val total = currentElapsed(nowEpochMillis)
        val lastLapTotal = laps.firstOrNull()?.totalMillis ?: 0L
        val lapDuration = (total - lastLapTotal).coerceAtLeast(0L)
        val newLap = Lap(index = laps.size + 1, lapMillis = lapDuration, totalMillis = total)
        return copy(laps = listOf(newLap) + laps)
    }

    fun reset(): StopwatchState = StopwatchState()

    companion object {
        fun format(millis: Long): String {
            val totalSeconds = millis / 1000L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            val hundredths = (millis % 1000L) / 10L
            val hours = totalSeconds / 3600L
            return if (hours > 0) {
                String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
            } else {
                String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
            }
        }
    }
}
