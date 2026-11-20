package com.example.tasky_add_mngmt

import android.app.DatePickerDialog
import android.content.Intent
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {

    private lateinit var categoryChips: List<Pair<TextView, String>>
    private lateinit var priorityOptions: List<Triple<TextView, String, String>>

    private var selectedCategory = "School"
    private var selectedPriority = "Medium"
    private var selectedDifficulty = "Moderate"
    private var editingId: Long? = null
    private var isSubtasksActive = false
    private lateinit var btnAddSubtasks: TextView
    private lateinit var layoutSubtasksContainer: LinearLayout
    private lateinit var btnAddAnotherSubtask: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBackButton()

        
        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_save_task).setOnClickListener { saveTaskToDatabase() }

        setupCategorySelection()
        setupPrioritySelection()
        setupDifficultySelection()
        setupSubtasksSection()
        setupDateTimePickers()
        setupDurationPickers()
        setupBottomNavigation(NavTab.NONE)
        editingId = intent.getLongExtra("task_id", -1L).takeIf { it > 0 }
        if (editingId != null) loadTask(editingId!!)
    }

    private fun setupCategorySelection() {
        val choiceSchool: TextView? = findViewById(R.id.choice_school)
        val choiceWork: TextView? = findViewById(R.id.choice_work)
        val choicePersonal: TextView? = findViewById(R.id.choice_personal)
        val choiceFamily: TextView? = findViewById(R.id.choice_family)
        val choiceOthers: TextView? = findViewById(R.id.choice_others)

        categoryChips = listOfNotNull(
            choiceSchool?.to("School"),
            choiceWork?.to("Work"),
            choicePersonal?.to("Personal"),
            choiceFamily?.to("Family"),
            choiceOthers?.to("Others")
        )

        categoryChips.forEach { (view, _) ->
            view.setOnClickListener {
                selectCategory(view)
            }
        }
    }

    private fun selectCategory(selectedView: TextView) {
        val colorSelected = Color.parseColor("#7656DB")
        val colorUnselected = Color.parseColor("#89869A")

        categoryChips.forEach { (view, name) ->
            if (view == selectedView) {
                selectedCategory = name
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(colorSelected)
                view.setTypeface(null, Typeface.BOLD)
                view.text = "✓  $name"
            } else {
                view.setBackgroundResource(R.drawable.bg_chip)
                view.setTextColor(colorUnselected)
                view.setTypeface(null, Typeface.NORMAL)
                view.text = name
            }
        }
    }

    private fun setupDifficultySelection() {
        val options = listOf(
            R.id.difficulty_easy to "Easy",
            R.id.difficulty_moderate to "Moderate",
            R.id.difficulty_difficult to "Difficult"
        )
        fun update() {
            options.forEach { (id, value) ->
                findViewById<TextView>(id).apply {
                    setBackgroundResource(if (selectedDifficulty == value) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                    setTextColor(Color.parseColor(if (selectedDifficulty == value) "#7656DB" else "#89869A"))
                    setTypeface(null, if (selectedDifficulty == value) Typeface.BOLD else Typeface.NORMAL)
                }
            }
        }
        options.forEach { (id, value) -> findViewById<TextView>(id).setOnClickListener {
            selectedDifficulty = value
            update()
        } }
        update()
    }

    private fun setupPrioritySelection() {
        val optionLow: TextView? = findViewById(R.id.priority_option_low)
        val optionMedium: TextView? = findViewById(R.id.priority_option_medium)
        val optionHigh: TextView? = findViewById(R.id.priority_option_high)

        priorityOptions = listOfNotNull(
            optionLow?.let { Triple(it, "Low", "⌄") },
            optionMedium?.let { Triple(it, "Medium", "—") },
            optionHigh?.let { Triple(it, "High", "⌃") }
        )

        priorityOptions.forEach { (view, _, _) ->
            view.setOnClickListener {
                selectPriority(view)
            }
        }
    }

    private fun selectPriority(selectedView: TextView) {
        val colorSelected = Color.parseColor("#7656DB")
        val colorUnselected = Color.parseColor("#89869A")

        priorityOptions.forEach { (view, name, _) ->
            if (view == selectedView) {
                selectedPriority = name
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(colorSelected)
                view.setTypeface(null, Typeface.BOLD)
                view.text = name
            } else {
                view.setBackgroundResource(R.drawable.bg_chip)
                view.setTextColor(colorUnselected)
                view.setTypeface(null, Typeface.NORMAL)
                view.text = name
            }
        }
    }

    private fun setupSubtasksSection() {
        btnAddSubtasks = findViewById(R.id.btn_add_subtasks) ?: return
        layoutSubtasksContainer = findViewById(R.id.layout_subtasks_container) ?: return
        btnAddAnotherSubtask = findViewById(R.id.btn_add_another_subtask) ?: return

        btnAddSubtasks.setOnClickListener {
            if (!isSubtasksActive) {
                activateSubtasks()
            } else {
                addSubtaskRow()
            }
        }

        btnAddAnotherSubtask.setOnClickListener {
            addSubtaskRow()
        }
    }

    private fun activateSubtasks() {
        isSubtasksActive = true
        btnAddSubtasks.setBackgroundResource(R.drawable.bg_chip_selected)
        btnAddSubtasks.setTextColor(Color.parseColor("#7656DB"))
        btnAddSubtasks.setTypeface(null, Typeface.BOLD)

        layoutSubtasksContainer.visibility = View.VISIBLE
        btnAddAnotherSubtask.visibility = View.VISIBLE

        if (layoutSubtasksContainer.childCount == 0) {
            addSubtaskRow()
        }
    }

    private fun addSubtaskRow() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(6), 0, 0)
            }
        }

        
        val checkBox = CheckBox(this).apply {
            buttonTintList = ColorStateList.valueOf(Color.parseColor("#7656DB"))
        }

        
        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(40),
                1f
            ).apply {
                setMargins(dpToPx(6), 0, dpToPx(6), 0)
            }
            hint = "Enter subtask..."
            setHintTextColor(Color.parseColor("#89869A"))
            setTextColor(Color.parseColor("#25223D"))
            textSize = 11f
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            isSingleLine = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
        }

        
        val btnDelete = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#89869A"))
            textSize = 12f
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener {
                layoutSubtasksContainer.removeView(row)
                if (layoutSubtasksContainer.childCount == 0) {
                    deactivateSubtasks()
                }
            }
        }

        row.addView(checkBox)
        row.addView(editText)
        row.addView(btnDelete)

        layoutSubtasksContainer.addView(row)
    }

    private fun deactivateSubtasks() {
        isSubtasksActive = false
        btnAddSubtasks.setBackgroundResource(R.drawable.bg_chip)
        btnAddSubtasks.setTextColor(Color.parseColor("#89869A"))
        btnAddSubtasks.setTypeface(null, Typeface.BOLD)

        layoutSubtasksContainer.visibility = View.GONE
        btnAddAnotherSubtask.visibility = View.GONE
    }

    private fun setupDateTimePickers() {
        val inputDueDate: EditText? = findViewById(R.id.input_due_date)
        val btnPickerDueDate: View? = findViewById(R.id.btn_picker_due_date)

        val inputDueTime: EditText? = findViewById(R.id.input_due_time)
        val btnPickerDueTime: View? = findViewById(R.id.btn_picker_due_time)

        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        inputDueDate?.setText(dateFormat.format(today.time))

        
        val openDatePicker = {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selYear, selMonth, selDay ->
                    val selCal = Calendar.getInstance().apply {
                        set(selYear, selMonth, selDay)
                    }
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    inputDueDate?.setText(sdf.format(selCal.time))
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        btnPickerDueDate?.setOnClickListener { openDatePicker() }

        
        val openTimePicker = {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selHour, selMinute ->
                    val isAm = selHour < 12
                    val displayHour = when {
                        selHour == 0 -> 12
                        selHour > 12 -> selHour - 12
                        else -> selHour
                    }
                    val amPm = if (isAm) "AM" else "PM"
                    val formattedTime = String.format(Locale.US, "%d:%02d %s", displayHour, selMinute, amPm)
                    inputDueTime?.setText(formattedTime)
                },
                hour,
                minute,
                false 
            )
            timePickerDialog.show()
        }

        btnPickerDueTime?.setOnClickListener { openTimePicker() }
    }

    private fun setupDurationPickers() {
        val inputHours: EditText? = findViewById(R.id.input_hours)
        val btnHoursUp: View? = findViewById(R.id.btn_hours_up)
        val btnHoursDown: View? = findViewById(R.id.btn_hours_down)

        val inputMinutes: EditText? = findViewById(R.id.input_minutes)
        val btnMinutesUp: View? = findViewById(R.id.btn_minutes_up)
        val btnMinutesDown: View? = findViewById(R.id.btn_minutes_down)

        btnHoursUp?.setOnClickListener {
            val current = inputHours?.text.toString().toIntOrNull() ?: 0
            inputHours?.setText((current + 1).coerceAtMost(24).toString())
        }

        btnHoursDown?.setOnClickListener {
            val current = inputHours?.text.toString().toIntOrNull() ?: 0
            inputHours?.setText((current - 1).coerceAtLeast(0).toString())
        }

        btnMinutesUp?.setOnClickListener {
            val current = inputMinutes?.text.toString().toIntOrNull() ?: 0
            inputMinutes?.setText((current + 1).coerceAtMost(59).toString())
        }

        btnMinutesDown?.setOnClickListener {
            val current = inputMinutes?.text.toString().toIntOrNull() ?: 0
            inputMinutes?.setText((current - 1).coerceAtLeast(0).toString())
        }
    }


    private fun saveTaskToDatabase() {
        val title = findViewById<EditText>(R.id.input_task_title).text.toString().trim()
        val description = findViewById<EditText>(R.id.input_task_desc).text.toString().trim()
        val date = findViewById<EditText>(R.id.input_due_date).text.toString().trim()
        val time = findViewById<EditText>(R.id.input_due_time).text.toString().trim()
        val hours = findViewById<EditText>(R.id.input_hours).text.toString().toIntOrNull() ?: 0
        val minutes = findViewById<EditText>(R.id.input_minutes).text.toString().toIntOrNull() ?: 0
        val duration = hours * 60 + minutes
        if (title.isBlank() || duration <= 0 || date.isBlank() || time.isBlank()) {
            Toast.makeText(this, "Enter a title, deadline, and duration greater than zero", Toast.LENGTH_LONG).show()
            return
        }
        val deadline = try {
            val parser = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).apply { isLenient = false }
            parser.parse("$date $time")?.time ?: error("Invalid deadline")
        } catch (e: Exception) {
            Toast.makeText(this, "Use a valid due date and time", Toast.LENGTH_LONG).show()
            return
        }
        val subtasks = (0 until layoutSubtasksContainer.childCount).mapNotNull { index ->
            val row = layoutSubtasksContainer.getChildAt(index) as? LinearLayout
            row?.let { (0 until it.childCount).map { i -> it.getChildAt(i) }.filterIsInstance<EditText>().firstOrNull()?.text?.toString() }
        }
        try {
            TaskyDatabase(this).saveTask(editingId, title, description, selectedCategory, selectedPriority, deadline, duration, subtasks, selectedDifficulty)
            Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show()
            TaskReminderScheduler.refresh(this)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadTask(id: Long) {
        if (TaskyDatabase(this).task(id) == null) { finish(); return }
        findViewById<TextView>(R.id.tvFormTitle).text = "Edit Task"
        findViewById<TextView>(R.id.btn_save_task).text = "Save Changes"
        TaskyDatabase(this).readableDatabase.use { db ->
            db.rawQuery("SELECT t.title,t.description,c.category_name,t.priority,t.deadline,t.estimated_minutes,COALESCE(t.difficulty,'Moderate') FROM tasks t JOIN categories c ON t.category_id=c.category_id WHERE t.task_id=?", arrayOf(id.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    findViewById<EditText>(R.id.input_task_title).setText(cursor.getString(0))
                    findViewById<EditText>(R.id.input_task_desc).setText(cursor.getString(1) ?: "")
                    val category = cursor.getString(2)
                    categoryChips.firstOrNull { it.second == category }?.let { selectCategory(it.first) }
                    val priority = cursor.getString(3)
                    priorityOptions.firstOrNull { it.second == priority }?.let { selectPriority(it.first) }
                    val deadline = java.util.Date(cursor.getLong(4))
                    findViewById<EditText>(R.id.input_due_date).setText(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(deadline))
                    findViewById<EditText>(R.id.input_due_time).setText(SimpleDateFormat("hh:mm a", Locale.US).format(deadline))
                    selectedDifficulty = cursor.getString(6) ?: "Moderate"
                    setupDifficultySelection()
                    val duration = cursor.getInt(5)
                    findViewById<EditText>(R.id.input_hours).setText((duration / 60).toString())
                    findViewById<EditText>(R.id.input_minutes).setText((duration % 60).toString())
                }
            }
            db.rawQuery("SELECT title FROM subtasks WHERE task_id=? ORDER BY subtask_id", arrayOf(id.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    if (!isSubtasksActive) activateSubtasks() else addSubtaskRow()
                    val row = layoutSubtasksContainer.getChildAt(layoutSubtasksContainer.childCount - 1) as LinearLayout
                    (0 until row.childCount).map { row.getChildAt(it) }.filterIsInstance<EditText>().firstOrNull()?.setText(cursor.getString(0))
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
    }
}
