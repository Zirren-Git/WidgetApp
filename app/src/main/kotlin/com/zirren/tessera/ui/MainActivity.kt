package com.zirren.tessera.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.data.Settings
import com.zirren.tessera.graph
import com.zirren.tessera.ui.theme.TesseraTheme
import com.zirren.tessera.widget.WidgetIntents
import com.zirren.tessera.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    private val navigator = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) navigator.open(screenFor(intent))
        setContent { TesseraRoot(navigator) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigator.open(screenFor(intent))
    }

    override fun onResume() {
        super.onResume()
        // Opening the app is a good moment to refresh weather (the policy still respects MET's Expires).
        val app = applicationContext
        app.graph.io.execute {
            app.graph.weather.refresh(RefreshTrigger.APP_OPENED)
            WidgetUpdater.updateAll(app)
        }
    }

    /** Widget taps open tessera://open/<screen>. */
    private fun screenFor(intent: Intent?): Screen? = when (intent?.data?.lastPathSegment) {
        WidgetIntents.SCREEN_FORECAST -> Screen.Forecast
        WidgetIntents.SCREEN_WEATHER -> Screen.Weather
        WidgetIntents.SCREEN_CALENDAR -> Screen.Calendar
        WidgetIntents.SCREEN_HOME -> Screen.Home
        else -> null
    }
}

sealed interface Screen {
    data object Home : Screen
    data object Weather : Screen
    data object PlaceSearch : Screen
    data object Calendar : Screen
    data object Forecast : Screen
    data object Health : Screen
}

/** Tiny back stack; Tessera has a handful of screens and no deep hierarchies. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack[stack.lastIndex]

    fun push(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun open(screen: Screen?) {
        if (screen == null) return
        stack.clear()
        stack.add(Screen.Home)
        if (screen != Screen.Home) stack.add(screen)
    }
}

@Composable
fun TesseraRoot(navigator: Navigator) {
    val settings = LocalContext.current.graph.settings
    val version by rememberSettingsVersion(settings)
    val useWallpaperColors = remember(version) { settings.useWallpaperColors }
    TesseraTheme(useWallpaperColors = useWallpaperColors) {
        BackHandler(enabled = navigator.stack.size > 1) { navigator.pop() }
        when (navigator.current) {
            Screen.Home -> HomeScreen(navigator, version)
            Screen.Weather -> WeatherScreen(navigator, version)
            Screen.PlaceSearch -> PlaceSearchScreen(navigator)
            Screen.Calendar -> CalendarScreen(navigator, version)
            Screen.Forecast -> ForecastScreen(navigator, version)
            Screen.Health -> HealthScreen(navigator, version)
        }
    }
}

/** Bumps whenever any setting (including weather bookkeeping) changes, so screens reload. */
@Composable
fun rememberSettingsVersion(settings: Settings): State<Int> {
    val version = remember { mutableIntStateOf(0) }
    DisposableEffect(settings) {
        // SharedPreferences holds listeners weakly; this local keeps it alive until dispose.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version.intValue++ }
        settings.addListener(listener)
        onDispose { settings.removeListener(listener) }
    }
    return version
}
