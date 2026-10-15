package com.zirren.tessera.ui.tools

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirren.tessera.R
import com.zirren.tessera.core.tools.DateSpanCalculator
import com.zirren.tessera.data.Texts
import com.zirren.tessera.graph
import com.zirren.tessera.ui.Body
import com.zirren.tessera.ui.Choices
import com.zirren.tessera.ui.Hairline
import com.zirren.tessera.ui.Label
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.ui.Panel
import com.zirren.tessera.ui.theme.Fraunces
import com.zirren.tessera.ui.theme.InstrumentSans
import com.zirren.tessera.ui.theme.PlexMono
import java.time.LocalDate

@Composable
fun DateCalcScreen(navigator: Navigator) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptic = LocalHapticFeedback.current
    val dateTimeText = remember { Texts.forContext(context, context.graph.settings).dateTime }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }

    fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onPicked(LocalDate.of(year, month + 1, dayOfMonth))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth,
        ).show()
    }

    fun swap() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val temp = startDate
        startDate = endDate
        endDate = temp
    }

    val diff = remember(startDate, endDate) { DateSpanCalculator.difference(startDate, endDate) }

    val insets = WindowInsets.safeDrawing.asPaddingValues()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = insets.calculateTopPadding() + 8.dp,
            bottom = insets.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item { ToolTopBar(Tool.DATES, onBack = { navigator.pop() }) }

        // Date selection cards
        item {
            DateSelectorCard(
                label = stringResource(R.string.dates_start_label),
                dateText = dateTimeText.longDate(startDate),
                onClick = { pickDate(startDate) { startDate = it } },
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { swap() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_swap_vertical),
                        contentDescription = stringResource(R.string.dates_swap),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        item {
            DateSelectorCard(
                label = stringResource(R.string.dates_end_label),
                dateText = dateTimeText.longDate(endDate),
                onClick = { pickDate(endDate) { endDate = it } },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Primary difference card
        item {
            Panel {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    val daysCount = diff.totalDays.toInt()
                    val mainText = when {
                        diff.totalDays == 0L -> stringResource(R.string.dates_result_today)
                        diff.isPast -> resources.getQuantityString(R.plurals.dates_result_days_ago, daysCount, diff.totalDays)
                        else -> resources.getQuantityString(R.plurals.dates_result_days_left, daysCount, diff.totalDays)
                    }

                    Text(
                        text = mainText,
                        fontFamily = Fraunces,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (diff.totalDays > 0L) {
                        val weeksPart = resources.getQuantityString(R.plurals.dates_weeks, diff.weeks.toInt(), diff.weeks)
                        val daysPart = resources.getQuantityString(R.plurals.dates_days, diff.remainderDays.toInt(), diff.remainderDays)
                        Text(
                            text = "$weeksPart, $daysPart",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Breakdown section
        item { Label(stringResource(R.string.dates_breakdown), Modifier.padding(bottom = 8.dp)) }

        item {
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BreakdownRow(
                        label = resources.getQuantityString(R.plurals.dates_business_days, diff.businessDays.toInt(), diff.businessDays),
                        detail = "Monday–Friday",
                    )
                    Hairline()
                    BreakdownRow(
                        label = resources.getQuantityString(R.plurals.dates_weekend_days, diff.weekendDays.toInt(), diff.weekendDays),
                        detail = "Saturday & Sunday",
                    )
                    if (diff.years > 0 || diff.months > 0) {
                        Hairline()
                        val calendarSpan = buildString {
                            if (diff.years > 0) append("${diff.years}y ")
                            if (diff.months > 0) append("${diff.months}m ")
                            if (diff.days > 0) append("${diff.days}d")
                        }.trim()
                        BreakdownRow(label = calendarSpan, detail = "Calendar months & days")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // Quick presets
        item { Label(stringResource(R.string.dates_quick_presets), Modifier.padding(bottom = 8.dp)) }

        item {
            val presets = listOf(
                "today" to stringResource(R.string.dates_today),
                "30d" to stringResource(R.string.dates_preset_30d),
                "100d" to stringResource(R.string.dates_preset_100d),
                "month" to stringResource(R.string.dates_preset_end_month),
                "year" to stringResource(R.string.dates_preset_end_year),
            )
            Choices(
                options = presets,
                selected = "",
                onSelect = { choice ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (choice) {
                        "today" -> {
                            startDate = LocalDate.now()
                            endDate = LocalDate.now()
                        }
                        "30d" -> endDate = startDate.plusDays(30)
                        "100d" -> endDate = startDate.plusDays(100)
                        "month" -> endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())
                        "year" -> endDate = LocalDate.of(startDate.year, 12, 31)
                    }
                },
            )
        }
    }
}

@Composable
private fun DateSelectorCard(
    label: String,
    dateText: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(14.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun BreakdownRow(label: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontFamily = PlexMono,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Body(detail)
    }
}
