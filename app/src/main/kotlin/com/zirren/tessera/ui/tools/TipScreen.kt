package com.zirren.tessera.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.model.NumberStyle
import com.zirren.tessera.core.model.NumberText
import com.zirren.tessera.core.tools.PercentCalculator
import com.zirren.tessera.core.tools.TipCalculator
import com.zirren.tessera.data.Texts
import com.zirren.tessera.graph
import com.zirren.tessera.ui.Body
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Hairline
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.ToggleRow
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import com.zirren.tessera.ui.theme.PlexMono
import java.math.BigDecimal

private enum class TipTab { BILL, PERCENT }

@Composable
fun TipScreen(navigator: Navigator) {
    var tab by remember { mutableStateOf(TipTab.BILL) }
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
        ToolTopBar(Tool.TIP, onBack = { navigator.pop() })

        Choices(
            options = listOf(
                TipTab.BILL to stringResource(R.string.tip_tab_bill),
                TipTab.PERCENT to stringResource(R.string.tip_tab_percent),
            ),
            selected = tab,
            onSelect = { tab = it },
        )

        Spacer(Modifier.height(14.dp))

        when (tab) {
            TipTab.BILL -> BillTabContent()
            TipTab.PERCENT -> PercentTabContent()
        }
    }
}

@Composable
private fun BillTabContent() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val locale = remember { Texts.forContext(context, context.graph.settings).dateTime.locale }
    val numberStyle = remember(locale) { NumberStyle.of(locale) }

    var billInput by remember { mutableStateOf("50") }
    var tipPercent by remember { mutableIntStateOf(15) }
    var people by remember { mutableIntStateOf(2) }
    var roundUp by remember { mutableStateOf(false) }

    val billDecimal = try { BigDecimal(billInput) } catch (e: Exception) { BigDecimal.ZERO }
    val result = remember(billDecimal, tipPercent, people, roundUp) {
        TipCalculator.calculate(billDecimal, BigDecimal(tipPercent), people, roundUp)
    }

    val tipPresets = listOf(
        0 to "0%",
        10 to "10%",
        15 to "15%",
        18 to "18%",
        20 to "20%",
        25 to "25%",
    )

    fun pressDigit(d: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        billInput = when {
            billInput == "0" -> d.toString()
            billInput.length >= 8 -> billInput
            else -> billInput + d
        }
    }

    fun pressDecimal() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if ('.' !in billInput) billInput = "$billInput."
    }

    fun pressBackspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val s = billInput.dropLast(1)
        billInput = if (s.isEmpty()) "0" else s
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Result summary card
        item {
            Panel {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.tip_summary_per_person).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = NumberText.format(result.totalPerPerson, numberStyle, 10, 2),
                        fontFamily = Fraunces,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Hairline()
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Body(stringResource(R.string.tip_summary_total))
                            Text(
                                text = NumberText.format(result.totalAmount, numberStyle, 10, 2),
                                fontFamily = InstrumentSans,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Body(stringResource(R.string.tip_summary_tip))
                            Text(
                                text = NumberText.format(result.tipAmount, numberStyle, 10, 2),
                                fontFamily = InstrumentSans,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // Bill input card
        item {
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.tip_bill_amount).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = NumberText.group(billInput, numberStyle),
                    fontFamily = Fraunces,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // Tip percent choices
        item {
            Label(stringResource(R.string.tip_percent_label))
            Spacer(Modifier.height(6.dp))
            Choices(
                options = tipPresets,
                selected = tipPercent,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tipPercent = it
                },
            )
        }

        // People split stepper
        item {
            Label(stringResource(R.string.tip_split_label))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (people > 1) people--
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    enabled = people > 1,
                ) {
                    Text("−", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = if (people == 1) stringResource(R.string.tip_person_single) else stringResource(R.string.tip_people_count, people),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (people < 99) people++
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Text("+", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Round up toggle
        item {
            ToggleRow(
                title = stringResource(R.string.tip_round_up),
                detail = stringResource(R.string.tip_round_up_detail),
                checked = roundUp,
                onChange = { roundUp = it },
            )
        }

        // Compact keypad for bill
        item {
            val keyShape = RoundedCornerShape(10.dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumKey("7", { pressDigit(7) }, Modifier.weight(1f), keyShape)
                    NumKey("8", { pressDigit(8) }, Modifier.weight(1f), keyShape)
                    NumKey("9", { pressDigit(9) }, Modifier.weight(1f), keyShape)
                    NumKey("C", { billInput = "0" }, Modifier.weight(1f), keyShape, isAction = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumKey("4", { pressDigit(4) }, Modifier.weight(1f), keyShape)
                    NumKey("5", { pressDigit(5) }, Modifier.weight(1f), keyShape)
                    NumKey("6", { pressDigit(6) }, Modifier.weight(1f), keyShape)
                    NumKey("⌫", { pressBackspace() }, Modifier.weight(1f), keyShape, isAction = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumKey("1", { pressDigit(1) }, Modifier.weight(1f), keyShape)
                    NumKey("2", { pressDigit(2) }, Modifier.weight(1f), keyShape)
                    NumKey("3", { pressDigit(3) }, Modifier.weight(1f), keyShape)
                    NumKey(numberStyle.decimal.toString(), { pressDecimal() }, Modifier.weight(1f), keyShape, isAction = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumKey("0", { pressDigit(0) }, Modifier.weight(3f), keyShape)
                    NumKey("00", { pressDigit(0); pressDigit(0) }, Modifier.weight(1f), keyShape)
                }
            }
        }
    }
}

@Composable
private fun NumKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    shape: RoundedCornerShape,
    isAction: Boolean = false,
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(if (isAction) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = InstrumentSans,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isAction) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PercentTabContent() {
    val context = LocalContext.current
    val locale = remember { Texts.forContext(context, context.graph.settings).dateTime.locale }
    val numberStyle = remember(locale) { NumberStyle.of(locale) }

    var p1Percent by remember { mutableStateOf("15") }
    var p1Total by remember { mutableStateOf("200") }

    var p2Part by remember { mutableStateOf("25") }
    var p2Total by remember { mutableStateOf("200") }

    var p3From by remember { mutableStateOf("80") }
    var p3To by remember { mutableStateOf("100") }

    val res1 = try {
        val pct = BigDecimal(p1Percent)
        val tot = BigDecimal(p1Total)
        NumberText.format(PercentCalculator.percentageOf(pct, tot), numberStyle, 8, 2)
    } catch (e: Exception) { "\u2013" }

    val res2 = try {
        val part = BigDecimal(p2Part)
        val tot = BigDecimal(p2Total)
        PercentCalculator.fractionOf(part, tot)?.let {
            NumberText.format(it, numberStyle, 8, 2) + "%"
        } ?: "\u2013"
    } catch (e: Exception) { "\u2013" }

    val res3 = try {
        val from = BigDecimal(p3From)
        val to = BigDecimal(p3To)
        PercentCalculator.percentageChange(from, to)?.let {
            val sign = if (it > BigDecimal.ZERO) "+" else ""
            sign + NumberText.format(it, numberStyle, 8, 2) + "%"
        } ?: "\u2013"
    } catch (e: Exception) { "\u2013" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Calculation 1: What is X% of Y?
        item {
            PercentCard(
                heading = stringResource(R.string.percent_calc_heading_of),
                expression = "${p1Percent}% ${stringResource(R.string.percent_calc_of)} ${p1Total}",
                result = res1,
            )
        }

        // Calculation 2: X is what % of Y?
        item {
            PercentCard(
                heading = stringResource(R.string.percent_calc_heading_ratio),
                expression = "${p2Part} ${stringResource(R.string.percent_calc_is_what_percent_of)} ${p2Total}",
                result = res2,
            )
        }

        // Calculation 3: Percent change from X to Y
        item {
            PercentCard(
                heading = stringResource(R.string.percent_calc_heading_change),
                expression = "${stringResource(R.string.percent_calc_from)} ${p3From} ${stringResource(R.string.percent_calc_to)} ${p3To}",
                result = res3,
            )
        }
    }
}

@Composable
private fun PercentCard(
    heading: String,
    expression: String,
    result: String,
) {
    Panel {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = heading.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = expression,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.percent_result, result),
                    fontFamily = PlexMono,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
