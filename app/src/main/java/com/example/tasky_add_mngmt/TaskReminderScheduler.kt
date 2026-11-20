package com.example.tasky_add_mngmt

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent


object TaskReminderScheduler {
    private const val START = 0
    private const val DUE = 1
    private const val OVERDUE = 2

    private fun pending(context: Context, taskId: Long, kind: Int): PendingIntent =
        PendingIntent.getBroadcast(context, (taskId * 3 + kind).toInt(),
            Intent(context, TaskReminderReceiver::class.java)
                .putExtra("task_id", taskId).putExtra("reminder_kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun refresh(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val store = AccountStore(context)
        val reminders = store.preference("reminders", "1") == "1"
        val overdue = store.preference("overdue", "1") == "1"
        val now = System.currentTimeMillis()
        TaskyDatabase(context).tasks().forEach { task ->
            for (kind in START..OVERDUE) {
                val pi = pending(context, task.id, kind)
                alarm.cancel(pi)
                val enabled = if (kind == OVERDUE) overdue else reminders
                val whenAt = when (kind) {
                    START -> task.deadline - task.remaining.coerceAtLeast(1) * 60_000L
                    DUE -> task.deadline - 15 * 60_000L
                    else -> task.deadline + 60_000L
                }
                
                val triggerAt = if (kind == DUE && whenAt <= now && task.deadline > now)
                    now + 5_000L else whenAt
                if (enabled && task.status != "Completed" && triggerAt > now &&
                    (kind != START || task.remaining > 0)) {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
    }

    fun cancel(context: Context, id: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (kind in START..OVERDUE) alarm.cancel(pending(context, id, kind))
    }
}
