package com.zirren.tessera.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.graph
import com.zirren.tessera.widget.WidgetUpdater

/**
 * Reboot, app update, time/zone/language changes, and the widget's own "refresh" tap.
 * All of these mean "re-check the jobs and redraw".
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        app.graph.io.execute {
            try {
                if (intent.action == ACTION_REFRESH_WEATHER) {
                    Scheduler.refreshWeatherSoon(app, RefreshTrigger.USER)
                } else {
                    // Also re-arms the calendar watcher, which doesn't survive a reboot.
                    Scheduler.ensure(app)
                }
                WidgetUpdater.updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WEATHER = "com.zirren.tessera.action.REFRESH_WEATHER"
    }
}
