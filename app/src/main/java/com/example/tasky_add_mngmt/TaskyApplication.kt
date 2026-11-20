package com.example.tasky_add_mngmt

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class TaskyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val mode = when (AccountStore(this).preference("theme", "system")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        TaskReminderScheduler.refresh(this)
    }
}
