package com.zirren.tessera

import android.app.Application
import android.content.pm.PackageManager
import com.zirren.tessera.core.weather.HttpTransport
import com.zirren.tessera.core.weather.MetNorwayClient
import com.zirren.tessera.core.weather.PlaceSearch
import com.zirren.tessera.core.weather.UrlConnectionTransport
import com.zirren.tessera.data.CalendarRepository
import com.zirren.tessera.data.DeviceLocation
import com.zirren.tessera.data.Settings
import com.zirren.tessera.data.ToolStore
import com.zirren.tessera.data.WeatherRepository
import com.zirren.tessera.data.WeatherStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AppGraph(app: Application) {
    val settings = Settings(app)
    val tools = ToolStore(app)

    /** Background work for receivers, jobs and widget rendering. Two threads is plenty. */
    val io: ExecutorService = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "tessera-io").apply { isDaemon = true }
    }

    /** MET Norway's terms ask for an identifying User-Agent with a way to reach us. */
    val userAgent: String = "Tessera/${versionName(app)} github.com/Zirren-Git/WidgetApp"

    private val transport: HttpTransport = UrlConnectionTransport()

    val weatherStore = WeatherStore(app)
    val weather = WeatherRepository(settings, weatherStore, MetNorwayClient(transport, userAgent))
    val placeSearch = PlaceSearch(transport, userAgent)
    val calendar = CalendarRepository(app)
    val location = DeviceLocation(app)

    @Suppress("DEPRECATION")
    private fun versionName(app: Application): String = try {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "dev"
    } catch (e: PackageManager.NameNotFoundException) {
        "dev"
    }
}
