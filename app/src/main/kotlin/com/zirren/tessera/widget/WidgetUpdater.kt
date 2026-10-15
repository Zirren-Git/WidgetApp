package com.zirren.tessera.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.zirren.tessera.graph
import com.zirren.tessera.sync.Scheduler

/** Redraws every Tessera widget on the home screen. */
object WidgetUpdater {
    private const val TAG = "TesseraWidgets"

    fun hasWidgets(context: Context, provider: Class<*>): Boolean = ids(context, provider).isNotEmpty()

    fun count(context: Context, provider: Class<*>): Int = ids(context, provider).size

    /** Fire-and-forget redraw from the main thread (settings screens). */
    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        app.graph.io.execute { updateAll(app) }
    }

    /** From an AppWidgetProvider: keeps the broadcast alive while we work in the background. */
    fun updateFromProvider(context: Context, pending: BroadcastReceiver.PendingResult) {
        val app = context.applicationContext
        app.graph.io.execute {
            try {
                Scheduler.ensure(app)
                updateAll(app)
            } finally {
                pending.finish()
            }
        }
    }

    /** Blocking: reads the calendar and the weather cache. Call from a background thread. */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val todayIds = ids(context, TodayWidgetProvider::class.java)
        val agendaIds = ids(context, AgendaWidgetProvider::class.java)
        if (todayIds.isEmpty() && agendaIds.isEmpty()) return

        val model = WidgetModel.load(context)
        for (id in todayIds) {
            draw(id) { manager.updateAppWidget(id, TodayWidget.render(context, model, manager.getAppWidgetOptions(id))) }
        }
        for (id in agendaIds) {
            draw(id) { manager.updateAppWidget(id, AgendaWidget.render(context, model, manager.getAppWidgetOptions(id))) }
        }
        Scheduler.scheduleBoundary(context, model.nextChange())
    }

    /** One broken widget must never stop the others from updating. */
    private inline fun draw(id: Int, block: () -> Unit) {
        try {
            block()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not draw widget $id", e)
        }
    }

    private fun ids(context: Context, provider: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, provider))
}
