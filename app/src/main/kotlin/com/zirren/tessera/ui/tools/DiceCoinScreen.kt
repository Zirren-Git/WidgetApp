package com.zirren.tessera.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.tools.CoinFlipHistory
import com.zirren.tessera.core.tools.CoinSide
import com.zirren.tessera.core.tools.DiceCoinEngine
import com.zirren.tessera.core.tools.DieType
import com.zirren.tessera.core.tools.RollRecord
import com.zirren.tessera.ui.Body
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Hairline
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.PrimaryButton
import com.zirren.tessera.ui.QuietButton
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import com.zirren.tessera.ui.theme.PlexMono

private enum class PlayTab { DICE, COIN }

@Composable
fun DiceCoinScreen(navigator: Navigator) {
    var tab by remember { mutableStateOf(PlayTab.DICE) }
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
        ToolTopBar(Tool.DICE, onBack = { navigator.pop() })

        Choices(
            options = listOf(
                PlayTab.DICE to stringResource(R.string.dice_tab_dice),
                PlayTab.COIN to stringResource(R.string.dice_tab_coin),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        Spacer(Modifier.height(16.dp))

        when (tab) {
            PlayTab.DICE -> DiceContent()
            PlayTab.COIN -> CoinContent()
        }
    }
}

@Composable
private fun DiceContent() {
    val haptic = LocalHapticFeedback.current
    val engine = remember { DiceCoinEngine() }

    var selectedType by remember { mutableStateOf(DieType.D6) }
    var count by remember { mutableIntStateOf(1) }
    var currentRoll by remember { mutableStateOf<RollRecord?>(null) }
    var history by remember { mutableStateOf<List<RollRecord>>(emptyList()) }

    fun roll() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val rec = engine.rollDice(selectedType, count)
        currentRoll = rec
        history = (listOf(rec) + history).take(20)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Die type selector
        item {
            Label(stringResource(R.string.dice_type_label))
            Spacer(Modifier.height(6.dp))
            Choices(
                options = DieType.entries.map { it to it.label },
                selected = selectedType,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedType = it
                },
            )
        }

        // Count selector
        item {
            Label(stringResource(R.string.dice_count_label))
            Spacer(Modifier.height(6.dp))
            Choices(
                options = listOf(1, 2, 3, 4, 5, 6).map { it to "$it" },
                selected = count,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    count = it
                },
            )
        }

        // Roll Action
        item {
            PrimaryButton(
                text = stringResource(R.string.dice_action_roll),
                onClick = { roll() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Results Card
        item {
            Panel {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val roll = currentRoll
                    if (roll != null) {
                        Text(
                            text = "${roll.total}",
                            fontFamily = Fraunces,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            roll.results.forEach { value ->
                                DieBadge(value = value, sides = roll.dieType.sides)
                            }
                        }
                    } else {
                        Text(
                            text = "–",
                            fontFamily = Fraunces,
                            fontSize = 44.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Body("Tap Roll to throw dice", Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // History list
        if (history.size > 1) {
            item { Label(stringResource(R.string.dice_history_label)) }
            items(history.drop(1)) { rec ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${rec.count}${rec.dieType.label}",
                        fontFamily = PlexMono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = rec.results.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "= ${rec.total}",
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Hairline()
            }
        }
    }
}

@Composable
private fun DieBadge(value: Int, sides: Int) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$value",
            fontFamily = InstrumentSans,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CoinContent() {
    val haptic = LocalHapticFeedback.current
    val engine = remember { DiceCoinEngine() }
    var history by remember { mutableStateOf(CoinFlipHistory()) }

    fun flip() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val (_, next) = engine.flipCoin(history)
        history = next
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        // Large tactile coin
        val coinShape = CircleShape
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(coinShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, coinShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = { flip() }, role = Role.Button),
            contentAlignment = Alignment.Center,
        ) {
            val text = when (history.lastResult) {
                CoinSide.HEADS -> stringResource(R.string.coin_heads)
                CoinSide.TAILS -> stringResource(R.string.coin_tails)
                null -> stringResource(R.string.coin_ready)
            }
            Text(
                text = text,
                fontFamily = Fraunces,
                fontSize = if (history.lastResult != null) 32.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (history.lastResult != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = stringResource(R.string.dice_action_flip),
                onClick = { flip() },
            )
            QuietButton(
                text = stringResource(R.string.dice_action_reset),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    history = CoinFlipHistory()
                },
                enabled = history.total > 0,
            )
        }

        Spacer(Modifier.height(32.dp))

        // Statistics
        if (history.total > 0) {
            Panel {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Label("Statistics")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.coin_stats, history.headsCount, history.tailsCount, history.total),
                        fontFamily = PlexMono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
