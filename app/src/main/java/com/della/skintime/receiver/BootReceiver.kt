package com.della.skintime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.della.skintime.utils.ReminderManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReminderManager.isReminderEnabled(context)) {
                ReminderManager.scheduleReminder(context)
            }
        }
    }
}
