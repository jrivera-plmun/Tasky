package com.example.tasky_add_mngmt

import java.util.Locale
import java.util.concurrent.TimeUnit




object SmartTaskPriority {
    fun score(task: TaskyDatabase.Task, now: Long = System.currentTimeMillis()): Int {
        if (task.status.equals("Completed", ignoreCase = true)) return Int.MIN_VALUE
        val hoursLeft = (task.deadline - now).toDouble() / TimeUnit.HOURS.toMillis(1)
        val urgency = when {
            hoursLeft < 0 -> 100
            hoursLeft <= 24 -> 85
            hoursLeft <= 72 -> 65
            hoursLeft <= 168 -> 45
            else -> 20
        }
        val importance = when (task.priority.lowercase(Locale.US)) {
            "high" -> 30
            "medium" -> 18
            else -> 8
        }
        val complexity = when (task.difficulty.lowercase(Locale.US)) {
            "difficult" -> 12
            "moderate" -> 7
            else -> 3
        }
        val effort = when {
            task.remaining >= 180 -> 8
            task.remaining >= 60 -> 5
            else -> 2
        }
        return urgency + importance + complexity + effort
    }

    fun comparator(now: Long = System.currentTimeMillis()): Comparator<TaskyDatabase.Task> =
        compareByDescending<TaskyDatabase.Task> { score(it, now) }
            .thenBy { it.deadline }
            .thenBy { it.title.lowercase(Locale.US) }

    fun label(task: TaskyDatabase.Task, now: Long = System.currentTimeMillis()): String {
        val remaining = task.deadline - now
        return when {
            remaining < 0 -> "Overdue"
            remaining <= TimeUnit.HOURS.toMillis(24) -> "Due soon"
            score(task, now) >= 105 -> "High focus"
            else -> "Planned"
        }
    }
}
