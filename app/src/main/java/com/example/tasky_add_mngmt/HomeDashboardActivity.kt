package com.example.tasky_add_mngmt

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeDashboardActivity : AppCompatActivity() {
    private lateinit var focusIndicator: FocusIndicator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2026)
        }
        enableEdgeToEdge()
        focusIndicator = FocusIndicator(this)
        setContentView(R.layout.home_dashboard)

        
        findViewById<View>(R.id.your_progress_card).setOnClickListener {
            startActivity(Intent(this, TaskProgressActivity::class.java))
        }
        findViewById<View>(R.id.workload_indicator).setOnClickListener {
            startActivity(Intent(this, WorkloadMonitoringActivity::class.java))
        }

        setupBottomNavigation(NavTab.HOME)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        focusIndicator.start()
        TaskyDateHeaders.update(window.decorView)
        val account = AccountStore(this).current()
        val name = account?.name?.trim()?.takeIf { it.isNotEmpty() } ?: "User"
        val hour = LocalTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
        findViewById<TextView>(R.id.hello).text = "$greeting, $name"
        findViewById<TextView>(R.id.current_date).text =
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))
        findViewById<TextView>(R.id.initials).text =
            name.split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("").ifBlank { "U" }
        val db = TaskyDatabase(this)
        val tasks = db.tasks()
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todaysCount = tasks.count { it.deadline >= todayStart && it.deadline < todayStart + 86_400_000L }
        val workload = when { todaysCount <= 3 -> "Light"; todaysCount <= 6 -> "Moderate"; else -> "Heavy" }
        findViewById<TextView>(R.id.moderate_status).text = workload
        
        
        val selectedIndex = when (workload) { "Light" -> 0; "Moderate" -> 1; else -> 2 }
        val workloadCards = intArrayOf(R.id.light_workload_card, R.id.moderate_workload_card, R.id.heavy_workload_card)
        val labels = arrayOf(
            intArrayOf(R.id.light_title, R.id.light_workload_tasks),
            intArrayOf(R.id.moderate_title, R.id.moderate_workload_tasks),
            intArrayOf(R.id.heavy_title, R.id.heavy_workload_tasks)
        )
        val density = resources.displayMetrics.density
        workloadCards.forEachIndexed { index, id ->
            val selected = index == selectedIndex
            val background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(if (selected) Color.parseColor("#E3D8FF") else if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.parseColor("#302B42") else Color.parseColor("#F5F3FA"))
            }
            findViewById<View>(id).background = background
            findViewById<View>(id).alpha = 1f
            labels[index].forEach { labelId ->
                findViewById<TextView>(labelId).setTextColor(
                    if (selected) Color.parseColor("#6540D8") else if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) Color.parseColor("#D4CBE8") else Color.parseColor("#77718B")
                )
            }
        }
        val completed = tasks.count { it.status == "Completed" }
        val percent = if(tasks.isEmpty()) 0 else completed * 100 / tasks.size
        findViewById<TextView>(R.id.count_total).text = tasks.size.toString()
        findViewById<TextView>(R.id.count_pending).text = tasks.count { it.status != "Completed" }.toString()
        findViewById<TextView>(R.id.count_completed).text = completed.toString()
        findViewById<TextView>(R.id.count_overdue).text = tasks.count { it.overdue }.toString()
        findViewById<TextView>(R.id.progress_percentage).text = "$percent%"
        
        val progressTrack = findViewById<View>(R.id.progress_track_bar)
        val progressFill = findViewById<View>(R.id.completed_progress)
        progressTrack.post {
            val width = (progressTrack.width * percent / 100f).toInt()
            progressFill.layoutParams = progressFill.layoutParams.apply { this.width = width }
            progressFill.requestLayout()
        }
        findViewById<TextView>(R.id.today_tasks_count).text = "${unfinishedDueToday(tasks, todayStart)} due today"
        val unfinished = tasks.filter { it.status != "Completed" }
        val urgent = unfinished.count { it.deadline < todayStart + 86_400_000L }
        findViewById<TextView>(R.id.home_focus_summary).text = when {
            urgent == 0 -> "All caught up for today. Plan your next step."
            urgent == 1 -> "1 task needs your attention today."
            else -> "$urgent tasks need your attention today."
        }
        findViewById<TextView>(R.id.home_focus_subtitle).text =
            "${unfinished.size} remaining  •  $completed completed  •  ${tasks.count { it.overdue }} overdue"
        findViewById<View>(R.id.home_focus_action).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        val homeOrder = SmartTaskPriority.comparator()
        val dueSoon = unfinished.filter { it.deadline < todayStart + 86_400_000L }.sortedWith(homeOrder)
        val upcoming = unfinished.filter { it.deadline >= todayStart + 86_400_000L }.sortedWith(homeOrder)
        val cards = intArrayOf(R.id.task_card_1,R.id.task_card_2,R.id.task_card_3,R.id.task_card_4,
            R.id.deadline_card_1,R.id.deadline_card_2,R.id.deadline_card_3)
        val titles = intArrayOf(R.id.pay_electricity_bill,R.id.send_project_proposal,R.id.review_lecture_notes,
            R.id.plan_meals_for_the_week,R.id.deadline_title_1,R.id.deadline_title_2,R.id.deadline_title_3)
        val checks = intArrayOf(R.id.completion_control_1,R.id.completion_control_2,R.id.completion_control_3,
            R.id.completion_control_4,R.id.deadline_control_1,R.id.deadline_control_2,R.id.deadline_control_3)
        val checkIcons = intArrayOf(R.id.completion_check_1,R.id.completion_check_2,R.id.completion_check_3,
            R.id.completion_check_4,R.id.deadline_check_1,R.id.deadline_check_2,R.id.deadline_check_3)
        cards.forEachIndexed { i, cardId ->
            val task = if (i < 4) dueSoon.getOrNull(i) else upcoming.getOrNull(i - 4)
            findViewById<View>(cardId).visibility = if(task == null) View.GONE else View.VISIBLE
            if(task != null) {
                findViewById<TextView>(titles[i]).apply {
                    text = task.title
                    setTextColor(Color.parseColor("#25223D"))
                }
                
                
                val cardView = findViewById<View>(cardId)
                var detailsBound = false
                fun bindLabels(view: View) {
                    if (view is TextView && view.id != titles[i]) {
                        
                        
                        val role = (view.tag as? String) ?: run {
                            val original = view.text.toString()
                            val detected = when {
                                original.contains("Priority Level", true) -> "priority"
                                original.contains("DATE", true) ||
                                    original.contains("Duration Time", true) -> "details"
                                original.equals("PERSONAL", true) ||
                                    original.equals("WORK", true) ||
                                    original.equals("STUDY", true) -> "category"
                                else -> ""
                            }
                            if (detected.isNotEmpty()) view.tag = detected
                            detected
                        }
                        if (role == "details") {
                            view.visibility = if (detailsBound) View.GONE else View.VISIBLE
                            detailsBound = true
                        }
                        when (role) {
                            "priority" -> view.text = "${task.priority} priority · ${task.difficulty} · ${SmartTaskPriority.label(task)}"
                            "details" -> view.text = "${java.text.SimpleDateFormat("MMM d, yyyy", Locale.US).format(java.util.Date(task.deadline))} · ${task.status} · ${task.estimated} min"
                            "category" -> view.text = task.category.uppercase(Locale.US)
                        }
                        view.setTextColor(Color.parseColor("#25223D"))
                    } else if (view is android.view.ViewGroup) {
                        for (j in 0 until view.childCount) bindLabels(view.getChildAt(j))
                    }
                }
                bindLabels(cardView)
                findViewById<View>(cardId).setOnClickListener {
                    startActivity(Intent(this, TaskDetailsActivity::class.java).putExtra("task_id",task.id))
                }
                val isCompleted = task.status == "Completed"
                findViewById<ImageView>(checkIcons[i]).visibility =
                    if (isCompleted) View.VISIBLE else View.GONE
                findViewById<View>(checks[i]).apply {
                    setBackgroundResource(
                        if (isCompleted) R.drawable.bg_rounded_completion_control
                        else R.drawable.bg_checkbox_unselected
                    )
                    backgroundTintList = if (isCompleted)
                        ColorStateList.valueOf(Color.parseColor("#7656DB")) else null
                }
                
                
                val progressRowId = when (i) {
                    1 -> R.id.task_progress_2
                    5 -> R.id.deadline_progress_2
                    else -> null
                }
                if (progressRowId != null) {
                    val actualPercent = task.percent
                    findViewById<View>(progressRowId).visibility =
                        if (actualPercent in 1..99) View.VISIBLE else View.GONE
                    val barId = if (i == 1) R.id.progress_bar_2 else R.id.deadline_progress_bar_2
                    val labelId = if (i == 1) R.id.progress_label_2 else R.id.deadline_progress_label_2
                    findViewById<android.widget.ProgressBar>(barId).progress = actualPercent
                    findViewById<TextView>(labelId).text = "$actualPercent%"
                }
                findViewById<View>(checks[i]).setOnClickListener {
                    if(task.status=="Completed") db.reopenTask(task.id) else db.completeTask(task.id)
                    TaskReminderScheduler.refresh(this)
                    onResume()
                }
            }
        }
    }


    private fun unfinishedDueToday(tasks: List<TaskyDatabase.Task>, todayStart: Long): Int =
        tasks.count { it.status != "Completed" && it.deadline >= todayStart && it.deadline < todayStart + 86_400_000L }

    private fun setupCheckbox(
        control: View,
        checkImageView: ImageView,
        titleTextView: TextView,
        initialChecked: Boolean
    ) {
        var isChecked = initialChecked

        val updateState = { checked: Boolean ->
            isChecked = checked
            if (checked) {
                control.setBackgroundResource(R.drawable.bg_rounded_completion_control)
                control.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7656DB"))
                checkImageView.visibility = View.VISIBLE
                titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                titleTextView.setTextColor(Color.parseColor("#FF7A778F"))
            } else {
                control.setBackgroundResource(R.drawable.bg_checkbox_unselected)
                control.backgroundTintList = null
                checkImageView.visibility = View.GONE
                titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                titleTextView.setTextColor(Color.parseColor("#FF25223D"))
            }
        }

        updateState(initialChecked)

        control.setOnClickListener {
            updateState(!isChecked)
        }
    }
    override fun onPause() {
        focusIndicator.stop()
        super.onPause()
    }

}
