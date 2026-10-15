package com.zirren.tessera.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.zirren.tessera.R
import com.zirren.tessera.core.calendar.AgendaItem
import com.zirren.tessera.graph
import com.zirren.tessera.sync.Scheduler

/** "Up Next": today and the coming days from the phone's calendars. */
class AgendaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        WidgetUpdater.updateFromProvider(context, goAsync())

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        WidgetUpdater.updateFromProvider(context, goAsync())

    override fun onDisabled(context: Context) {
        val app = context.applicationContext
        app.graph.io.execute { Scheduler.ensure(app) }
    }
}

object AgendaWidget {

    fun render(context: Context, model: WidgetModel, options: Bundle?): RemoteViews =
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews(
                mapOf(
                    SizeF(180f, 100f) to build(context, model, rowBudget = 2),
                    SizeF(180f, 170f) to build(context, model, rowBudget = 4),
                    SizeF(180f, 260f) to build(context, model, rowBudget = 7),
                    SizeF(180f, 360f) to build(context, model, rowBudget = 10),
                ),
            )
        } else {
            build(context, model, rowsFor(options))
        }

    /** About 64 dp of header and padding, then ~44 dp per row. */
    private fun rowsFor(options: Bundle?): Int {
        val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0
        return if (height == 0) 4 else ((height - 64) / 44).coerceIn(1, 12)
    }

    private fun build(context: Context, model: WidgetModel, rowBudget: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_agenda)
        val texts = model.texts

        // "Thu 15 Oct · W42", formatted live by the launcher. Week numbers follow the phone's region (ISO in Sweden).
        val header = texts.dateTime.pattern("EEEdMMM") + " · " + context.getString(R.string.agenda_week_pattern)
        views.setCharSequence(R.id.agenda_date, "setFormat12Hour", header)
        views.setCharSequence(R.id.agenda_date, "setFormat24Hour", header)
        views.setOnClickPendingIntent(R.id.agenda_date, WidgetIntents.openCalendarAt(context, model.now))
        views.removeAllViews(R.id.agenda_rows)

        if (!model.calendarAllowed) {
            views.setViewVisibility(R.id.agenda_add, View.GONE)
            showEmpty(views, context.getString(R.string.widget_agenda_permission), WidgetIntents.openScreen(context, WidgetIntents.SCREEN_CALENDAR))
            return views
        }
        views.setViewVisibility(R.id.agenda_add, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.agenda_add, WidgetIntents.newEvent(context))

        val agenda = model.agenda
        if (agenda == null || agenda.isEmpty) {
            val line = agenda?.let(texts.agenda::emptyLine) ?: context.getString(R.string.widget_agenda_empty)
            showEmpty(views, line, WidgetIntents.openCalendarAt(context, model.now))
            return views
        }
        views.setViewVisibility(R.id.agenda_empty, View.GONE)

        var used = 0
        if (agenda.todayItems.isEmpty()) {
            views.addView(R.id.agenda_rows, label(context, texts.agenda.emptyLine(agenda)))
            used++
        }
        for (day in agenda.days) {
            if (used >= rowBudget) break
            if (day.date != agenda.today) {
                if (used + 1 >= rowBudget) break // a header with no room for its events helps nobody
                views.addView(R.id.agenda_rows, label(context, texts.agenda.dayHeader(day.date, agenda.today)))
                used++
            }
            for (item in day.items) {
                if (used >= rowBudget) break
                views.addView(R.id.agenda_rows, row(context, model, item))
                used++
            }
        }
        return views
    }

    private fun row(context: Context, model: WidgetModel, item: AgendaItem): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_agenda_row)
        row.setTextViewText(R.id.row_title, item.title)
        val time = model.texts.agenda.timeRange(item, model.zone)
        row.setTextViewText(R.id.row_meta, listOfNotNull(time, item.location).joinToString(" · "))
        row.setViewVisibility(R.id.row_now, if (item.isOngoing && !item.instance.allDay) View.VISIBLE else View.GONE)
        row.setInt(R.id.row_bar, "setColorFilter", item.instance.color ?: context.getColor(R.color.tessera_accent))
        row.setOnClickPendingIntent(R.id.agenda_row, WidgetIntents.openEvent(context, item.instance))
        return row
    }

    private fun label(context: Context, text: String): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_agenda_day).apply { setTextViewText(R.id.day_label, text) }

    private fun showEmpty(views: RemoteViews, text: String, tap: PendingIntent) {
        views.setViewVisibility(R.id.agenda_empty, View.VISIBLE)
        views.setTextViewText(R.id.agenda_empty, text)
        views.setOnClickPendingIntent(R.id.agenda_empty, tap)
    }
}
