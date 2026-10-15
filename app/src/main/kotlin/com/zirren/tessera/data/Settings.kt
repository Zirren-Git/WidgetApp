package com.zirren.tessera.data

import android.content.Context
import android.content.SharedPreferences
import com.zirren.tessera.core.calendar.AgendaSettings
import com.zirren.tessera.core.calendar.TitlePrivacy
import com.zirren.tessera.core.model.GeoPoint
import com.zirren.tessera.core.model.Place
import com.zirren.tessera.core.model.PrecipitationUnit
import com.zirren.tessera.core.model.TemperatureUnit
import com.zirren.tessera.core.model.UnitPreferences
import com.zirren.tessera.core.model.WindSpeedUnit
import com.zirren.tessera.core.weather.RefreshState
import java.time.Instant

/**
 * Everything Tessera remembers, in one small SharedPreferences file.
 * Thread-safe for reads and apply(); listeners let Compose screens refresh when anything changes.
 */
class Settings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tessera", Context.MODE_PRIVATE)

    fun addListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun removeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    // ---- Weather location -------------------------------------------------------------

    enum class LocationMode { NONE, PLACE, DEVICE }

    var locationMode: LocationMode
        get() = LocationMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: LocationMode.NONE
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()

    /** A place picked from city search. */
    var place: Place?
        get() = readPlace(PREFIX_PLACE)
        set(value) = writePlace(PREFIX_PLACE, value)

    /** Where "Use my location" last found the phone, already rounded to about 1 km. */
    var devicePlace: Place?
        get() = readPlace(PREFIX_DEVICE)
        set(value) = writePlace(PREFIX_DEVICE, value)

    fun activePlace(): Place? = when (locationMode) {
        LocationMode.PLACE -> place
        LocationMode.DEVICE -> devicePlace
        LocationMode.NONE -> null
    }

    // ---- Units (null = follow the phone's region) -------------------------------------

    var temperatureOverride: TemperatureUnit?
        get() = TemperatureUnit.entries.firstOrNull { it.name == prefs.getString(KEY_TEMPERATURE_UNIT, null) }
        set(value) = prefs.edit().putString(KEY_TEMPERATURE_UNIT, value?.name).apply()

    var windOverride: WindSpeedUnit?
        get() = WindSpeedUnit.entries.firstOrNull { it.name == prefs.getString(KEY_WIND_UNIT, null) }
        set(value) = prefs.edit().putString(KEY_WIND_UNIT, value?.name).apply()

    fun units(regionCountryCode: String?): UnitPreferences {
        val defaults = UnitPreferences.defaultsFor(regionCountryCode)
        val temperature = temperatureOverride ?: defaults.temperature
        return UnitPreferences(
            temperature = temperature,
            windSpeed = windOverride ?: defaults.windSpeed,
            precipitation = when (temperatureOverride) {
                null -> defaults.precipitation
                TemperatureUnit.FAHRENHEIT -> PrecipitationUnit.INCHES
                TemperatureUnit.CELSIUS -> PrecipitationUnit.MILLIMETERS
            },
        )
    }

    // ---- Calendar ---------------------------------------------------------------------

    /** null = every calendar the user shows in their calendar app. */
    var calendarIds: Set<Long>?
        get() = prefs.getString(KEY_CALENDAR_IDS, null)?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.toSet()
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_CALENDAR_IDS) else putString(KEY_CALENDAR_IDS, value.sorted().joinToString(","))
        }.apply()

    var includeAllDay: Boolean
        get() = prefs.getBoolean(KEY_ALL_DAY, true)
        set(value) = prefs.edit().putBoolean(KEY_ALL_DAY, value).apply()

    var hideDeclined: Boolean
        get() = prefs.getBoolean(KEY_HIDE_DECLINED, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_DECLINED, value).apply()

    var showTitles: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TITLES, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TITLES, value).apply()

    var mergeDuplicates: Boolean
        get() = prefs.getBoolean(KEY_MERGE_DUPLICATES, true)
        set(value) = prefs.edit().putBoolean(KEY_MERGE_DUPLICATES, value).apply()

    var daysAhead: Int
        get() = prefs.getInt(KEY_DAYS_AHEAD, 7).coerceIn(1, 31)
        set(value) = prefs.edit().putInt(KEY_DAYS_AHEAD, value.coerceIn(1, 31)).apply()

    /** Set once the user has seen the calendar screen, so widgets stop suggesting it. */
    var calendarPromptDismissed: Boolean
        get() = prefs.getBoolean(KEY_CALENDAR_PROMPT_DISMISSED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALENDAR_PROMPT_DISMISSED, value).apply()

    fun agendaSettings(busyLabel: String, untitledLabel: String) = AgendaSettings(
        calendarIds = calendarIds,
        includeAllDay = includeAllDay,
        hideDeclined = hideDeclined,
        privacy = if (showTitles) TitlePrivacy.SHOW_TITLES else TitlePrivacy.BUSY_ONLY,
        daysAhead = daysAhead,
        mergeDuplicates = mergeDuplicates,
        busyLabel = busyLabel,
        untitledLabel = untitledLabel,
    )

    // ---- Appearance -------------------------------------------------------------------

    var useWallpaperColors: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER_COLORS, false)
        set(value) = prefs.edit().putBoolean(KEY_WALLPAPER_COLORS, value).apply()

    // ---- Weather bookkeeping ----------------------------------------------------------

    var refreshState: RefreshState
        get() = RefreshState(
            lastSuccessAt = instant(KEY_LAST_SUCCESS),
            expiresAt = instant(KEY_EXPIRES),
            lastAttemptAt = instant(KEY_LAST_ATTEMPT),
            consecutiveFailures = prefs.getInt(KEY_FAILURES, 0),
            throttledUntil = instant(KEY_THROTTLED_UNTIL),
            pointKey = prefs.getString(KEY_POINT, null),
        )
        set(value) = prefs.edit()
            .putInstant(KEY_LAST_SUCCESS, value.lastSuccessAt)
            .putInstant(KEY_EXPIRES, value.expiresAt)
            .putInstant(KEY_LAST_ATTEMPT, value.lastAttemptAt)
            .putInt(KEY_FAILURES, value.consecutiveFailures)
            .putInstant(KEY_THROTTLED_UNTIL, value.throttledUntil)
            .putString(KEY_POINT, value.pointKey)
            .apply()

    /** MET's Last-Modified value, sent back as If-Modified-Since. */
    var lastModified: String?
        get() = prefs.getString(KEY_LAST_MODIFIED, null)
        set(value) = prefs.edit().putString(KEY_LAST_MODIFIED, value).apply()

    /** Plain-language reason the last refresh failed, for the health check. */
    var lastWeatherError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR, value).apply()

    // ---- Helpers ------------------------------------------------------------------------

    private fun instant(key: String): Instant? =
        prefs.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(Instant::ofEpochMilli)

    private fun SharedPreferences.Editor.putInstant(key: String, value: Instant?): SharedPreferences.Editor =
        if (value == null) remove(key) else putLong(key, value.toEpochMilli())

    private fun readPlace(prefix: String): Place? {
        val name = prefs.getString(prefix + "name", null) ?: return null
        if (!prefs.contains(prefix + "lat") || !prefs.contains(prefix + "lon")) return null
        val latitude = Double.fromBits(prefs.getLong(prefix + "lat", 0))
        val longitude = Double.fromBits(prefs.getLong(prefix + "lon", 0))
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        val altitude = prefs.getInt(prefix + "alt", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        return Place(
            name = name,
            point = GeoPoint(latitude, longitude, altitude),
            region = prefs.getString(prefix + "region", null),
            country = prefs.getString(prefix + "country", null),
            countryCode = prefs.getString(prefix + "countryCode", null),
            timeZoneId = prefs.getString(prefix + "zone", null),
        )
    }

    private fun writePlace(prefix: String, place: Place?) {
        val editor = prefs.edit()
        listOf("name", "lat", "lon", "alt", "region", "country", "countryCode", "zone").forEach { editor.remove(prefix + it) }
        if (place != null) {
            editor.putString(prefix + "name", place.name)
                .putLong(prefix + "lat", place.point.latitude.toRawBits())
                .putLong(prefix + "lon", place.point.longitude.toRawBits())
                .putString(prefix + "region", place.region)
                .putString(prefix + "country", place.country)
                .putString(prefix + "countryCode", place.countryCode)
                .putString(prefix + "zone", place.timeZoneId)
            place.point.altitudeMeters?.let { editor.putInt(prefix + "alt", it) }
        }
        editor.apply()
    }

    private companion object {
        const val PREFIX_PLACE = "weather.place."
        const val PREFIX_DEVICE = "weather.device."
        const val KEY_MODE = "weather.mode"
        const val KEY_TEMPERATURE_UNIT = "units.temperature"
        const val KEY_WIND_UNIT = "units.wind"
        const val KEY_CALENDAR_IDS = "calendar.ids"
        const val KEY_ALL_DAY = "calendar.allDay"
        const val KEY_HIDE_DECLINED = "calendar.hideDeclined"
        const val KEY_SHOW_TITLES = "calendar.showTitles"
        const val KEY_MERGE_DUPLICATES = "calendar.mergeDuplicates"
        const val KEY_DAYS_AHEAD = "calendar.daysAhead"
        const val KEY_CALENDAR_PROMPT_DISMISSED = "calendar.promptDismissed"
        const val KEY_WALLPAPER_COLORS = "appearance.wallpaperColors"
        const val KEY_LAST_SUCCESS = "weather.lastSuccess"
        const val KEY_EXPIRES = "weather.expires"
        const val KEY_LAST_ATTEMPT = "weather.lastAttempt"
        const val KEY_FAILURES = "weather.failures"
        const val KEY_THROTTLED_UNTIL = "weather.throttledUntil"
        const val KEY_POINT = "weather.point"
        const val KEY_LAST_MODIFIED = "weather.lastModified"
        const val KEY_LAST_ERROR = "weather.lastError"
    }
}
