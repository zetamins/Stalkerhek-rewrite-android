package com.streamhek.tv

import android.app.Application
import android.content.Intent
import com.streamhek.tv.engine.EngineController

class StreamHekApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, EngineService::class.java))
    }

    override fun onTerminate() {
        EngineController.shutdown()
        super.onTerminate()
    }
}
