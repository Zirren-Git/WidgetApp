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
import com.zirren.tessera.core.model.Age
import com.zirren.tessera.core.weather.Freshness
import com.zirren.tessera.core.weather.PrecipitationOutlook
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.data.WeatherSnapshot
import com.zirren.tessera.graph
import com.zirren.tessera.sync.Scheduler

/** "Morning Paper": date, a big clock, today's weather and the next event. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        WidgetUpdater.updateFromProvider(context, goAsync())

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        WidgetUpdater.updateFromProvider(context, goAsync())

    override fun onEnabled(context: Context) = Scheduler.refreshWeatherSoon(context, RefreshTrigger.APP_OPENED)

    override fun onDisabled(context: Context) {
        val app = context.applicationContext
        app.graph.io.execute { Scheduler.ensure(app) }
    }
}

object TodayWidget {
    private enum class Size { SMALL, MEDIUM, LARGE }

    /**
     * Android 12+ gets all three layouts at once and switches between them instantly while
     * the user resizes; older versions get the one matching the reported size.
     */
    fun render(context: Context, model: WidgetModel, options: Bundle?): RemoteViews =
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews(
                mapOf(
                    SizeF(100f, 80f) to build(context, model, Size.SMALL),
                    SizeF(180f, 100f) to build(context, model, Size.MEDIUM),
                    SizeF(180f, 190f) to build(context, model, Size.LARGE),
                ),
            )
        } else {
            build(context, model, sizeFor(options))
        }

    private fun sizeFor(options: Bundle?): Size {
        // Portrait reports the minimum width and the maximum height.
        val width = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
        val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) ?: 0
        return when {
            width == 0 -> Size.MEDIUM
            width >= 180 && height >= 190 -> Size.LARGE
            width >= 180 -> Size.MEDIUM
            else -> Size.SMALL
        }
    }

    private fun build(context: Context, model: WidgetModel, size: Size): RemoteViews {
        val layout = when (size) {
            Size.SMALL -> R.layout.widget_today_small
            Size.MEDIUM -> R.layout.widget_today
            Size.LARGE -> R.layout.widget_today_large
        }
        val views = RemoteViews(context.packageName, layout)

        // The clock and date are TextClocks: the launcher ticks them, so they're never late.
        val datePattern = model.texts.dateTime.pattern(if (size == Size.SMALL) "EEEdMMM" else "EEEEdMMMM")
        views.setCharSequence(R.id.today_date, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.today_date, "setFormat24Hour", datePattern)
        views.setOnClickPendingIntent(R.id.today_date, WidgetIntents.openCalendarAt(context, model.now))
        views.setOnClickPendingIntent(R.id.today_time, WidgetIntents.showAlarms(context))

        bindWeather(context, views, model, size)
        if (size != Size.SMALL) {
            bindStatus(context, views, model)
            bindNextEvent(context, views, model)
        }
        if (size == Size.LARGE) bindHours(views, model)
        return views
    }

    private fun bindWeather(context: Context, views: RemoteViews, model: WidgetModel, size: Size) {
        val snapshot = model.weather
        val current = snapshot?.report?.current
        val text = model.texts.weather
        when {
            !model.weatherConfigured -> {
                views.setViewVisibility(R.id.weather_temp, View.GONE)
                views.setTextViewText(R.id.weather_line, context.getString(R.string.widget_set_place))
                views.setOnClickPendingIntent(R.id.weather_block, WidgetIntents.openScreen(context, WidgetIntents.SCREEN_WEATHER))
            }
            snapshot == null || current == null -> {
                views.setViewVisibility(R.id.weather_temp, View.VISIBLE)
                views.setTextViewCompoundDrawablesRelative(R.id.weather_temp, 0, 0, 0, 0)
                views.setTextViewText(R.id.weather_temp, "\u2013")
                views.setTextViewText(R.id.weather_line, context.getString(R.string.widget_weather_loading))
                views.setOnClickPendingIntent(R.id.weather_block, WidgetIntents.refreshWeather(context))
            }
            else -> {
                val temperature = text.temperature(current.temperature)
                val line = weatherLine(snapshot, model, size)
                views.setViewVisibility(R.id.weather_temp, View.VISIBLE)
                // The icon is the temperature's compound drawable: one view instead of two.
                views.setTextViewCompoundDrawablesRelative(R.id.weather_temp, WeatherIcons.drawable(current.symbol?.icon), 0, 0, 0)
                views.setTextViewText(R.id.weather_temp, temperature)
                views.setTextViewText(R.id.weather_line, line)
                views.setContentDescription(
                    R.id.weather_block,
                    listOfNotNull(temperature, current.symbol?.label, line).distinct().joinToString(", "),
                )
                views.setOnClickPendingIntent(R.id.weather_block, WidgetIntents.openScreen(context, WidgetIntents.SCREEN_FORECAST))
            }
        }
    }

    /** When it might rain, say so; otherwise the sky and today's high and low. */
    private fun weatherLine(snapshot: WeatherSnapshot, model: WidgetModel, size: Size): String {
        val text = model.texts.weather
        val outlook = snapshot.report.outlook
        if (outlook is PrecipitationOutlook.StartsAt || outlook is PrecipitationOutlook.Ongoing) return text.outlook(outlook)
        val condition = snapshot.report.current?.symbol?.label
        val highLow = snapshot.report.today?.let(text::highLow)
        return if (size == Size.SMALL) condition ?: highLow.orEmpty() else listOfNotNull(condition, highLow).joinToString(" · ")
    }

    /** Quiet freshness note, only when data is getting old. Tapping it refreshes. */
    private fun bindStatus(context: Context, views: RemoteViews, model: WidgetModel) {
        val status = model.weather?.let { snapshot ->
            val age = model.texts.age.format(Age.between(snapshot.fetchedAt, model.now))
            when (snapshot.freshness) {
                Freshness.AGING -> context.getString(R.string.widget_updated_ago, age)
                Freshness.STALE -> context.getString(R.string.widget_offline_updated_ago, age)
                else -> null
            }
        }
        if (status == null) {
            views.setViewVisibility(R.id.today_status, View.GONE)
        } else {
            views.setViewVisibility(R.id.today_status, View.VISIBLE)
            views.setTextViewText(R.id.today_status, status)
            views.setOnClickPendingIntent(R.id.today_status, WidgetIntents.refreshWeather(context))
        }
    }

    private fun bindNextEvent(context: Context, views: RemoteViews, model: WidgetModel) {
        val agendaText = model.texts.agenda
        val agenda = model.agenda
        val next = agenda?.nextUp
        when {
            !model.calendarAllowed && !model.showCalendarPrompt -> {
                views.setViewVisibility(R.id.next_event, View.GONE)
                views.setViewVisibility(R.id.today_divider, View.GONE)
            }
            !model.calendarAllowed ->
                showHint(views, context.getString(R.string.widget_calendar_prompt), WidgetIntents.openScreen(context, WidgetIntents.SCREEN_CALENDAR))
            agenda == null || next == null ->
                showHint(views, agenda?.let(agendaText::emptyLine).orEmpty(), WidgetIntents.openCalendarAt(context, model.now))
            else -> {
                views.setViewVisibility(R.id.next_event, View.VISIBLE)
                views.setViewVisibility(R.id.today_divider, View.VISIBLE)
                views.setViewVisibility(R.id.next_event_hint, View.GONE)
                views.setViewVisibility(R.id.next_event_bar, View.VISIBLE)
                views.setViewVisibility(R.id.next_event_when, View.VISIBLE)
                views.setViewVisibility(R.id.next_event_title, View.VISIBLE)
                views.setInt(R.id.next_event_bar, "setColorFilter", next.instance.color ?: context.getColor(R.color.tessera_accent))
                views.setTextViewText(R.id.next_event_when, agendaText.whenLabel(next, model.now, model.zone))
                views.setTextViewText(R.id.next_event_title, next.title)
                views.setOnClickPendingIntent(R.id.next_event, WidgetIntents.openEvent(context, next.instance))
            }
        }
    }

    private fun showHint(views: RemoteViews, hint: String, tap: PendingIntent) {
        views.setViewVisibility(R.id.next_event, View.VISIBLE)
        views.setViewVisibility(R.id.today_divider, View.VISIBLE)
        views.setViewVisibility(R.id.next_event_bar, View.GONE)
        views.setViewVisibility(R.id.next_event_when, View.GONE)
        views.setViewVisibility(R.id.next_event_title, View.GONE)
        views.setViewVisibility(R.id.next_event_hint, View.VISIBLE)
        views.setTextViewText(R.id.next_event_hint, hint)
        views.setOnClickPendingIntent(R.id.next_event, tap)
    }

    /** Large size only: the next five hours. */
    private fun bindHours(views: RemoteViews, model: WidgetModel) {
        val columns = arrayOf(
            intArrayOf(R.id.hour_0, R.id.hour_0_time, R.id.hour_0_icon, R.id.hour_0_temp),
            intArrayOf(R.id.hour_1, R.id.hour_1_time, R.id.hour_1_icon, R.id.hour_1_temp),
            intArrayOf(R.id.hour_2, R.id.hour_2_time, R.id.hour_2_icon, R.id.hour_2_temp),
            intArrayOf(R.id.hour_3, R.id.hour_3_time, R.id.hour_3_icon, R.id.hour_3_temp),
            intArrayOf(R.id.hour_4, R.id.hour_4_time, R.id.hour_4_icon, R.id.hour_4_temp),
        )
        val hours = model.weather?.report?.hourly?.drop(1)?.take(columns.size).orEmpty()
        views.setViewVisibility(R.id.hours, if (hours.isEmpty()) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.hours_divider, if (hours.isEmpty()) View.GONE else View.VISIBLE)
        columns.forEachIndexed { index, (column, time, icon, temperature) ->
            val hour = hours.getOrNull(index)
            if (hour == null) {
                views.setViewVisibility(column, View.INVISIBLE)
            } else {
                views.setViewVisibility(column, View.VISIBLE)
                views.setTextViewText(time, model.texts.dateTime.hour(hour.time.atZone(model.zone).toLocalTime()))
                views.setImageViewResource(icon, WeatherIcons.drawable(hour.symbol?.icon))
                views.setTextViewText(temperature, hour.temperature?.let(model.texts.weather::temperature) ?: "\u2013")
            }
        }
    }
}
