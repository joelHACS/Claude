package com.example.servicereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (AlarmScheduler.isActive(context)) {
            AlarmScheduler.scheduleInitial(context)
        }
    }
}
