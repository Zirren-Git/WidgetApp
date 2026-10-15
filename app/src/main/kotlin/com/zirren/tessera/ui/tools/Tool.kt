package com.zirren.tessera.ui.tools

import androidx.compose.runtime.Composable
import com.zirren.tessera.R
import com.zirren.tessera.ui.Navigator
import com.zirren.tessera.widget.WidgetIntents

/** Tessera's small offline tools, in the order the Tools screen lists them. */
enum class Tool(
    /** Deep link: tessera://open/<segment>, used by widget taps and home-screen shortcuts. */
    val segment: String,
    val title: Int,
    val summary: Int,
    val icon: Int,
    /** Adaptive icon for "Add to home screen" and the app icon's long-press menu. */
    val shortcutIcon: Int,
    /** Offered when the user long-presses Tessera's app icon. Launchers show about four. */
    val inAppIconMenu: Boolean,
) {
    CALCULATOR(
        WidgetIntents.SCREEN_CALCULATOR, R.string.tool_calculator, R.string.tool_calculator_summary,
        R.drawable.ic_calculator, R.drawable.shortcut_calculator, inAppIconMenu = true,
    ),
    CONVERTER(
        WidgetIntents.SCREEN_CONVERTER, R.string.tool_converter, R.string.tool_converter_summary,
        R.drawable.ic_converter, R.drawable.shortcut_converter, inAppIconMenu = true,
    ),
    TIMER(
        WidgetIntents.SCREEN_TIMER, R.string.tool_timer, R.string.tool_timer_summary,
        R.drawable.ic_timer, R.drawable.shortcut_timer, inAppIconMenu = true,
    ),
    DATES(
        WidgetIntents.SCREEN_DATES, R.string.tool_dates, R.string.tool_dates_summary,
        R.drawable.ic_dates, R.drawable.shortcut_dates, inAppIconMenu = false,
    ),
    TIP(
        WidgetIntents.SCREEN_TIP, R.string.tool_tip, R.string.tool_tip_summary,
        R.drawable.ic_tip, R.drawable.shortcut_tip, inAppIconMenu = false,
    ),
    DICE(
        WidgetIntents.SCREEN_DICE, R.string.tool_dice, R.string.tool_dice_summary,
        R.drawable.ic_dice, R.drawable.shortcut_dice, inAppIconMenu = false,
    ),
    ;

    val shortcutId: String get() = "tool.$segment"

    companion object {
        fun fromSegment(segment: String?): Tool? = entries.firstOrNull { it.segment == segment }
    }
}

/** The screen for [tool]. */
@Composable
fun ToolScreen(navigator: Navigator, tool: Tool) {
    when (tool) {
        Tool.CALCULATOR -> CalculatorScreen(navigator)
        Tool.CONVERTER -> ConverterScreen(navigator)
        Tool.TIMER -> TimerScreen(navigator)
        Tool.DATES -> DateCalcScreen(navigator)
        Tool.TIP -> TipScreen(navigator)
        Tool.DICE -> DiceCoinScreen(navigator)
    }
}
