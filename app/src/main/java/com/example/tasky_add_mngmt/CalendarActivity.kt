package com.example.tasky_add_mngmt

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {
    private val focusIndicator by lazy { FocusIndicator(this) }


    private lateinit var btnMonth: TextView
    private lateinit var btnWeek: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvMonthLabel: TextView
    private lateinit var tvSelectedDay: TextView
    private lateinit var badgeToday: TextView
    private lateinit var monthContainer: View
    private lateinit var weekContainer: View
    private lateinit var footnoteContainer: View
    private lateinit var laterThisWeekContainer: View
    private lateinit var rvMonth: RecyclerView
    private lateinit var plannerContainer: LinearLayout
    private val plannerPrefs by lazy { getSharedPreferences("tasky_schedule_planner_${AccountStore(this).currentId}", MODE_PRIVATE) }

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        val mainView = findViewById<View>(R.id.main)
        val bottomNav = findViewById<View>(R.id.bottom_navigation)

        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                mainView.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                bottomNav.setPadding(
                    bottomNav.paddingLeft,
                    bottomNav.paddingTop,
                    bottomNav.paddingRight,
                    systemBars.bottom
                )
                insets
            }
        }

        btnMonth = findViewById(R.id.btnMonth)
        btnWeek = findViewById(R.id.btnWeek)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvMonthLabel = findViewById(R.id.tvMonthLabel)
        tvSelectedDay = findViewById(R.id.tvSelectedDay)
        badgeToday = findViewById(R.id.badgeToday)
        monthContainer = findViewById(R.id.monthContainer)
        weekContainer = findViewById(R.id.weekContainer)
        footnoteContainer = findViewById(R.id.footnoteContainer)
        laterThisWeekContainer = findViewById(R.id.laterThisWeekContainer)
        rvMonth = findViewById(R.id.rvMonth)
        plannerContainer = findViewById(R.id.plannerContainer)

        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnToday = findViewById<TextView>(R.id.btnToday)

        btnMonth.setOnClickListener { selectTab(isMonth = true) }
        btnWeek.setOnClickListener { selectTab(isMonth = false) }

        tvMonthLabel.setOnClickListener { showMonthPicker() }

        btnPrev.setOnClickListener {
            if (currentMonth == Calendar.JANUARY) {
                currentMonth = Calendar.DECEMBER
                currentYear--
            } else {
                currentMonth--
            }
            selectedDay = 1
            updateMonthView()
        }

        btnNext.setOnClickListener {
            if (currentMonth == Calendar.DECEMBER) {
                currentMonth = Calendar.JANUARY
                currentYear++
            } else {
                currentMonth++
            }
            selectedDay = 1
            updateMonthView()
        }

        btnToday.setOnClickListener {
            currentYear = Calendar.getInstance().get(Calendar.YEAR)
            currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            updateMonthView()
        }

        rvMonth.layoutManager = GridLayoutManager(this, 7)

        updateMonthView()

        setupBottomNavigation(NavTab.CALENDAR)
    }

    override fun onResume() {
        focusIndicator.start()
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        if (::rvMonth.isInitialized) updateMonthView()
    }

    private fun showMonthPicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null)
        var pickerYear = currentYear

        val tvPickerYear = dialogView.findViewById<TextView>(R.id.tvPickerYear)
        val btnPrevYear = dialogView.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = dialogView.findViewById<ImageButton>(R.id.btnNextYear)

        tvPickerYear.text = pickerYear.toString()

        val monthButtons = listOf(
            dialogView.findViewById<TextView>(R.id.btnMonthJan),
            dialogView.findViewById<TextView>(R.id.btnMonthFeb),
            dialogView.findViewById<TextView>(R.id.btnMonthMar),
            dialogView.findViewById<TextView>(R.id.btnMonthApr),
            dialogView.findViewById<TextView>(R.id.btnMonthMay),
            dialogView.findViewById<TextView>(R.id.btnMonthJun),
            dialogView.findViewById<TextView>(R.id.btnMonthJul),
            dialogView.findViewById<TextView>(R.id.btnMonthAug),
            dialogView.findViewById<TextView>(R.id.btnMonthSep),
            dialogView.findViewById<TextView>(R.id.btnMonthOct),
            dialogView.findViewById<TextView>(R.id.btnMonthNov),
            dialogView.findViewById<TextView>(R.id.btnMonthDec)
        )

        fun updateMonthButtonsState() {
            monthButtons.forEachIndexed { index, tvMonth ->
                if (pickerYear == currentYear && index == currentMonth) {
                    tvMonth.background = ContextCompat.getDrawable(this, R.drawable.bg_week_chip_selected)
                    tvMonth.setTextColor(Color.WHITE)
                } else {
                    tvMonth.background = ContextCompat.getDrawable(this, R.drawable.bg_week_chip_normal)
                    tvMonth.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }

        updateMonthButtonsState()

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        btnPrevYear.setOnClickListener {
            pickerYear--
            tvPickerYear.text = pickerYear.toString()
            updateMonthButtonsState()
        }

        btnNextYear.setOnClickListener {
            pickerYear++
            tvPickerYear.text = pickerYear.toString()
            updateMonthButtonsState()
        }

        monthButtons.forEachIndexed { index, tvMonth ->
            tvMonth.setOnClickListener {
                currentYear = pickerYear
                currentMonth = index
                selectedDay = 1
                updateMonthView()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateMonthView() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        tvMonthLabel.text = monthFormat.format(cal.time)

        cal.set(Calendar.DAY_OF_MONTH, selectedDay)
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH)
        tvSelectedDay.text = dateFormat.format(cal.time)

        if (currentYear == Calendar.getInstance().get(Calendar.YEAR) && currentMonth == Calendar.getInstance().get(Calendar.MONTH) && selectedDay == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
            badgeToday.visibility = View.VISIBLE
        } else {
            badgeToday.visibility = View.GONE
        }

        val days = getDaysForMonth(currentYear, currentMonth, selectedDay)
        setupScheduleRecyclerView()
        setupWeekRecyclerView()
        updateSchedulePlanner()
        rvMonth.adapter = MonthAdapter(days) { clickedDay ->
            if (clickedDay.isCurrentMonth) {
                selectedDay = clickedDay.dayNumber
                updateMonthView()
            } else {
                if (clickedDay.month < currentMonth || (currentMonth == 0 && clickedDay.month == 11)) {
                    if (currentMonth == Calendar.JANUARY) {
                        currentMonth = Calendar.DECEMBER
                        currentYear--
                    } else {
                        currentMonth--
                    }
                } else {
                    if (currentMonth == Calendar.DECEMBER) {
                        currentMonth = Calendar.JANUARY
                        currentYear++
                    } else {
                        currentMonth++
                    }
                }
                selectedDay = clickedDay.dayNumber
                updateMonthView()
            }
        }
    }

    private fun getDaysForMonth(year: Int, month: Int, selectedDayOfMonth: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) 
        val daysBefore = (firstDayOfWeek + 5) % 7 

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        
        val prevCal = cal.clone() as Calendar
        prevCal.add(Calendar.MONTH, -1)
        val prevDaysInMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startPrevDay = prevDaysInMonth - daysBefore + 1
        val prevMonth = prevCal.get(Calendar.MONTH)
        val prevYear = prevCal.get(Calendar.YEAR)

        for (d in startPrevDay..prevDaysInMonth) {
            days.add(
                CalendarDay(
                    dayNumber = d,
                    isCurrentMonth = false,
                    isSelected = false,
                    month = prevMonth,
                    year = prevYear,
                    hasWork = false,
                    hasSchool = false,
                    hasPersonal = false
                )
            )
        }

        
        for (d in 1..daysInMonth) {
            val isSel = (selectedDayOfMonth == d)
            val dayTasks = TaskyDatabase(this).tasks().filter { task -> val c = Calendar.getInstance().apply { timeInMillis=task.deadline }; c.get(Calendar.YEAR)==year && c.get(Calendar.MONTH)==month && c.get(Calendar.DAY_OF_MONTH)==d }
            val hasWork = dayTasks.any { it.category == "Work" }
            val hasSchool = dayTasks.any { it.category == "School" }
            val hasPersonal = dayTasks.any { it.category == "Personal" }

            days.add(
                CalendarDay(
                    dayNumber = d,
                    isCurrentMonth = true,
                    isSelected = isSel,
                    month = month,
                    year = year,
                    hasWork = hasWork,
                    hasSchool = hasSchool,
                    hasPersonal = hasPersonal
                )
            )
        }

        
        val nextCal = cal.clone() as Calendar
        nextCal.add(Calendar.MONTH, 1)
        val nextMonth = nextCal.get(Calendar.MONTH)
        val nextYear = nextCal.get(Calendar.YEAR)
        val totalSoFar = days.size
        val targetSize = if (totalSoFar <= 35) 35 else 42
        val remaining = targetSize - totalSoFar

        for (d in 1..remaining) {
            days.add(
                CalendarDay(
                    dayNumber = d,
                    isCurrentMonth = false,
                    isSelected = false,
                    month = nextMonth,
                    year = nextYear,
                    hasWork = false,
                    hasSchool = false,
                    hasPersonal = false
                )
            )
        }

        return days
    }

    private fun selectTab(isMonth: Boolean) {
        if (isMonth) {
            btnMonth.background = ContextCompat.getDrawable(this, R.drawable.bg_toggle_selected)
            btnMonth.setTextColor(Color.WHITE)
            btnWeek.background = ContextCompat.getDrawable(this, R.drawable.bg_toggle_unselected)
            btnWeek.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            tvSubtitle.text = "See the big picture. Find your focus for today."
            monthContainer.visibility = View.VISIBLE
            weekContainer.visibility = View.GONE
            footnoteContainer.visibility = View.GONE
            laterThisWeekContainer.visibility = View.GONE
        } else {
            btnWeek.background = ContextCompat.getDrawable(this, R.drawable.bg_toggle_selected)
            btnWeek.setTextColor(Color.WHITE)
            btnMonth.background = ContextCompat.getDrawable(this, R.drawable.bg_toggle_unselected)
            btnMonth.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            tvSubtitle.text = "A little structure for a smoother week."
            monthContainer.visibility = View.GONE
            weekContainer.visibility = View.VISIBLE
            footnoteContainer.visibility = View.VISIBLE
            laterThisWeekContainer.visibility = View.VISIBLE
        }
    }

    private fun setupWeekRecyclerView() {
        val rvWeek = findViewById<RecyclerView>(R.id.rvWeek)
        rvWeek.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val start = Calendar.getInstance().apply { set(currentYear,currentMonth,selectedDay); firstDayOfWeek=Calendar.MONDAY; set(Calendar.DAY_OF_WEEK,Calendar.MONDAY) }
        val allTasks = TaskyDatabase(this).tasks()
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val label = SimpleDateFormat("MMM d", Locale.US)
        findViewById<TextView>(R.id.tvWeekRange).text = "${label.format(start.time)} – ${label.format(end.time)}, ${end.get(Calendar.YEAR)}"
        fun inWeek(task: TaskyDatabase.Task): Boolean = task.deadline >= start.timeInMillis && task.deadline < (end.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val weekly = allTasks.filter(::inWeek)
        findViewById<TextView>(R.id.tvWeekTasks).text = "${weekly.size} tasks this week"
        findViewById<TextView>(R.id.tvWeekCompleted).text = "${weekly.count { it.status == "Completed" }} completed"
        updateLaterThisWeek(start, allTasks)
        val weekDays = (0..6).map { offset ->
            val c = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH,offset) }
            WeekDay(SimpleDateFormat("EEE",Locale.US).format(c.time), c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.YEAR)==currentYear && c.get(Calendar.MONTH)==currentMonth && c.get(Calendar.DAY_OF_MONTH)==selectedDay,
                allTasks.any { task -> val t=Calendar.getInstance().apply { timeInMillis=task.deadline }; t.get(Calendar.YEAR)==c.get(Calendar.YEAR) && t.get(Calendar.DAY_OF_YEAR)==c.get(Calendar.DAY_OF_YEAR) })
        }
        rvWeek.adapter = WeekAdapter(weekDays) { index ->
            val clicked = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            currentYear = clicked.get(Calendar.YEAR)
            currentMonth = clicked.get(Calendar.MONTH)
            selectedDay = clicked.get(Calendar.DAY_OF_MONTH)
            updateMonthView()
        }
    }

    private fun updateLaterThisWeek(start: Calendar, allTasks: List<TaskyDatabase.Task>) {
        val container = laterThisWeekContainer as LinearLayout
        container.removeAllViews()
        val title = TextView(this).apply {
            text = "Later this week"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@CalendarActivity, R.color.text_primary))
        }
        container.addView(title)
        val selected = Calendar.getInstance().apply { set(currentYear, currentMonth, selectedDay, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        val days = (1..6).map { offset -> (selected.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) } }
            .filter { it.timeInMillis < end.timeInMillis }
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        var shown = 0
        for (day in days) {
            val dayTasks = allTasks.filter { task ->
                val c = Calendar.getInstance().apply { timeInMillis = task.deadline }
                c.get(Calendar.YEAR) == day.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
            }
            if (dayTasks.isEmpty()) continue
            shown++
            val minutes = dayTasks.sumOf { it.estimated }
            val line = TextView(this).apply {
                text = "${fmt.format(day.time)}  •  ${dayTasks.size} task(s)  •  ${minutes / 60}h ${minutes % 60}m\n${dayTasks.take(3).joinToString(", ") { it.title }}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@CalendarActivity, R.color.text_primary))
                setBackgroundResource(R.drawable.bg_later_card)
                setPadding(18, 14, 18, 14)
                val params = LinearLayout.LayoutParams(-1, -2)
                params.topMargin = 12
                layoutParams = params
                setOnClickListener {
                    currentYear = day.get(Calendar.YEAR)
                    currentMonth = day.get(Calendar.MONTH)
                    selectedDay = day.get(Calendar.DAY_OF_MONTH)
                    updateMonthView()
                }
            }
            container.addView(line)
        }
        if (shown == 0) container.addView(TextView(this).apply {
            text = "No other deadlines this week"
            setTextColor(ContextCompat.getColor(this@CalendarActivity, R.color.text_secondary))
            setPadding(0, 14, 0, 0)
        })
    }

    

    private fun updateSchedulePlanner() {
        val container = plannerContainer
        container.removeAllViews()
        val budget = plannerPrefs.getInt("daily_budget_minutes", 180)
        val availableStart = plannerPrefs.getInt("available_start_minutes", 9 * 60)
        val availableEnd = plannerPrefs.getInt("available_end_minutes", 17 * 60)
        val windowMinutes = availableEnd - availableStart
        val effectiveBudget = minOf(budget, windowMinutes)
        fun clock(minutes: Int): String = SimpleDateFormat("h:mm a", Locale.US).format(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
            }.time
        )
        val density = resources.displayMetrics.density
        fun dp(n: Int) = (n * density).toInt()
        fun label(value: String, size: Float = 13f, bold: Boolean = false, secondary: Boolean = false): TextView =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(ContextCompat.getColor(this@CalendarActivity,
                    if (secondary) R.color.text_secondary else R.color.text_primary))
                if (bold) setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(5), 0, dp(5))
            }

        val primary = ContextCompat.getColor(this, R.color.primary)
        val surface = ContextCompat.getColor(this, R.color.surface)
        val primaryText = ContextCompat.getColor(this, R.color.text_primary)
        val secondaryText = ContextCompat.getColor(this, R.color.text_secondary)
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val tileColor = if (isDark) Color.parseColor("#363047") else Color.parseColor("#F1EDFB")
        val cardColor = if (isDark) Color.parseColor("#302B3D") else Color.parseColor("#F8F6FD")
        fun rounded(color: Int, radius: Int = 14): GradientDrawable = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
        fun gap(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
        fun summaryCard(caption: String, amount: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(tileColor)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(label(caption, 11f, secondary = true))
            addView(label(amount, 18f, true))
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        heading.addView(label("Suggested work plan", 19f, true), LinearLayout.LayoutParams(0, -2, 1f))
        val change = label("Edit time", 12f, true).apply {
            setTextColor(primary)
            setOnClickListener {
                MaterialAlertDialogBuilder(this@CalendarActivity)
                    .setTitle("Your daily availability")
                    .setItems(arrayOf(
                        "Work budget: ${budget / 60}h ${budget % 60}m",
                        "Available from: ${clock(availableStart)}",
                        "Available until: ${clock(availableEnd)}"
                    )) { _, choice ->
                        when (choice) {
                            0 -> {
                                val options = arrayOf("1 hour", "2 hours", "3 hours", "4 hours", "6 hours", "8 hours")
                                val values = intArrayOf(60, 120, 180, 240, 360, 480)
                                MaterialAlertDialogBuilder(this@CalendarActivity)
                                    .setTitle("Daily work budget")
                                    .setSingleChoiceItems(options, values.indexOf(budget).coerceAtLeast(0)) { dialog, which ->
                                        plannerPrefs.edit().putInt("daily_budget_minutes", values[which]).apply()
                                        dialog.dismiss()
                                        updateSchedulePlanner()
                                    }.setNegativeButton("Cancel", null).show()
                            }
                            1, 2 -> {
                                val current = if (choice == 1) availableStart else availableEnd
                                android.app.TimePickerDialog(this@CalendarActivity, { _, hour, minute ->
                                    val picked = hour * 60 + minute
                                    val valid = if (choice == 1) picked < availableEnd else picked > availableStart
                                    if (valid) {
                                        plannerPrefs.edit().putInt(
                                            if (choice == 1) "available_start_minutes" else "available_end_minutes", picked
                                        ).apply()
                                        updateSchedulePlanner()
                                    } else {
                                        android.widget.Toast.makeText(this@CalendarActivity,
                                            "Start time must be earlier than end time.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }, current / 60, current % 60, false).show()
                            }
                        }
                    }.setNegativeButton("Cancel", null).show()
            }
        }
        heading.addView(change)
        container.addView(heading)
        container.addView(label("Available ${clock(availableStart)}–${clock(availableEnd)} daily · ${effectiveBudget / 60}h ${effectiveBudget % 60}m usable per day", 12f, secondary = true))
        container.addView(label("Set your free hours using Edit time. TASKY schedules only within this window.", 11f, secondary = true))
        container.addView(gap(8))
        
        
        val now = System.currentTimeMillis()
        val selected = Calendar.getInstance().apply {
            set(currentYear, currentMonth, selectedDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val planningStart = maxOf(selected.timeInMillis, now)
        val horizon = Calendar.getInstance().apply {
            timeInMillis = planningStart
            add(Calendar.DAY_OF_YEAR, 7)
        }.timeInMillis
        val tasks = TaskyDatabase(this).tasks()
            .filter { it.status != "Completed" && it.remaining > 0 && it.deadline < horizon }
            .sortedWith(SmartTaskPriority.comparator(now))
        if (tasks.isEmpty()) {
            container.addView(label("No unfinished deadlines in the next seven days.", secondary = true))
            return
        }
        
        
        
        data class DeadlineWindow(val title: String, val start: Long, val end: Long)
        val windows = tasks.filter { it.deadline > planningStart }.map { task ->
            DeadlineWindow(task.title,
                maxOf(planningStart, task.deadline - task.remaining.toLong() * 60_000L),
                task.deadline)
        }.filter { it.start < it.end }
        val overlappingPairs = mutableListOf<String>()
        for (i in windows.indices) {
            for (j in i + 1 until windows.size) {
                val first = windows[i]
                val second = windows[j]
                if (first.start < second.end && second.start < first.end) {
                    overlappingPairs.add("${first.title} and ${second.title}")
                }
            }
        }
        if (overlappingPairs.isNotEmpty()) {
            val overlapWarning = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(if (isDark) Color.parseColor("#45322F") else Color.parseColor("#FFF1E6"))
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            overlapWarning.addView(label("⚠ Potential deadline pressure", 13f, true))
            overlapWarning.addView(label("These tasks have overlapping estimated last-minute work windows:", 12f))
            overlappingPairs.take(5).forEach { pair ->
                overlapWarning.addView(label("• $pair", 12f))
            }
            if (overlappingPairs.size > 5) {
                overlapWarning.addView(label("And ${overlappingPairs.size - 5} more overlaps.", 12f))
            }
            overlapWarning.addView(label("This is an estimate based on remaining minutes before each deadline, not a confirmed schedule conflict. Starting earlier may resolve it.", 11f, secondary = true))
            container.addView(overlapWarning)
            container.addView(gap(10))
        }
        val totalMinutes = tasks.sumOf { it.remaining }
        val summary = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        summary.addView(summaryCard("Usable daily time", "${effectiveBudget / 60}h ${String.format(Locale.US, "%02d", effectiveBudget % 60)}m"),
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        summary.addView(summaryCard("Work remaining", "${totalMinutes / 60}h ${String.format(Locale.US, "%02d", totalMinutes % 60)}m"),
            LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        container.addView(summary)
        container.addView(gap(12))
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        data class Slot(val start: Long, val end: Long, val task: TaskyDatabase.Task)
        val slots = mutableListOf<Slot>()
        val unscheduled = mutableListOf<String>()
        val usedByDay = mutableMapOf<Long, Int>()
        val cursor = Calendar.getInstance().apply {
            timeInMillis = planningStart
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val lastDay = Calendar.getInstance().apply { timeInMillis = horizon }
        val days = mutableListOf<Long>()
        while (cursor.timeInMillis <= lastDay.timeInMillis) {
            days.add(cursor.timeInMillis)
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        for (task in tasks) {
            var left = task.remaining
            if (task.deadline <= planningStart) {
                unscheduled.add("${task.title}: overdue · $left min not scheduled")
                continue
            }
            for (day in days) {
                if (left <= 0) break
                val startOfDay = Calendar.getInstance().apply { timeInMillis = day }
                val windowStart = (startOfDay.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, availableStart / 60)
                    set(Calendar.MINUTE, availableStart % 60)
                }.timeInMillis
                val windowEnd = (startOfDay.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, availableEnd / 60)
                    set(Calendar.MINUTE, availableEnd % 60)
                }.timeInMillis
                val used = usedByDay[day] ?: 0
                val available = effectiveBudget - used
                if (available <= 0) continue
                val start = maxOf(windowStart + used * 60_000L, planningStart)
                val cutoff = minOf(task.deadline, horizon, windowEnd)
                val possible = ((cutoff - start) / 60_000L).coerceAtLeast(0L).toInt()
                val chunk = minOf(left, available, possible)
                if (chunk <= 0) continue
                slots.add(Slot(start, start + chunk * 60_000L, task))
                usedByDay[day] = used + chunk
                left -= chunk
            }
            if (left > 0) unscheduled.add("${task.title}: $left min cannot fit before the deadline")
        }
        if (slots.isEmpty()) {
            container.addView(label("No work blocks fit before the upcoming deadlines with this budget.", 12f, secondary = true))
        } else {
            val sessionsHeading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            sessionsHeading.addView(label("Your suggested sessions", 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
            sessionsHeading.addView(label("${slots.size} blocks", 12f).apply { setTextColor(primary) })
            container.addView(sessionsHeading)
            slots.sortedBy { it.start }.forEachIndexed { index, slot ->
                val minutes = ((slot.end - slot.start) / 60_000L).toInt()
                val entry = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = rounded(cardColor)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                }
                val top = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                val number = label(String.format(Locale.US, "%02d", index + 1), 12f, true).apply {
                    setTextColor(primary)
                    background = rounded(tileColor, 8)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                }
                top.addView(number)
                top.addView(label("  ${dateFormat.format(java.util.Date(slot.start))}", 13f, true),
                    LinearLayout.LayoutParams(0, -2, 1f))
                val durationText = if (minutes >= 60) "${minutes / 60}h ${if (minutes % 60 > 0) "${minutes % 60}m" else ""}".trim() else "${minutes}m"
                top.addView(label(durationText, 12f, true).apply { setTextColor(primary) })
                entry.addView(top)
                entry.addView(label("◷  ${timeFormat.format(java.util.Date(slot.start))} – ${timeFormat.format(java.util.Date(slot.end))}", 12f, secondary = true))
                entry.addView(label(slot.task.title, 14f, true))
                entry.addView(label("${slot.task.category}  ·  ${slot.task.priority} priority  ·  ${slot.task.difficulty} difficulty", 11f, secondary = true))
                container.addView(entry, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            }
        }
        if (unscheduled.isNotEmpty()) {
            container.addView(gap(12))
            val warning = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(if (isDark) Color.parseColor("#45322F") else Color.parseColor("#FFF1E6"))
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            warning.addView(label("⚠ Needs attention", 13f, true))
            unscheduled.forEach { warning.addView(label(it, 12f)) }
            warning.addView(label("Consider changing your available hours, work budget, or task deadline.", 11f, secondary = true))
            container.addView(warning)
        }
        container.addView(gap(10))
        container.addView(label("Suggestions only. Uses your saved daily availability window and work budget; these are not calendar reservations. TASKY does not change deadlines. Check personal commitments before following the plan.", 11f, secondary = true))
    }

    private fun setupScheduleRecyclerView() {
        val rvSchedule = findViewById<RecyclerView>(R.id.rvSchedule)
        rvSchedule.layoutManager = LinearLayoutManager(this)

        val selected = Calendar.getInstance().apply { set(currentYear,currentMonth,selectedDay) }
        val tasks = TaskyDatabase(this).tasks().filter { task ->
            val date = Calendar.getInstance().apply { timeInMillis=task.deadline }
            date.get(Calendar.YEAR)==currentYear && date.get(Calendar.MONTH)==currentMonth && date.get(Calendar.DAY_OF_MONTH)==selectedDay
        }.map { task ->
            ScheduleTask("", java.text.SimpleDateFormat("h:mm a",Locale.US).format(java.util.Date(task.deadline)),
                task.title,"${task.category} · ${task.estimated} min · ${task.status}",
                R.drawable.bg_card_purple,R.color.card_purple_title,task.status=="Completed")
        }
        val nowMillis = System.currentTimeMillis()
        val dueSoonCount = TaskyDatabase(this).tasks().count {
            it.status != "Completed" && it.deadline >= nowMillis &&
                it.deadline <= nowMillis + 7L * 24 * 60 * 60 * 1000
        }
        findViewById<TextView>(R.id.tvDueSoon).text = "$dueSoonCount due soon"
        findViewById<TextView>(R.id.tvDueToday).text = "${tasks.size} due"
        findViewById<TextView>(R.id.tvDaySummary).text = "${tasks.size} tasks"
        findViewById<View>(R.id.cardUpcoming).visibility = View.GONE
        findViewById<TextView>(R.id.tvUpcomingNote).text = ""
        rvSchedule.adapter = ScheduleAdapter(tasks)
    }

    private fun open(target: Class<*>) {
        openTab(target)
    }

    override fun onPause() {
        focusIndicator.stop()
        super.onPause()
    }
}


data class CalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val month: Int,
    val year: Int,
    val hasWork: Boolean = false,
    val hasSchool: Boolean = false,
    val hasPersonal: Boolean = false
)

class MonthAdapter(
    private val days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<MonthAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvDay: TextView = v.findViewById(R.id.tvDay)
        val dotWork: ImageView = v.findViewById(R.id.dotWork)
        val dotSchool: ImageView = v.findViewById(R.id.dotSchool)
        val dotPersonal: ImageView = v.findViewById(R.id.dotPersonal)
        val dotWhite: ImageView = v.findViewById(R.id.dotWhite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day.dayNumber.toString()

        if (day.isSelected) {
            holder.tvDay.background = ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_day_selected)
            holder.tvDay.setTextColor(Color.WHITE)
            holder.dotWork.visibility = View.GONE
            holder.dotSchool.visibility = View.GONE
            holder.dotPersonal.visibility = View.GONE
            holder.dotWhite.visibility = View.VISIBLE
        } else {
            holder.tvDay.background = null
            if (day.isCurrentMonth) {
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            } else {
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
            }
            holder.dotWork.visibility = if (day.hasWork) View.VISIBLE else View.GONE
            holder.dotSchool.visibility = if (day.hasSchool) View.VISIBLE else View.GONE
            holder.dotPersonal.visibility = if (day.hasPersonal) View.VISIBLE else View.GONE
            holder.dotWhite.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onDayClick(day)
        }
    }

    override fun getItemCount() = days.size
}

data class WeekDay(val weekday: String, val dayNumber: Int, val isSelected: Boolean, val hasTasks: Boolean)

class WeekAdapter(private val days: List<WeekDay>, private val onDayClick: (Int) -> Unit) : RecyclerView.Adapter<WeekAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val weekChip: LinearLayout = v.findViewById(R.id.weekChip)
        val tvWeekday: TextView = v.findViewById(R.id.tvWeekday)
        val tvDayNumber: TextView = v.findViewById(R.id.tvDayNumber)
        val dotHasTasks: ImageView = v.findViewById(R.id.dotHasTasks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_week_day, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = days[position]
        val context = holder.itemView.context
        holder.tvWeekday.text = item.weekday
        holder.tvDayNumber.text = item.dayNumber.toString()
        holder.itemView.setOnClickListener { onDayClick(position) }

        if (item.isSelected) {
            holder.weekChip.background = ContextCompat.getDrawable(context, R.drawable.bg_week_chip_selected)
            holder.tvWeekday.setTextColor(Color.WHITE)
            holder.tvDayNumber.setTextColor(Color.WHITE)
            holder.dotHasTasks.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dot_white))
            holder.dotHasTasks.visibility = View.VISIBLE
        } else {
            holder.weekChip.background = ContextCompat.getDrawable(context, R.drawable.bg_week_chip_normal)
            holder.tvWeekday.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.dotHasTasks.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dot_work))
            holder.dotHasTasks.visibility = if (item.hasTasks) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount() = days.size
}

data class ScheduleTask(
    val startTime: String,
    val endTime: String,
    val title: String,
    val meta: String,
    val bgDrawableRes: Int,
    val textColorRes: Int,
    val isCompleted: Boolean
)

class ScheduleAdapter(private val tasks: List<ScheduleTask>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvStartTime: TextView = v.findViewById(R.id.tvStartTime)
        val tvEndTime: TextView = v.findViewById(R.id.tvEndTime)
        val cardTask: View = v.findViewById(R.id.cardTask)
        val tvTaskTitle: TextView = v.findViewById(R.id.tvTaskTitle)
        val tvTaskMeta: TextView = v.findViewById(R.id.tvTaskMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_task, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        val context = holder.itemView.context
        holder.tvStartTime.text = task.startTime
        holder.tvEndTime.text = task.endTime
        holder.tvTaskTitle.text = task.title
        holder.tvTaskMeta.text = task.meta

        holder.cardTask.background = ContextCompat.getDrawable(context, task.bgDrawableRes)
        val textColor = ContextCompat.getColor(context, task.textColorRes)
        holder.tvTaskTitle.setTextColor(textColor)
        holder.tvTaskMeta.setTextColor(textColor)

        if (task.isCompleted) {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
    override fun getItemCount() = tasks.size
}
