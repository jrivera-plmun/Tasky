package com.example.tasky_add_mngmt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            TaskReminderScheduler.refresh(context)
        }
    }
}
