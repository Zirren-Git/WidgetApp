package com.zirren.tessera.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.zirren.tessera.R
import com.zirren.tessera.core.model.Age
import com.zirren.tessera.core.weather.Freshness
import com.zirren.tessera.graph
import com.zirren.tessera.widget.AgendaWidgetProvider
import com.zirren.tessera.widget.TodayWidgetProvider
import com.zirren.tessera.widget.WidgetUpdater
import java.time.Instant

/** One-tap fixes the health screen can offer. */
enum class Fix { SET_PLACE, REFRESH_WEATHER, ALLOW_LOCATION, CONNECT_CALENDAR, APP_SETTINGS, ADD_WIDGET }

/** [ok] = false counts as "needs attention". Optional things (like the calendar) stay ok but may offer a fix. */
data class Check(val ok: Boolean, val title: String, val detail: String, val fix: Fix? = null)

/**
 * "Why isn't my widget updating?" answered in plain language: KWGT's most common support
 * question, turned into a screen that checks everything and offers the fix.
 */
object HealthChecks {

    fun run(context: Context, now: Instant = Instant.now()): List<Check> {
        val graph = context.graph
        val settings = graph.settings
        val checks = mutableListOf<Check>()

        // Clocks need no checking; saying so is the point (KWGT's clocks freeze when its service is killed).
        checks += Check(true, context.getString(R.string.health_clocks_title), context.getString(R.string.health_clocks_detail))

        val placed = WidgetUpdater.count(context, TodayWidgetProvider::class.java) + WidgetUpdater.count(context, AgendaWidgetProvider::class.java)
        checks += if (placed > 0) {
            Check(true, context.getString(R.string.health_widgets_title), context.resources.getQuantityString(R.plurals.home_placed, placed, placed))
        } else {
            Check(false, context.getString(R.string.health_widgets_title), context.getString(R.string.health_widgets_none), Fix.ADD_WIDGET)
        }

        val place = settings.activePlace()
        val weatherTitle = context.getString(R.string.health_weather_title)
        if (place == null) {
            checks += Check(false, weatherTitle, context.getString(R.string.health_weather_no_place), Fix.SET_PLACE)
        } else {
            val ages = Texts.agePhrases(context)
            val fetchedAt = settings.refreshState.lastSuccessAt?.takeIf { settings.refreshState.pointKey == place.point.rounded(4).key() }
            val error = settings.lastWeatherError?.let { " " + context.getString(R.string.health_last_error, it) }.orEmpty()
            checks += when {
                fetchedAt == null ->
                    Check(false, weatherTitle, context.getString(R.string.health_weather_never, place.label) + error, Fix.REFRESH_WEATHER)
                Freshness.of(fetchedAt, now) == Freshness.FRESH ->
                    Check(true, weatherTitle, context.getString(R.string.health_weather_fresh, place.label, ages.format(Age.between(fetchedAt, now))))
                else ->
                    Check(false, weatherTitle, context.getString(R.string.health_weather_old, ages.format(Age.between(fetchedAt, now))) + error, Fix.REFRESH_WEATHER)
            }
            if (settings.locationMode == Settings.LocationMode.DEVICE && !graph.location.hasPermission()) {
                checks += Check(false, context.getString(R.string.health_location_title), context.getString(R.string.health_location_detail), Fix.ALLOW_LOCATION)
            }
        }

        checks += if (graph.calendar.hasPermission()) {
            Check(true, context.getString(R.string.health_calendar_title), context.getString(R.string.health_calendar_ok))
        } else {
            Check(true, context.getString(R.string.health_calendar_title), context.getString(R.string.health_calendar_off), Fix.CONNECT_CALENDAR)
        }

        val restricted = Build.VERSION.SDK_INT >= 28 &&
            context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
        checks += if (restricted) {
            Check(false, context.getString(R.string.health_background_title), context.getString(R.string.health_background_restricted), Fix.APP_SETTINGS)
        } else {
            Check(true, context.getString(R.string.health_background_title), context.getString(R.string.health_background_ok))
        }

        val exempt = context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
        checks += Check(
            true,
            context.getString(R.string.health_battery_title),
            context.getString(if (exempt) R.string.health_battery_exempt else R.string.health_battery_normal),
        )
        return checks
    }
}
