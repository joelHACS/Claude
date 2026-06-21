package com.example.servicereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

object AlarmScheduler {

    private const val REQUEST_CODE = 1001
    const val PREFS_NAME = "prefs"
    const val KEY_ACTIVE = "active"
    const val KEY_DONE_UNTIL = "doneUntil"

    private val berlinTz = TimeZone.getTimeZone("Europe/Berlin")

    /** Called when the user activates reminders or after a reboot. */
    fun scheduleInitial(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, true).apply()

        val cal = Calendar.getInstance(berlinTz)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        if (hour < 14) {
            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            // Next whole hour
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
            if (cal.get(Calendar.HOUR_OF_DAY) < 14) {
                // Wrapped past midnight – wait for 14:00 (date already advanced by add())
                cal.set(Calendar.HOUR_OF_DAY, 14)
            }
        }

        setAlarm(context, cal.timeInMillis)
    }

    /** Called by ReminderReceiver to chain the next hourly alarm. */
    fun scheduleNextHour(context: Context) {
        val cal = Calendar.getInstance(berlinTz)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 1)

        if (cal.get(Calendar.HOUR_OF_DAY) < 14) {
            // Crossed midnight – push to 14:00 (date already correct after add())
            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        setAlarm(context, cal.timeInMillis)
    }

    /** Called when the user confirms "Erledigt" – pauses until 14:00 next day. */
    fun scheduleForNextDay14(context: Context) {
        val cal = Calendar.getInstance(berlinTz)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 14)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        prefs(context).edit().putLong(KEY_DONE_UNTIL, cal.timeInMillis).apply()
        setAlarm(context, cal.timeInMillis)
    }

    fun deactivate(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context))
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_DONE_UNTIL, 0L)
            .apply()
    }

    fun isActive(context: Context) = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun getDoneUntil(context: Context) = prefs(context).getLong(KEY_DONE_UNTIL, 0L)

    private fun setAlarm(context: Context, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            buildPendingIntent(context)
        )
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
