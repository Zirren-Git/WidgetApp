package com.zirren.tessera.widget

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.zirren.tessera.core.calendar.EventInstance
import com.zirren.tessera.sync.SystemEventsReceiver
import com.zirren.tessera.ui.MainActivity
import java.time.Instant

/**
 * Smart default taps: the clock opens alarms, the date opens the calendar, an event opens
 * that event, weather opens Tessera's forecast. Falls back to Tessera when no app handles a tap.
 */
object WidgetIntents {
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    const val SCREEN_HOME = "home"
    const val SCREEN_FORECAST = "forecast"
    const val SCREEN_WEATHER = "weather"
    const val SCREEN_CALENDAR = "calendar"
    const val SCREEN_TOOLS = "tools"
    const val SCREEN_CALCULATOR = "calculator"

    /** tessera://open/<screen>: what a widget tap opens, and what a tool's home-screen shortcut opens. */
    fun screenIntent(context: Context, screen: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("tessera://open/$screen"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun openScreen(context: Context, screen: String): PendingIntent =
        PendingIntent.getActivity(context, screen.hashCode(), screenIntent(context, screen), FLAGS)

    fun showAlarms(context: Context): PendingIntent {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return activityOr(context, intent, REQUEST_ALARMS, SCREEN_HOME)
    }

    fun openCalendarAt(context: Context, time: Instant): PendingIntent {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        ContentUris.appendId(builder, time.toEpochMilli())
        val intent = Intent(Intent.ACTION_VIEW, builder.build()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return activityOr(context, intent, REQUEST_CALENDAR, SCREEN_CALENDAR)
    }

    fun openEvent(context: Context, event: EventInstance): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin.toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Repeating events share one URI, so the request code tells their occurrences apart.
        val requestCode = (event.eventId * 31 + event.begin.toEpochMilli() / 60_000).toInt()
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    fun newEvent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return activityOr(context, intent, REQUEST_NEW_EVENT, SCREEN_CALENDAR)
    }

    fun refreshWeather(context: Context): PendingIntent {
        val intent = Intent(context, SystemEventsReceiver::class.java).setAction(SystemEventsReceiver.ACTION_REFRESH_WEATHER)
        return PendingIntent.getBroadcast(context, REQUEST_REFRESH, intent, FLAGS)
    }

    private fun activityOr(context: Context, intent: Intent, requestCode: Int, fallbackScreen: String): PendingIntent =
        if (intent.resolveActivity(context.packageManager) != null) {
            PendingIntent.getActivity(context, requestCode, intent, FLAGS)
        } else {
            openScreen(context, fallbackScreen)
        }

    private const val REQUEST_ALARMS = 10
    private const val REQUEST_CALENDAR = 11
    private const val REQUEST_NEW_EVENT = 12
    private const val REQUEST_REFRESH = 13
}
