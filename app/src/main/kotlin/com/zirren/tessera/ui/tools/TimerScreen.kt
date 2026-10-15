package com.zirren.tessera.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.tools.StopwatchState
import com.zirren.tessera.core.tools.TimerState
import com.zirren.tessera.ui.Body
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Hairline
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.PrimaryButton
import com.zirren.tessera.ui.QuietButton
import com.zirren.tessera.ui.Section
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import com.zirren.tessera.ui.theme.PlexMono
import kotlinx.coroutines.delay

private enum class ToolTab { TIMER, STOPWATCH }

@Composable
fun TimerScreen(navigator: Navigator) {
    var currentTab by remember { mutableStateOf(ToolTab.TIMER) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = insets.calculateTopPadding() + 8.dp,
                bottom = insets.calculateBottomPadding() + 16.dp,
            ),
    ) {
        ToolTopBar(Tool.TIMER, onBack = { navigator.pop() })

        Choices(
            options = listOf(
                ToolTab.TIMER to stringResource(R.string.timer_tab_timer),
                ToolTab.STOPWATCH to stringResource(R.string.timer_tab_stopwatch),
            ),
            selected = currentTab,
            onSelect = { currentTab = it },
        )

        Spacer(Modifier.height(16.dp))

        when (currentTab) {
            ToolTab.TIMER -> TimerContent()
            ToolTab.STOPWATCH -> StopwatchContent()
        }
    }
}

@Composable
private fun TimerContent() {
    val haptic = LocalHapticFeedback.current
    var timer by remember { mutableStateOf(TimerState(totalSeconds = 300L)) }

    LaunchedEffect(timer.isRunning) {
        while (timer.isRunning) {
            delay(50)
            timer = timer.tick(System.currentTimeMillis())
        }
    }

    val presets = listOf(
        60L to stringResource(R.string.timer_preset_1m),
        180L to stringResource(R.string.timer_preset_3m),
        300L to stringResource(R.string.timer_preset_5m),
        600L to stringResource(R.string.timer_preset_10m),
        900L to stringResource(R.string.timer_preset_15m),
        1500L to stringResource(R.string.timer_preset_25m),
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Large countdown ring with time
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.outlineVariant,
                strokeWidth = 6.dp,
            )
            CircularProgressIndicator(
                progress = { timer.progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timer.formatted(),
                    fontFamily = Fraunces,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (timer.isFinished) {
                    Text(
                        text = stringResource(R.string.timer_finished),
                        fontFamily = InstrumentSans,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Main controls
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!timer.isRunning) {
                PrimaryButton(
                    text = if (timer.remainingMillis < timer.totalSeconds * 1000L && !timer.isFinished) {
                        stringResource(R.string.timer_resume)
                    } else {
                        stringResource(R.string.timer_start)
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        timer = timer.start(System.currentTimeMillis())
                    },
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.timer_pause),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        timer = timer.pause(System.currentTimeMillis())
                    },
                )
            }

            QuietButton(
                text = stringResource(R.string.timer_reset),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    timer = timer.reset()
                },
            )

            QuietButton(
                text = stringResource(R.string.timer_add_minute),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    timer = timer.addSeconds(60L, System.currentTimeMillis())
                },
            )
        }

        Spacer(Modifier.height(32.dp))

        // Preset chips
        Label(stringResource(R.string.timer_presets), Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Choices(
            options = presets,
            selected = timer.totalSeconds,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                timer = timer.setDuration(it)
            },
        )
    }
}

@Composable
private fun StopwatchContent() {
    val haptic = LocalHapticFeedback.current
    var stopwatch by remember { mutableStateOf(StopwatchState()) }
    var currentElapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(stopwatch.isRunning) {
        while (stopwatch.isRunning) {
            delay(30)
            currentElapsed = stopwatch.currentElapsed(System.currentTimeMillis())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))

        // Elapsed time display
        Panel {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = StopwatchState.format(if (stopwatch.isRunning) currentElapsed else stopwatch.accumulatedMillis),
                    fontFamily = PlexMono,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!stopwatch.isRunning) {
                PrimaryButton(
                    text = if (stopwatch.accumulatedMillis > 0L) stringResource(R.string.timer_resume) else stringResource(R.string.timer_start),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        stopwatch = stopwatch.start(System.currentTimeMillis())
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.timer_pause),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        stopwatch = stopwatch.pause(System.currentTimeMillis())
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            QuietButton(
                text = stringResource(R.string.stopwatch_lap),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    stopwatch = stopwatch.lap(System.currentTimeMillis())
                },
                enabled = stopwatch.isRunning,
                modifier = Modifier.weight(1f),
            )

            QuietButton(
                text = stringResource(R.string.timer_reset),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    stopwatch = stopwatch.reset()
                    currentElapsed = 0L
                },
                enabled = !stopwatch.isRunning && stopwatch.accumulatedMillis > 0L,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // Laps list
        if (stopwatch.laps.isEmpty()) {
            Body(stringResource(R.string.stopwatch_no_laps), Modifier.padding(top = 12.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.stopwatch_lap_header).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(60.dp),
                )
                Text(
                    text = stringResource(R.string.stopwatch_split_header).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
                Text(
                    text = stringResource(R.string.stopwatch_total_header).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
            Hairline()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(stopwatch.laps, key = { it.index }) { lap ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${lap.index}",
                            fontFamily = PlexMono,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            text = StopwatchState.format(lap.lapMillis),
                            fontFamily = PlexMono,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                        )
                        Text(
                            text = StopwatchState.format(lap.totalMillis),
                            fontFamily = PlexMono,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                        )
                    }
                    Hairline()
                }
            }
        }
    }
}
