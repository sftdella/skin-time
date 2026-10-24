package com.della.skintime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.della.skintime.data.db.AppDatabase

class SkinTimeApp : Application() {

    val database: AppDatabase by lazy { AppDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channelId   = getString(R.string.notif_channel_id)
        val channelName = getString(R.string.notif_channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pengingat rutinitas skincare harian"
            enableVibration(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
