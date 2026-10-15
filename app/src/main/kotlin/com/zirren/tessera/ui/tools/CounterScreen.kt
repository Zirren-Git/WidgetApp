package com.zirren.tessera.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.tools.TallyCounter
import com.zirren.tessera.graph
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.QuietButton
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.PlexMono

@Composable
fun CounterScreen(navigator: Navigator) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val store = context.graph.tools

    var counter by remember { mutableStateOf(TallyCounter.decode(store.counter)) }

    fun update(next: TallyCounter) {
        counter = next
        store.counter = next.encode()
    }

    val stepPresets = listOf(1L to "+1", 5L to "+5", 10L to "+10", 50L to "+50", 100L to "+100")
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
        ToolTopBar(Tool.COUNTER, onBack = { navigator.pop() })

        // Large Counter Panel
        Panel(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${counter.count}",
                    fontFamily = Fraunces,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Step size choices
        Label(stringResource(R.string.counter_step_label))
        Spacer(Modifier.height(6.dp))
        Choices(
            options = stepPresets,
            selected = counter.step,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                update(counter.setStepSize(it))
            },
        )

        Spacer(Modifier.height(20.dp))

        // Main action buttons: Minus, Giant Plus, Reset
        val cdIncrement = stringResource(R.string.counter_action_increment)
        val cdDecrement = stringResource(R.string.counter_action_decrement)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Subtract button
            val subShape = CircleShape
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(subShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, subShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            update(counter.decrement())
                        },
                        role = Role.Button,
                    )
                    .semantics { contentDescription = cdDecrement },
                contentAlignment = Alignment.Center,
            ) {
                Text("−", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            // Giant Add button
            val addShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 80.dp)
                    .clip(addShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            update(counter.increment())
                        },
                        role = Role.Button,
                    )
                    .semantics { contentDescription = cdIncrement },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }

            // Reset button
            QuietButton(
                text = stringResource(R.string.counter_action_reset),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    update(counter.reset())
                },
                enabled = counter.count > 0L,
            )
        }
    }
}
