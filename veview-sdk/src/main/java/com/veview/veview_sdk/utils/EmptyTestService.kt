package com.veview.veview_sdk.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import com.veview.veview_sdk.R

class EmptyTestService : Service() {
    private val binder = LocalBinder()
    private val NOTIFICATION_CHANNEL_ID = "veview_test_channel"

    inner class LocalBinder : Binder() {
        fun getService(): EmptyTestService = this@EmptyTestService
    }

    override fun onCreate() {
        super.onCreate()

        val notifChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "VeView Test",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notifChannel)

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("VeView Test")
            .setContentText("Recording is ongoing")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
