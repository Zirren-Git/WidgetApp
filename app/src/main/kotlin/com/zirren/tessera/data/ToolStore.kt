package com.zirren.tessera.data

import android.content.Context
import android.content.SharedPreferences

/**
 * What the tools remember between visits: the calculator's display, the counter, running timers.
 *
 * Kept apart from [Settings] on purpose. These values change with every tap, and Settings'
 * listeners would otherwise refresh whichever screen is open each time. Keys follow the same
 * `area.key` naming.
 */
class ToolStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tessera.tools", Context.MODE_PRIVATE)

    /** The calculator as CalculatorState.encode() wrote it, so it reopens where the user left it. */
    var calculator: String?
        get() = prefs.getString(KEY_CALCULATOR, null)
        set(value) = prefs.edit().putString(KEY_CALCULATOR, value).apply()

    var counter: String?
        get() = prefs.getString(KEY_COUNTER, null)
        set(value) = prefs.edit().putString(KEY_COUNTER, value).apply()

    private companion object {
        const val KEY_CALCULATOR = "tools.calculator.state"
        const val KEY_COUNTER = "tools.counter.state"
    }
}
