package com.zirren.tessera.sync

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.provider.CalendarContract
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.graph
import com.zirren.tessera.widget.AgendaWidgetProvider
import com.zirren.tessera.widget.TodayWidgetProvider
import com.zirren.tessera.widget.WidgetUpdater
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * All background work goes through JobScheduler: no permanent service, no exact-alarm permission.
 * Clocks don't need any of this; they tick inside the launcher (TextClock).
 *
 * - Weather: periodic job, hourly, network required.
 * - Calendar: a content-trigger job that fires when the phone's calendar changes.
 * - Boundaries: a one-off job at the next moment a widget would look different
 *   (an event starts or ends, the hour turns, midnight).
 */
object Scheduler {
    const val JOB_WEATHER_PERIODIC = 1_001
    const val JOB_WEATHER_NOW = 1_002
    const val JOB_CALENDAR_CHANGED = 1_003
    const val JOB_BOUNDARY = 1_004
    const val EXTRA_TRIGGER = "trigger"

    /** Make the scheduled jobs match what's on the home screen. Cheap and idempotent. */
    fun ensure(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        val hasWeatherWidgets = WidgetUpdater.hasWidgets(context, TodayWidgetProvider::class.java)
        val hasAnyWidgets = hasWeatherWidgets || WidgetUpdater.hasWidgets(context, AgendaWidgetProvider::class.java)

        if (hasWeatherWidgets && context.graph.settings.activePlace() != null) {
            // Re-scheduling a periodic job restarts its clock, so only schedule when it's missing.
            if (jobs.getPendingJob(JOB_WEATHER_PERIODIC) == null) {
                jobs.schedule(
                    JobInfo.Builder(JOB_WEATHER_PERIODIC, service(context))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(TimeUnit.MINUTES.toMillis(60), TimeUnit.MINUTES.toMillis(15))
                        .setPersisted(true)
                        .build(),
                )
            }
        } else {
            jobs.cancel(JOB_WEATHER_PERIODIC)
        }

        if (hasAnyWidgets && context.graph.calendar.hasPermission()) {
            if (jobs.getPendingJob(JOB_CALENDAR_CHANGED) == null) watchCalendar(context)
        } else {
            jobs.cancel(JOB_CALENDAR_CHANGED)
        }

        if (!hasAnyWidgets) jobs.cancel(JOB_BOUNDARY)
    }

    /**
     * Fires shortly after any change in the phone's calendars. Content-trigger jobs are one-shot
     * and can't be persisted, so this is re-armed after every run and after reboot.
     */
    fun watchCalendar(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        jobs.schedule(
            JobInfo.Builder(JOB_CALENDAR_CHANGED, service(context))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(CalendarContract.CONTENT_URI, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS),
                )
                .setTriggerContentUpdateDelay(2_000)
                .setTriggerContentMaxDelay(15_000)
                .build(),
        )
    }

    /** Fetch weather as soon as there is a network (expedited on Android 12+). */
    fun refreshWeatherSoon(context: Context, trigger: RefreshTrigger) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        val extras = PersistableBundle().apply { putString(EXTRA_TRIGGER, trigger.name) }
        val builder = JobInfo.Builder(JOB_WEATHER_NOW, service(context))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(extras)
        if (Build.VERSION.SDK_INT >= 31) builder.setExpedited(true)
        if (jobs.schedule(builder.build()) != JobScheduler.RESULT_SUCCESS && Build.VERSION.SDK_INT >= 31) {
            // Out of expedited quota: run as a normal job instead.
            jobs.schedule(builder.setExpedited(false).build())
        }
    }

    /** Re-render widgets at [at] (clamped to 30 s – 24 h from now). Replaces any earlier boundary job. */
    fun scheduleBoundary(context: Context, at: Instant) {
        val jobs = context.getSystemService(JobScheduler::class.java) ?: return
        val delay = Duration.between(Instant.now(), at).toMillis().coerceIn(30_000L, TimeUnit.HOURS.toMillis(24))
        jobs.schedule(
            JobInfo.Builder(JOB_BOUNDARY, service(context))
                .setMinimumLatency(delay)
                .setOverrideDeadline(delay + 60_000L)
                .setPersisted(true)
                .build(),
        )
    }

    fun pendingWeatherJob(context: Context): JobInfo? =
        context.getSystemService(JobScheduler::class.java)?.getPendingJob(JOB_WEATHER_PERIODIC)

    private fun service(context: Context) = ComponentName(context, RefreshJobService::class.java)
}
