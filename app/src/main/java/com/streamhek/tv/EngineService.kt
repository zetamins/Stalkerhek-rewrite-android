package com.streamhek.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.streamhek.tv.engine.EngineController
import kotlin.system.exitProcess

class EngineService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted -- run without foreground priority
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                // Kill the process to ensure full cleanup
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
            else -> {
                EngineController.init(filesDir.absolutePath, this)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { EngineController.shutdown() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Engine Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "StreamHek engine running status"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val ip = getLocalIpAddress()
        val mgmtUrl = "http://$ip:${Constants.MGMT_PORT}"

        val stopIntent = Intent(this, EngineService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        @Suppress("DEPRECATION")
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("StreamHek Running")
            .setContentText("Management: $mgmtUrl")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_pause, "Stop Engine", stopPending)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "stalkerhek_engine"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.streamhek.tv.STOP_ENGINE"
    }
}
