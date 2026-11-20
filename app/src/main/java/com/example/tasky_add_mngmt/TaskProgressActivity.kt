package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TaskProgressActivity : AppCompatActivity() {
    private val focusIndicator by lazy { FocusIndicator(this) }

    data class TaskRow(val title: String, val meta: String, val percent: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_progress)

        findViewById<TextView>(R.id.tvTitle).text = "Task Progress"
        findViewById<View>(R.id.cardWorkloadLink).setOnClickListener { startActivity(Intent(this, WorkloadMonitoringActivity::class.java)) }
        setupBottomNavigation(NavTab.PROGRESS)
    }

    private fun refreshProgress() {
        refreshFocusHistory()
        val tasks = TaskyDatabase(this).tasks()
        val complete = tasks.count { it.status == "Completed" }
        val percent = if(tasks.isEmpty()) 0 else 100 * complete / tasks.size
        findViewById<TextView>(R.id.tvTaskTotal).text = tasks.size.toString()
        findViewById<TextView>(R.id.tvCompletedCount).text = complete.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = tasks.count { it.status != "Completed" }.toString()
        findViewById<TextView>(R.id.tvOverdueCount).text = tasks.count { it.overdue }.toString()
        findViewById<TextView>(R.id.tvTodayCount).text = "$complete of ${tasks.size} tasks"
        findViewById<TextView>(R.id.tvTodayPercent).text = "$percent%"
        findViewById<ProgressBar>(R.id.pbToday).progress = percent
        findViewById<TextView>(R.id.tvWeeklyPercent).text = "$percent%"
        findViewById<ProgressBar>(R.id.pbWeekly).progress = percent


        val rows = tasks.map { TaskRow(it.title, "${it.category} · ${it.status} · ${it.estimated} min", it.percent) }
        findViewById<RecyclerView>(R.id.rvTasks).apply {
            layoutManager = LinearLayoutManager(this@TaskProgressActivity)
            adapter = TaskAdapter(rows)
            isNestedScrollingEnabled = false
        }

    }
    private fun refreshFocusHistory() {
        val summary = TaskyDatabase(this).focusSummary()
        fun duration(seconds: Long): String = if (seconds >= 3600L) "${seconds / 3600}h ${(seconds % 3600) / 60}m" else "${seconds / 60}m ${seconds % 60}s"
        val focusDb = TaskyDatabase(this)
        findViewById<TextView>(R.id.tvFocusSummary).text =
            "${duration(summary.totalSeconds)} focused  •  ${summary.completedSessions} finished sessions\n" +
            "${duration(focusDb.totalBreakSeconds())} on breaks  •  ${focusDb.distractionCount()} distractions recorded"
        val week = findViewById<LinearLayout>(R.id.layoutFocusWeek)
        week.removeAllViews()
        val max = (summary.dailySeconds.maxOrNull() ?: 0L).coerceAtLeast(60L)
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        summary.dailySeconds.forEachIndexed { index, seconds ->
            val day = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index - 6) }
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, dp(120), 1f)
            }
            column.addView(TextView(this).apply {
                text = "${seconds / 60}m"; textSize = 10f
                setTextColor(getColor(R.color.text_primary))
                gravity = android.view.Gravity.CENTER
            })
            column.addView(View(this).apply {
                setBackgroundColor(Color.parseColor("#7955DB"))
                layoutParams = LinearLayout.LayoutParams(dp(19),
                    ((seconds.toFloat() / max) * dp(70)).toInt().coerceAtLeast(dp(3)))
            })
            column.addView(TextView(this).apply {
                text = dayFormat.format(day.time); textSize = 10f
                setTextColor(getColor(R.color.text_primary))
                gravity = android.view.Gravity.CENTER
            })
            week.addView(column)
        }
        val history = findViewById<LinearLayout>(R.id.layoutFocusHistory)
        history.removeAllViews()
        if (summary.sessions.isEmpty()) {
            history.addView(TextView(this).apply {
                text = "No focus sessions yet. Start one from a task to see it here."
                textSize = 13f; setTextColor(getColor(R.color.text_primary))
            })
        }
        val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
        summary.sessions.take(12).forEach { session ->
            history.addView(TextView(this).apply {
                text = "${session.taskTitle}\n${dateFormat.format(Date(session.startedAt))}  •  ${duration(session.seconds)}  •  ${session.status}"
                textSize = 13f
                setTextColor(getColor(R.color.text_primary))
                setPadding(0, dp(8), 0, dp(8))
            })
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class TaskAdapter(private val rows: List<TaskRow>) :
        RecyclerView.Adapter<TaskAdapter.TaskHolder>() {
        class TaskHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val meta: TextView = view.findViewById(R.id.tvMeta)
            val percent: TextView = view.findViewById(R.id.tvPercent)
            val progress: ProgressBar = view.findViewById(R.id.pbTask)
            val icon: ImageView = view.findViewById(R.id.ivStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_task_detail, parent, false)
            return TaskHolder(view)
        }

        override fun onBindViewHolder(holder: TaskHolder, position: Int) {
            val row = rows[position]
            holder.title.text = row.title
            holder.meta.text = row.meta
            holder.percent.text = "${row.percent}%"
            holder.progress.progress = row.percent
        }

        override fun getItemCount() = rows.size
    }
    override fun onResume() {
        focusIndicator.start()
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        refreshProgress()
    }
    override fun onPause() {
        focusIndicator.stop()
        super.onPause()
    }
}
