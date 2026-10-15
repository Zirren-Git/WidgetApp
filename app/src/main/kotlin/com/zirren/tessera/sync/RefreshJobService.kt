package com.zirren.tessera.sync

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.zirren.tessera.core.weather.RefreshTrigger
import com.zirren.tessera.graph
import com.zirren.tessera.widget.WidgetUpdater

/** Runs every scheduled job, then redraws the widgets. */
class RefreshJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        val context = applicationContext
        context.graph.io.execute {
            try {
                when (params.jobId) {
                    Scheduler.JOB_WEATHER_PERIODIC -> context.graph.weather.refresh(RefreshTrigger.SCHEDULED)
                    Scheduler.JOB_WEATHER_NOW -> context.graph.weather.refresh(trigger(params))
                    else -> Unit // Calendar change or time boundary: redrawing is the whole job.
                }
                WidgetUpdater.updateAll(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Job ${params.jobId} failed", t)
            } finally {
                // Content-trigger jobs are one-shot; re-arm before finishing (the documented pattern).
                if (params.jobId == Scheduler.JOB_CALENDAR_CHANGED) Scheduler.watchCalendar(context)
                jobFinished(params, false)
            }
        }
        return true
    }

    /** Stopped early (e.g. lost network): retry, except for boundaries, which get rescheduled on the next redraw. */
    override fun onStopJob(params: JobParameters): Boolean = params.jobId != Scheduler.JOB_BOUNDARY

    private fun trigger(params: JobParameters): RefreshTrigger {
        val name = params.extras.getString(Scheduler.EXTRA_TRIGGER)
        return RefreshTrigger.entries.firstOrNull { it.name == name } ?: RefreshTrigger.USER
    }

    private companion object {
        const val TAG = "TesseraJobs"
    }
}
