package com.zirren.tessera.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.zirren.tessera.core.model.NumberStyle
import com.zirren.tessera.core.tools.CalcError
import com.zirren.tessera.core.tools.CalcKey
import com.zirren.tessera.core.tools.CalcOp
import com.zirren.tessera.core.tools.CalculatorState
import com.zirren.tessera.data.Texts
import com.zirren.tessera.graph
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import com.zirren.tessera.ui.theme.PlexMono

@Composable
fun CalculatorScreen(navigator: Navigator) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val store = context.graph.tools
    val locale = remember { Texts.forContext(context, context.graph.settings).dateTime.locale }
    val numberStyle = remember(locale) { NumberStyle.of(locale) }

    var state by remember { mutableStateOf(CalculatorState.decode(store.calculator)) }

    fun press(key: CalcKey) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = state.press(key)
        state = next
        store.calculator = next.encode()
    }

    val copyCd = stringResource(R.string.calc_copy_cd)
    val copiedToast = stringResource(R.string.calc_copied)
    fun copyDisplay() {
        val textToCopy = state.copyText(numberStyle) ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.tool_calculator), textToCopy))
        Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
    }

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
        ToolTopBar(Tool.CALCULATOR, onBack = { navigator.pop() })

        // Display
        val displayText = when (state.error) {
            CalcError.DIVIDE_BY_ZERO -> stringResource(R.string.calc_error_divide_by_zero)
            CalcError.TOO_LARGE -> stringResource(R.string.calc_error_too_large)
            null -> state.display(numberStyle)
        }
        val isError = state.error != null

        val fontSize = when {
            isError -> 28.sp
            displayText.length <= 7 -> 54.sp
            displayText.length <= 10 -> 42.sp
            displayText.length <= 13 -> 32.sp
            else -> 24.sp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .combinedClickable(
                    onClick = { copyDisplay() },
                    onLongClick = { copyDisplay() },
                )
                .semantics { contentDescription = copyCd }
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (state.activeOperator != null) {
                    val opSymbol = when (state.activeOperator) {
                        CalcOp.PLUS -> "+"
                        CalcOp.MINUS -> "\u2212"
                        CalcOp.TIMES -> "\u00D7"
                        CalcOp.DIVIDE -> "\u00F7"
                        null -> ""
                    }
                    Text(
                        text = opSymbol,
                        fontFamily = PlexMono,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = displayText,
                    fontFamily = if (isError) InstrumentSans else Fraunces,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.1f).sp,
                    textAlign = TextAlign.End,
                    color = if (isError) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Keypad grid
        val activeOp = state.activeOperator
        val rows = listOf(
            listOf(
                KeySpec.Action("C", CalcKey.CLEAR, stringResource(R.string.calc_cd_clear)),
                KeySpec.Action("\u00B1", CalcKey.SIGN, stringResource(R.string.calc_cd_sign)),
                KeySpec.Action("%", CalcKey.PERCENT, stringResource(R.string.calc_cd_percent)),
                KeySpec.Op("\u00F7", CalcKey.DIVIDE, CalcOp.DIVIDE, stringResource(R.string.calc_cd_divide)),
            ),
            listOf(
                KeySpec.Digit(7, CalcKey.DIGIT_7, stringResource(R.string.calc_cd_digit, 7)),
                KeySpec.Digit(8, CalcKey.DIGIT_8, stringResource(R.string.calc_cd_digit, 8)),
                KeySpec.Digit(9, CalcKey.DIGIT_9, stringResource(R.string.calc_cd_digit, 9)),
                KeySpec.Op("\u00D7", CalcKey.TIMES, CalcOp.TIMES, stringResource(R.string.calc_cd_multiply)),
            ),
            listOf(
                KeySpec.Digit(4, CalcKey.DIGIT_4, stringResource(R.string.calc_cd_digit, 4)),
                KeySpec.Digit(5, CalcKey.DIGIT_5, stringResource(R.string.calc_cd_digit, 5)),
                KeySpec.Digit(6, CalcKey.DIGIT_6, stringResource(R.string.calc_cd_digit, 6)),
                KeySpec.Op("\u2212", CalcKey.MINUS, CalcOp.MINUS, stringResource(R.string.calc_cd_subtract)),
            ),
            listOf(
                KeySpec.Digit(1, CalcKey.DIGIT_1, stringResource(R.string.calc_cd_digit, 1)),
                KeySpec.Digit(2, CalcKey.DIGIT_2, stringResource(R.string.calc_cd_digit, 2)),
                KeySpec.Digit(3, CalcKey.DIGIT_3, stringResource(R.string.calc_cd_digit, 3)),
                KeySpec.Op("+", CalcKey.PLUS, CalcOp.PLUS, stringResource(R.string.calc_cd_add)),
            ),
            listOf(
                KeySpec.Digit(0, CalcKey.DIGIT_0, stringResource(R.string.calc_cd_digit, 0)),
                KeySpec.Action(numberStyle.decimal.toString(), CalcKey.DECIMAL, stringResource(R.string.calc_cd_decimal)),
                KeySpec.IconKey(R.drawable.ic_backspace, CalcKey.BACKSPACE, stringResource(R.string.calc_cd_backspace)),
                KeySpec.Equals("=", CalcKey.EQUALS, stringResource(R.string.calc_cd_equals)),
            ),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { spec ->
                        CalcButton(
                            spec = spec,
                            isActiveOp = (spec is KeySpec.Op && spec.op == activeOp),
                            onClick = { press(spec.key) },
                            modifier = Modifier.weight(1f).height(64.dp),
                        )
                    }
                }
            }
        }
    }
}

private sealed interface KeySpec {
    val key: CalcKey
    val description: String

    data class Digit(val value: Int, override val key: CalcKey, override val description: String) : KeySpec
    data class Action(val label: String, override val key: CalcKey, override val description: String) : KeySpec
    data class Op(val label: String, override val key: CalcKey, val op: CalcOp, override val description: String) : KeySpec
    data class Equals(val label: String, override val key: CalcKey, override val description: String) : KeySpec
    data class IconKey(val iconRes: Int, override val key: CalcKey, override val description: String) : KeySpec
}

@Composable
private fun CalcButton(
    spec: KeySpec,
    isActiveOp: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    val (bg, border, fg) = when {
        isActiveOp -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        spec is KeySpec.Equals -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        spec is KeySpec.Op -> Triple(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.primary,
        )
        spec is KeySpec.Action -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        spec is KeySpec.IconKey -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Triple( // Digit
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.onSurface,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .background(bg)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = spec.description },
        contentAlignment = Alignment.Center,
    ) {
        when (spec) {
            is KeySpec.IconKey -> {
                Icon(
                    painter = painterResource(spec.iconRes),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(22.dp),
                )
            }
            is KeySpec.Digit -> {
                Text(
                    text = spec.value.toString(),
                    fontFamily = InstrumentSans,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                )
            }
            is KeySpec.Op -> {
                Text(
                    text = spec.label,
                    fontFamily = InstrumentSans,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                )
            }
            is KeySpec.Action -> {
                Text(
                    text = spec.label,
                    fontFamily = InstrumentSans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                )
            }
            is KeySpec.Equals -> {
                Text(
                    text = spec.label,
                    fontFamily = InstrumentSans,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                )
            }
        }
    }
}
