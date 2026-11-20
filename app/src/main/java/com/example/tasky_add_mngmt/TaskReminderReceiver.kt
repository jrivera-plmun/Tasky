package com.example.tasky_add_mngmt

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        if (taskId < 0) return
        val task = TaskyDatabase(context).task(taskId) ?: return
        if (task.status == "Completed") return
        val kind = intent.getIntExtra("reminder_kind", 1)
        if (kind !in 0..2) return
        val preference = if (kind == 2) "overdue" else "reminders"
        if (AccountStore(context).preference(preference, "1") != "1") return
        val now = System.currentTimeMillis()
        if (kind == 2 && task.deadline > now) return
        if (kind != 2 && task.deadline <= now) return
        
        if (kind == 1 && task.deadline - now > 16 * 60_000L) return
        if (kind == 0 && task.deadline - now > task.remaining.coerceAtLeast(1) * 60_000L + 60_000L) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("tasky_deadlines", "Task reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, taskId.toInt(),
            Intent(context, TaskDetailsActivity::class.java).putExtra("task_id", taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = when (kind) {
            0 -> "Time to start your task"
            1 -> "Task due in 15 minutes"
            else -> "Unfinished task overdue"
        }
        val body = when (kind) {
            0 -> "${task.title} · ${task.remaining} min remaining"
            else -> task.title
        }
        val notification = NotificationCompat.Builder(context, "tasky_deadlines")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title).setContentText(body)
            .setAutoCancel(true).setContentIntent(open).build()
        try { manager.notify((taskId * 3 + kind).toInt(), notification) }
        catch (_: SecurityException) {  }
    }
}
