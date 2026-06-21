package com.example.servicereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class DoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Dismiss the notification
        NotificationManagerCompat.from(context).cancel(ReminderReceiver.NOTIFICATION_ID)

        // Reschedule for 14:00 tomorrow
        AlarmScheduler.scheduleForNextDay14(context)
    }
}
