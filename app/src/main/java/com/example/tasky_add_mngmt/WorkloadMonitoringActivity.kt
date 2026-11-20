package com.example.tasky_add_mngmt

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity

class WorkloadMonitoringActivity : AppCompatActivity() {
    private val focusIndicator by lazy { FocusIndicator(this) }

    private val focusHandler = Handler(Looper.getMainLooper())
    private val focusRefresh = object : Runnable {
        override fun run() {
            if (!isFinishing) {
                renderFocusHistory()
                focusHandler.postDelayed(this, 1000L)
            }
        }
    }
    private val selectedDate = java.util.Calendar.getInstance()
    private fun renderSelectedDate() {
        findViewById<TextView>(R.id.tvSelectedDate).text =
            java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US).format(selectedDate.time)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workload_monitoring)

        findViewById<TextView>(R.id.tvTitle).text = "Workload Monitoring"
        findViewById<View>(R.id.btnPrevDay).setOnClickListener { updateDate(-1) }
        findViewById<View>(R.id.btnNextDay).setOnClickListener { updateDate(1) }

        renderSelectedDate()
        refreshWorkload()
        setupBottomNavigation(NavTab.PROGRESS)
    }

    private fun updateDate(offset: Int) {
        selectedDate.add(java.util.Calendar.DAY_OF_MONTH, offset)
        renderSelectedDate()
        refreshWorkload()
    }

    private fun refreshWorkload() {
        val calendar = selectedDate.clone() as java.util.Calendar
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        val end = start + 24L * 60 * 60 * 1000
        val tasks = TaskyDatabase(this).tasks().filter { it.deadline in start until end }
        val done = tasks.filter { it.status == "Completed" }
        val totalMinutes = tasks.sumOf { it.estimated }
        findViewById<TextView>(R.id.tvEstimate).text = "$totalMinutes min"
        val preferences = getSharedPreferences("tasky_schedule_planner_${AccountStore(this).currentId}", MODE_PRIVATE)
        val dailyBudget = preferences.getInt("daily_budget_minutes", 180).coerceAtLeast(1)
        val availableStart = preferences.getInt("available_start_minutes", 9 * 60)
        val availableEnd = preferences.getInt("available_end_minutes", 17 * 60)
        val availableWindow = (availableEnd - availableStart).coerceAtLeast(1)
        val usableMinutes = minOf(dailyBudget, availableWindow)
        val openTasks = tasks.filter { it.status != "Completed" }
        val remainingMinutes = openTasks.sumOf { it.remaining.coerceAtLeast(0) }
        val weightedMinutes = openTasks.sumOf { task ->
            val multiplier = when (task.difficulty.lowercase(Locale.US)) {
                "easy" -> 1.0
                "difficult", "hard" -> 1.4
                else -> 1.2
            }
            task.remaining.coerceAtLeast(0) * multiplier
        }
        val loadRatio = weightedMinutes / usableMinutes
        val workloadLevel = when {
            openTasks.isEmpty() -> "Light"
            loadRatio <= 0.6 -> "Light"
            loadRatio <= 1.0 -> "Moderate"
            else -> "Heavy"
        }
        findViewById<TextView>(R.id.tvHeroFooter).text =
            "$remainingMinutes min remaining · $usableMinutes min available · difficulty-adjusted load ${"%.0f".format(Locale.US, loadRatio * 100)}%"
        findViewById<TextView>(R.id.tvLevel).text = workloadLevel
        findViewById<TextView>(R.id.tvTaskCount).text = tasks.size.toString()
        findViewById<TextView>(R.id.tvProgressCount).text = "${done.size} of ${tasks.size} complete"
        findViewById<ProgressBar>(R.id.pbToday).progress =
            if (tasks.isEmpty()) 0 else done.size * 100 / tasks.size
        findViewById<TextView>(R.id.tvDoneMinutes).text = "${done.sumOf { it.estimated }} min completed"
        findViewById<TextView>(R.id.tvOpenMinutes).text =
            "${tasks.filter { it.status != "Completed" }.sumOf { it.remaining }} min in open tasks"
        val work = tasks.filter { it.category.equals("Work", true) }.sumOf { it.estimated }
        val school = tasks.filter { it.category.equals("School", true) || it.category.equals("Study", true) }.sumOf { it.estimated }
        val personal = tasks.filter { it.category.equals("Personal", true) }.sumOf { it.estimated }
        val family = tasks.filter { it.category.equals("Family", true) }.sumOf { it.estimated }
        val others = tasks.filter { it.category.equals("Others", true) }.sumOf { it.estimated }
        findViewById<TextView>(R.id.tvWorkMin).text = "$work min"
        findViewById<TextView>(R.id.tvSchoolMin).text = "$school min"
        findViewById<TextView>(R.id.tvPersonalMin).text = "$personal min"
        findViewById<TextView>(R.id.tvFamilyMin).text = "$family min"
        findViewById<TextView>(R.id.tvOthersMin).text = "$others min"
        val categoryTotal = totalMinutes.coerceAtLeast(1)
        findViewById<ProgressBar>(R.id.pbWork).progress = work * 100 / categoryTotal
        findViewById<ProgressBar>(R.id.pbSchool).progress = school * 100 / categoryTotal
        findViewById<ProgressBar>(R.id.pbPersonal).progress = personal * 100 / categoryTotal
        findViewById<ProgressBar>(R.id.pbFamily).progress = family * 100 / categoryTotal
        findViewById<ProgressBar>(R.id.pbOthers).progress = others * 100 / categoryTotal
        renderWeeklyChart()
        highlightWorkload(workloadLevel)
    }

    private fun highlightWorkload(level: String) {
        val dark = (resources.configuration.uiMode and 48) == 32
        val selected = Color.rgb(118, 86, 219)
        val neutral = if (dark) Color.rgb(43, 39, 55) else Color.rgb(242, 239, 250)
        listOf(Triple(R.id.chipLight, R.id.tvChipLightName, "Light"),
            Triple(R.id.chipModerate, R.id.tvChipModerateName, "Moderate"),
            Triple(R.id.chipHeavy, R.id.tvChipHeavyName, "Heavy")).forEach { (chipId, labelId, name) ->
            val active = name == level
            val chip = findViewById<LinearLayout>(chipId)
            chip.background = GradientDrawable().apply {
                cornerRadius = 28f * resources.displayMetrics.density
                setColor(if (active) selected else neutral)
            }
            for (i in 0 until chip.childCount) {
                (chip.getChildAt(i) as? TextView)?.setTextColor(
                    if (active) Color.WHITE else if (dark) Color.rgb(221, 216, 235) else Color.rgb(65, 57, 83))
            }
            findViewById<TextView>(labelId).setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun Int.dpFocus() = (this * resources.displayMetrics.density).toInt()
    private var showAllFocusSessions = false

    private fun focusColor() = if ((resources.configuration.uiMode and 48) == 32)
        Color.WHITE else Color.rgb(45, 37, 65)
    private fun mutedFocusColor() = if ((resources.configuration.uiMode and 48) == 32)
        Color.rgb(194, 186, 213) else Color.rgb(112, 104, 135)
    private fun focusSurfaceColor() = if ((resources.configuration.uiMode and 48) == 32)
        Color.rgb(52, 46, 68) else Color.rgb(244, 241, 251)
    private fun roundedFocusBackground(color: Int, radius: Int = 14) = GradientDrawable().apply {
        cornerRadius = radius.dpFocus().toFloat()
        setColor(color)
    }
    private fun focusText(text: String, size: Float = 14f, bold: Boolean = false, muted: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(if (muted) mutedFocusColor() else focusColor())
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun formatFocus(seconds: Long): String {
        val minutes = seconds / 60
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m ${seconds % 60}s"
    }

    private fun renderFocusHistory() {
        val container = findViewById<LinearLayout>(R.id.focusHistoryContainer)
        val summary = try { TaskyDatabase(this).focusSummary() } catch (e: Exception) {
            container.removeAllViews()
            container.addView(focusText("Focus history unavailable: ${e.message}"))
            return
        }
        container.removeAllViews()
        container.addView(focusText("Focus & Productivity", 19f, true))
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 14.dpFocus(), 0, 16.dpFocus())
        }
        fun statCard(value: String, label: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dpFocus(), 13.dpFocus(), 10.dpFocus(), 13.dpFocus())
            background = roundedFocusBackground(focusSurfaceColor())
            addView(focusText(value, 21f, true))
            addView(focusText(label, 12f, muted = true).apply { setPadding(0, 5.dpFocus(), 0, 0) })
        }
        stats.addView(statCard(formatFocus(summary.totalSeconds), "Total focus time"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 5.dpFocus()
            })
        stats.addView(statCard(summary.completedSessions.toString(), "Finished sessions"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 5.dpFocus()
            })
        container.addView(stats)
        container.addView(focusText("Focus time this week", 15f, true))
        container.addView(focusText("Last 7 days • ${formatFocus(summary.dailySeconds.sum())} total", 12f, muted = true).apply {
            setPadding(0, 5.dpFocus(), 0, 12.dpFocus())
        })
        val chart = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 0, 12.dpFocus())
        }
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        val today = java.util.Calendar.getInstance()
        val maxSeconds = (summary.dailySeconds.maxOrNull() ?: 0L).coerceAtLeast(1L)
        summary.dailySeconds.forEachIndexed { index, seconds ->
            val day = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, index - 6) }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            col.addView(focusText(if (seconds == 0L) "" else formatFocus(seconds), 10f, muted = true).apply {
                gravity = Gravity.CENTER
                maxLines = 1
            }, LinearLayout.LayoutParams(-1, 20.dpFocus()))
            val barHeight = if (seconds == 0L) 3.dpFocus() else
                (100.dpFocus().toLong() * seconds / maxSeconds).toInt().coerceAtLeast(9.dpFocus())
            col.addView(View(this).apply {
                background = roundedFocusBackground(
                    if (seconds == 0L) focusSurfaceColor() else Color.rgb(146, 108, 239), 5)
            }, LinearLayout.LayoutParams(22.dpFocus(), barHeight))
            col.addView(focusText(dayFormat.format(day.time), 11f, muted = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, 7.dpFocus(), 0, 0)
            })
            chart.addView(col, LinearLayout.LayoutParams(0, 146.dpFocus(), 1f))
        }
        container.addView(chart)
        val historyHeading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8.dpFocus(), 0, 12.dpFocus())
        }
        historyHeading.addView(focusText("Recent sessions", 15f, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        if (summary.sessions.size > 3) {
            historyHeading.addView(focusText(if (showAllFocusSessions) "Show less" else "View all", 13f, true).apply {
                setTextColor(Color.rgb(157, 125, 244))
                setPadding(10.dpFocus(), 6.dpFocus(), 0, 6.dpFocus())
                setOnClickListener {
                    showAllFocusSessions = !showAllFocusSessions
                    renderFocusHistory()
                }
            })
        }
        container.addView(historyHeading)
        if (summary.sessions.isEmpty()) {
            container.addView(focusText("No focus sessions yet. Start a session from Task Details.", 13f, muted = true))
        } else {
            val dateFormat = SimpleDateFormat("MMM d • h:mm a", Locale.US)
            val visible = if (showAllFocusSessions) summary.sessions else summary.sessions.take(3)
            visible.forEachIndexed { index, session ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12.dpFocus(), 12.dpFocus(), 12.dpFocus(), 12.dpFocus())
                    background = roundedFocusBackground(focusSurfaceColor())
                }
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                info.addView(focusText(session.taskTitle, 14f, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
                info.addView(focusText(dateFormat.format(java.util.Date(session.startedAt)), 11f, muted = true).apply {
                    setPadding(0, 5.dpFocus(), 0, 0)
                })
                row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.END }
                right.addView(focusText(formatFocus(session.seconds), 13f, true))
                right.addView(focusText(session.status, 11f, muted = true).apply {
                    gravity = Gravity.END
                    setPadding(0, 5.dpFocus(), 0, 0)
                })
                row.addView(right)
                container.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = 8.dpFocus()
                })
            }
        }
    }

    private fun renderWeeklyChart() {
        val week = selectedDate.clone() as java.util.Calendar
        week.firstDayOfWeek = java.util.Calendar.MONDAY
        while (week.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
            week.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        week.set(java.util.Calendar.HOUR_OF_DAY, 0)
        week.set(java.util.Calendar.MINUTE, 0)
        week.set(java.util.Calendar.SECOND, 0)
        week.set(java.util.Calendar.MILLISECOND, 0)
        val all = TaskyDatabase(this).tasks()
        val counts = (0..6).map { day ->
            val start = week.timeInMillis + day * 86_400_000L
            all.count { it.deadline >= start && it.deadline < start + 86_400_000L }
        }
        val chart = findViewById<LinearLayout>(R.id.weeklyChart)
        chart.removeAllViews()
        val maxCount = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
        val dp = resources.displayMetrics.density
        val barMaxPx = (125 * dp).toInt()
        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        counts.forEachIndexed { i, count ->
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            }
            val number = TextView(this).apply {
                text = count.toString()
                textSize = 11f
                setTextColor(if (resources.configuration.uiMode and 48 == 32) Color.WHITE else Color.rgb(37,34,61))
                gravity = Gravity.CENTER
            }
            column.addView(number)
            val bar = View(this).apply {
                setBackgroundColor(if (i == 0) Color.rgb(118,86,219) else Color.rgb(157,125,244))
                layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(),
                    if (count == 0) (2 * dp).toInt() else (barMaxPx * count / maxCount).coerceAtLeast((8 * dp).toInt()))
            }
            column.addView(bar)
            column.addView(TextView(this).apply {
                text = days[i]
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (resources.configuration.uiMode and 48 == 32) Color.WHITE else Color.rgb(37,34,61))
            })
            chart.addView(column)
        }
        val total = counts.sum()
        findViewById<TextView>(R.id.tvWeekNote).text =
            "$total scheduled task${if (total == 1) "" else "s"} this week. Bars show tasks by deadline date."
        findViewById<TextView>(R.id.tvWeekSummary).text = "$total tasks this week"
        val weekEnd = week.timeInMillis + 7 * 86_400_000L
        val weeklyTasks = all.filter { it.deadline >= week.timeInMillis && it.deadline < weekEnd }
        val complete = weeklyTasks.count { it.status == "Completed" }
        findViewById<TextView>(R.id.tvWeekPercent).text =
            "${if (total == 0) 0 else complete * 100 / total}% complete"
    }

    override fun onResume() {
        focusIndicator.start()
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        refreshWorkload()
        focusHandler.removeCallbacks(focusRefresh)
        focusHandler.post(focusRefresh)
    }

    override fun onPause() {
        focusIndicator.stop()
        focusHandler.removeCallbacks(focusRefresh)
        super.onPause()
    }
}
