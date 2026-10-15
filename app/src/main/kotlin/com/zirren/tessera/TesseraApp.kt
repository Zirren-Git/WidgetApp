package com.zirren.tessera

import android.app.Application
import android.content.Context
import com.zirren.tessera.sync.Scheduler

class TesseraApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.io.execute { Scheduler.ensure(this) }
    }
}

/** The app's single object graph. No DI framework: the app is small and this keeps it readable. */
val Context.graph: AppGraph
    get() = (applicationContext as TesseraApp).graph
