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
    }
}
