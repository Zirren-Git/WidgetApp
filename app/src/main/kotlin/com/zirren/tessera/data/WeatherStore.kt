package com.zirren.tessera.data

import android.content.Context
import com.zirren.tessera.core.weather.Forecast
import com.zirren.tessera.core.weather.MetNorwayParser
import com.zirren.tessera.core.weather.WeatherParseException
import java.io.File
import java.io.IOException

/**
 * Keeps the last MET Norway response on disk. The raw JSON is stored, not a parsed form,
 * so parser improvements in an app update apply to data that is already cached.
 */
class WeatherStore(context: Context) {
    private val dir = File(context.filesDir, "weather")
    private val file = File(dir, "forecast.json")
    private val lock = Any()
    private var parsed: Forecast? = null

    fun hasData(): Boolean = file.isFile

    fun save(body: String) {
        synchronized(lock) {
            dir.mkdirs()
            val temp = File(dir, "forecast.json.tmp")
            temp.writeText(body, Charsets.UTF_8)
            // rename() replaces the old file in one step, so a widget never reads half a forecast.
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
            parsed = null
        }
    }

    fun load(): Forecast? {
        synchronized(lock) {
            parsed?.let { return it }
            if (!file.isFile) return null
            return try {
                MetNorwayParser.parse(file.readText(Charsets.UTF_8)).also { parsed = it }
            } catch (e: WeatherParseException) {
                null
            } catch (e: IOException) {
                null
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            file.delete()
            parsed = null
        }
    }
}
