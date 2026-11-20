package com.example.tasky_add_mngmt

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class TaskManagementActivity : AppCompatActivity() {
    private lateinit var focusIndicator: FocusIndicator
    private lateinit var db: TaskyDatabase
    private lateinit var list: LinearLayout
    private var todayOnly = true
    private var search = ""
    private var categoryFilter = "All"
    private var priorityFilter = "All"
    private var statusFilter = "All"
    private var deadlineFilter = "All dates"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        focusIndicator = FocusIndicator(this)
        setContentView(R.layout.task_management)
        db = TaskyDatabase(this)
        findViewById<View>(R.id.taskCardMain).visibility = View.GONE
        findViewById<View>(R.id.btnAdd).setOnClickListener { startActivity(Intent(this, AddTaskActivity::class.java)) }
        findViewById<View>(R.id.btnToday).setOnClickListener { todayOnly = true; refresh() }
        findViewById<View>(R.id.btnAllTasks).setOnClickListener { todayOnly = false; refresh() }
        setupFilter(R.id.btnCategory, "Category", listOf("All", "School", "Work", "Personal", "Family", "Others")) { categoryFilter = it }
        setupFilter(R.id.btnPriority, "Priority", listOf("All", "Low", "Medium", "High")) { priorityFilter = it }
        setupFilter(R.id.btnStatus, "Status", listOf("All", "Pending", "Completed", "Overdue")) { statusFilter = it }
        setupFilter(R.id.btnDeadline, "Deadline", listOf("All dates", "Overdue", "Today", "Tomorrow", "Next 7 days")) {
            deadlineFilter = it
            if (it != "All dates") todayOnly = false
        }
        val sample = findViewById<View>(R.id.taskCardMain)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        (sample.parent as android.view.ViewGroup).addView(list, (sample.parent as android.view.ViewGroup).indexOfChild(sample) + 1)
        val searchField = findViewById<EditText>(R.id.etSearch)
        searchField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { search = s.toString(); refresh() }
            override fun afterTextChanged(s: Editable?) {}
        })
        setupBottomNavigation(NavTab.TASKS)
    }
    override fun onResume() { super.onResume()
        TaskyDateHeaders.update(window.decorView); refresh() }
    private fun setupFilter(id: Int, title: String, options: List<String>, select: (String) -> Unit) {
        val view = findViewById<View>(id)
        view.setOnClickListener {
            AlertDialog.Builder(this).setTitle(title)
                .setItems(options.toTypedArray()) { _, index ->
                    select(options[index])
                    if (view is TextView) view.text = if (index == 0) "$title ⌄" else "${options[index]} ⌄"
                    TaskReminderScheduler.refresh(this)
                refresh()
                }.show()
        }
    }
    private fun refresh() {
        val tasks = db.tasks()
        val now = System.currentTimeMillis()
        val end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,23);set(Calendar.MINUTE,59);set(Calendar.SECOND,59) }.timeInMillis
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val tomorrowStart = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val dayAfterTomorrow = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.DAY_OF_YEAR, 2)
        }.timeInMillis
        val nextWeek = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.DAY_OF_YEAR, 7)
        }.timeInMillis
        val shown = tasks.filter {
            (!todayOnly || it.deadline <= end) &&
            (search.isBlank() || it.title.contains(search,true) || it.category.contains(search,true)) &&
            (categoryFilter == "All" || it.category.equals(categoryFilter, true)) &&
            (priorityFilter == "All" || it.priority.equals(priorityFilter, true)) &&
            (when (deadlineFilter) {
                "Overdue" -> it.overdue
                "Today" -> it.deadline >= startOfToday && it.deadline < tomorrowStart
                "Tomorrow" -> it.deadline >= tomorrowStart && it.deadline < dayAfterTomorrow
                "Next 7 days" -> it.deadline >= startOfToday && it.deadline < nextWeek
                else -> true
            }) &&
            (statusFilter == "All" || when (statusFilter) {
                "Overdue" -> it.overdue
                "Pending" -> it.status != "Completed" && !it.overdue
                else -> it.status.equals(statusFilter, true)
            })
        }
        val activeColor = android.graphics.Color.parseColor("#7955D9")
        val inactiveColor = ContextCompat.getColor(this, R.color.card_surface)
        findViewById<View>(R.id.btnToday).setBackgroundColor(if (todayOnly) activeColor else inactiveColor)
        findViewById<View>(R.id.btnAllTasks).setBackgroundColor(if (todayOnly) inactiveColor else activeColor)
        (findViewById<View>(R.id.btnToday) as? TextView)?.setTextColor(if (todayOnly) Color.WHITE else ContextCompat.getColor(this, R.color.text_primary))
        (findViewById<View>(R.id.btnAllTasks) as? TextView)?.setTextColor(if (todayOnly) ContextCompat.getColor(this, R.color.text_primary) else Color.WHITE)
        findViewById<TextView>(R.id.tvTotal).text = tasks.size.toString()
        findViewById<TextView>(R.id.tvPending).text = tasks.count { it.status != "Completed" }.toString()
        findViewById<TextView>(R.id.tvCompleted).text = tasks.count { it.status == "Completed" }.toString()
        findViewById<TextView>(R.id.tvOverdue).text = tasks.count { it.overdue }.toString()
        findViewById<TextView>(R.id.tvTaskCount).text = "${shown.size} tasks"
        findViewById<TextView>(R.id.tvDate).text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(now))
        findViewById<View>(R.id.tvEmpty).visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        list.removeAllViews()
        val activeFocusTaskId = db.activeFocusSessionAny()?.taskId
        fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
        val primaryText = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryText = ContextCompat.getColor(this, R.color.text_secondary)
        val surface = ContextCompat.getColor(this, R.color.card_surface)
        fun rounded(color: Int, radius: Int = 18): GradientDrawable = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
        }
        shown.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(14))
                background = rounded(surface)
                elevation = dp(2).toFloat()
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, dp(7), 0, dp(9))
                }
            }
            fun text(value: String, size: Float, color: Int = primaryText, bold: Boolean = false) = TextView(this).apply {
                this.text = value; textSize = size; setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
            }
            val heading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val category = text(task.category, 12f, Color.parseColor("#9F83FF"), true).apply {
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rounded(ContextCompat.getColor(this@TaskManagementActivity, R.color.primary_light), 12)
            }
            heading.addView(category)
            heading.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            heading.addView(text("⚑ ${task.priority} priority  ·  ${task.difficulty}", 12f, secondaryText))
            card.addView(heading)
            card.addView(text(task.title, 17f, primaryText, true).apply {
                setPadding(0, dp(12), 0, dp(8))
            })
            card.addView(text("${SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(Date(task.deadline))}  ·  ${task.estimated} min", 12f, secondaryText))
            val statusText = if (task.overdue) "Overdue" else task.status
            card.addView(text("$statusText  ·  ${task.percent}% progress", 12f, secondaryText).apply {
                setPadding(0, dp(10), 0, dp(5))
            })
            val track = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = task.percent
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#8D6AF0"))
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this@TaskManagementActivity, R.color.primary_light))
                layoutParams = LinearLayout.LayoutParams(-1, dp(6))
            }
            card.addView(track)
            if (task.id == activeFocusTaskId) {
                card.addView(text("● Focus active · Tap to return to session", 12f, Color.parseColor("#9F83FF"), true).apply {
                    setPadding(0, dp(12), 0, 0)
                    setOnClickListener {
                        startActivity(Intent(this@TaskManagementActivity, FocusSessionActivity::class.java).putExtra("task_id", task.id))
                    }
                })
            }
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(14), 0, 0)
            }
            fun action(label: String, prominent: Boolean = false, click: () -> Unit) {
                val button = TextView(this).apply {
                    text = label; textSize = 12f; gravity = android.view.Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(if (prominent) Color.WHITE else primaryText)
                    background = rounded(if (prominent) Color.parseColor("#7955D9")
                        else ContextCompat.getColor(this@TaskManagementActivity, R.color.primary_light), 12)
                    setOnClickListener { click() }
                }
                actions.addView(button, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    marginEnd = dp(7)
                })
            }
            action("Details", true) {
                startActivity(Intent(this, TaskDetailsActivity::class.java).putExtra("task_id", task.id))
            }
            action("Edit") {
                startActivity(Intent(this, AddTaskActivity::class.java).putExtra("task_id", task.id))
            }
            action("More") {
                val choices = arrayOf(if (task.status == "Completed") "Reopen task" else "Mark completed", "Delete task")
                AlertDialog.Builder(this).setTitle(task.title).setItems(choices) { _, which ->
                    if (which == 0) {
                        if (task.status == "Completed") db.reopenTask(task.id) else db.completeTask(task.id)
                        TaskReminderScheduler.refresh(this)
                        refresh()
                    } else {
                        AlertDialog.Builder(this).setMessage("Delete '${task.title}'?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                db.deleteTask(task.id)
                                TaskReminderScheduler.cancel(this, task.id)
                                refresh()
                            }.show()
                    }
                }.show()
            }
            card.addView(actions)
            list.addView(card)
        }

    }
    override fun onPause() {
        focusIndicator.stop()
        super.onPause()
    }

}
