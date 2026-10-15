package com.zirren.tessera.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.model.MeasureUnit
import com.zirren.tessera.core.model.NumberStyle
import com.zirren.tessera.core.model.NumberText
import com.zirren.tessera.core.model.UnitCategory
import com.zirren.tessera.core.model.UnitConverter
import com.zirren.tessera.data.Texts
import com.zirren.tessera.graph
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import java.math.BigDecimal

private enum class PickTarget { FROM, TO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(navigator: Navigator) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val locale = remember { Texts.forContext(context, context.graph.settings).dateTime.locale }
    val numberStyle = remember(locale) { NumberStyle.of(locale) }
    val countryCode = remember(context) { Texts.regionOf(context) }

    var category by remember { mutableStateOf(UnitCategory.LENGTH) }
    var defaultPair = remember(category, countryCode) { UnitConverter.defaultPair(category, countryCode) }
    var fromUnit by remember(category) { mutableStateOf(defaultPair.first) }
    var toUnit by remember(category) { mutableStateOf(defaultPair.second) }

    var input by remember { mutableStateOf("1") }
    var pickingTarget by remember { mutableStateOf<PickTarget?>(null) }

    fun onCategoryChange(newCat: UnitCategory) {
        if (category == newCat) return
        category = newCat
        val pair = UnitConverter.defaultPair(newCat, countryCode)
        fromUnit = pair.first
        toUnit = pair.second
    }

    fun swap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val temp = fromUnit
        fromUnit = toUnit
        toUnit = temp
    }

    fun pressDigit(digit: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        input = when {
            input == "0" -> digit.toString()
            input == "-0" -> "-$digit"
            input.count { it.isDigit() } >= 12 -> input
            else -> input + digit
        }
    }

    fun pressDecimal() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if ('.' !in input) input = "$input."
    }

    fun pressBackspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val shorter = input.dropLast(1)
        input = if (shorter.isEmpty() || shorter == "-") "0" else shorter
    }

    fun pressClear() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        input = "0"
    }

    fun toggleSign() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        input = if (input.startsWith('-')) input.substring(1) else "-$input"
    }

    val copiedToast = stringResource(R.string.calc_copied)
    fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.tool_converter), text))
        Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
    }

    // Convert value
    val inputValue = try { BigDecimal(input) } catch (e: Exception) { BigDecimal.ZERO }
    val convertedValue = UnitConverter.convert(inputValue, fromUnit, toUnit)
    val formattedResult = convertedValue?.let {
        NumberText.format(it, numberStyle, significant = 10, maxFraction = 8)
    } ?: "\u2013"

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
        ToolTopBar(Tool.CONVERTER, onBack = { navigator.pop() })

        // Category tabs
        Choices(
            options = UnitCategory.entries.map { it to stringResource(UnitNames.categoryTitle(it)) },
            selected = category,
            onSelect = { onCategoryChange(it) },
        )

        Spacer(Modifier.height(16.dp))

        // Conversion display panels
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            // From Panel
            UnitPanel(
                label = stringResource(R.string.converter_from),
                unit = fromUnit,
                displayValue = NumberText.group(input, numberStyle),
                isInput = true,
                onPickUnit = { pickingTarget = PickTarget.FROM },
                onCopy = { copyText(input) },
            )

            // Swap button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { swap() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_swap_vertical),
                        contentDescription = stringResource(R.string.converter_swap),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // To Panel
            UnitPanel(
                label = stringResource(R.string.converter_to),
                unit = toUnit,
                displayValue = formattedResult,
                isInput = false,
                onPickUnit = { pickingTarget = PickTarget.TO },
                onCopy = { copyText(formattedResult) },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Keypad (4 columns)
        val keyShape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row 1: 7, 8, 9, C
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("7", { pressDigit(7) }, Modifier.weight(1f), keyShape)
                KeyButton("8", { pressDigit(8) }, Modifier.weight(1f), keyShape)
                KeyButton("9", { pressDigit(9) }, Modifier.weight(1f), keyShape)
                KeyAction("C", { pressClear() }, Modifier.weight(1f), keyShape, stringResource(R.string.calc_cd_clear))
            }
            // Row 2: 4, 5, 6, Backspace
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("4", { pressDigit(4) }, Modifier.weight(1f), keyShape)
                KeyButton("5", { pressDigit(5) }, Modifier.weight(1f), keyShape)
                KeyButton("6", { pressDigit(6) }, Modifier.weight(1f), keyShape)
                KeyIcon(R.drawable.ic_backspace, { pressBackspace() }, Modifier.weight(1f), keyShape, stringResource(R.string.calc_cd_backspace))
            }
            // Row 3: 1, 2, 3, ±
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("1", { pressDigit(1) }, Modifier.weight(1f), keyShape)
                KeyButton("2", { pressDigit(2) }, Modifier.weight(1f), keyShape)
                KeyButton("3", { pressDigit(3) }, Modifier.weight(1f), keyShape)
                KeyAction("\u00B1", { toggleSign() }, Modifier.weight(1f), keyShape, stringResource(R.string.calc_cd_sign))
            }
            // Row 4: 0 (weight 2f), . (weight 1f), Swap (weight 1f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("0", { pressDigit(0) }, Modifier.weight(2f), keyShape)
                KeyButton(numberStyle.decimal.toString(), { pressDecimal() }, Modifier.weight(1f), keyShape, stringResource(R.string.calc_cd_decimal))
                KeyIcon(R.drawable.ic_swap_vertical, { swap() }, Modifier.weight(1f), keyShape, stringResource(R.string.converter_swap))
            }
        }
    }

    // Unit Picker Bottom Sheet
    if (pickingTarget != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { pickingTarget = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.converter_select_unit),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(category.units) { unit ->
                        val isSelected = (pickingTarget == PickTarget.FROM && unit == fromUnit) ||
                            (pickingTarget == PickTarget.TO && unit == toUnit)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    if (pickingTarget == PickTarget.FROM) fromUnit = unit else toUnit = unit
                                    pickingTarget = null
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(UnitNames.unitName(unit)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = unit.symbol,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitPanel(
    label: String,
    unit: MeasureUnit,
    displayValue: String,
    isInput: Boolean,
    onPickUnit: () -> Unit,
    onCopy: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val border = if (isInput) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
    val bg = if (isInput) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, border, shape)
            .background(bg)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            // Clickable unit picker chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickUnit, role = Role.Button)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(UnitNames.unitName(unit))} (${unit.symbol})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val fontSize = when {
            displayValue.length <= 8 -> 36.sp
            displayValue.length <= 12 -> 28.sp
            else -> 22.sp
        }

        Text(
            text = displayValue,
            fontFamily = Fraunces,
            fontSize = fontSize,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy),
        )
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    contentDesc: String? = null,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick, role = Role.Button)
            .then(if (contentDesc != null) Modifier.semantics { contentDescription = contentDesc } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = InstrumentSans,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun KeyAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    contentDesc: String,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = InstrumentSans,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyIcon(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    contentDesc: String,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
