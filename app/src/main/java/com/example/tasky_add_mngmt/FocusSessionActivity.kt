package com.example.tasky_add_mngmt

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale


class FocusSessionActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var sessionId: Long? = null
    private var onBreak = false
    private var taskId = -1L
    private lateinit var database: TaskyDatabase
    private lateinit var timer: TextView
    private lateinit var progress: ProgressBar
    private lateinit var details: TextView
    private lateinit var breakDetails: TextView
    private lateinit var focusButton: Button
    private lateinit var breakButton: Button
    private lateinit var distractionButton: Button
    private val purple = Color.rgb(119, 82, 219)
    private val ink = Color.rgb(42, 35, 65)
    private val muted = Color.rgb(119, 111, 139)
    private val bg = Color.rgb(249, 247, 255)
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.dp().toFloat()
    }
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun button(value: String, color: Int, textColor: Int = Color.WHITE) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 14f
        setTypeface(null, Typeface.BOLD); setTextColor(textColor)
        backgroundTintList = ColorStateList.valueOf(color)
    }
    private val tick = object : Runnable {
        override fun run() {
            updateDisplay()
            if (sessionId != null) handler.postDelayed(this, 1000L)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = TaskyDatabase(this)
        taskId = intent.getLongExtra("task_id", -1L)
        val task = database.task(taskId) ?: run { finish(); return }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22.dp(), 30.dp(), 22.dp(), 22.dp())
        }
        body.addView(text("TASKY  /  FOCUS", 12f, purple, true))
        body.addView(text("Focus Session", 27f, ink, true).apply { setPadding(0, 8.dp(), 0, 4.dp()) })
        body.addView(text("One step at a time. You've got this.", 13f, muted))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20.dp(), 24.dp(), 20.dp(), 22.dp()); background = rounded(Color.WHITE)
        }
        body.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 22.dp() })
        card.addView(text(task.title, 18f, ink, true).apply { gravity = Gravity.CENTER })
        card.addView(text("FOCUS TIME", 11f, purple, true).apply { setPadding(0, 24.dp(), 0, 0) })
        timer = text("00:00", 52f, purple, true).apply { gravity = Gravity.CENTER }
        card.addView(timer)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = ColorStateList.valueOf(purple)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(232, 226, 246))
        }
        card.addView(progress, LinearLayout.LayoutParams(-1, 8.dp()).apply { topMargin = 15.dp() })
        details = text("", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(0, 14.dp(), 0, 4.dp()) }
        card.addView(details)
        breakDetails = text("", 12f, muted).apply { gravity = Gravity.CENTER }
        card.addView(breakDetails)
        focusButton = button("Pause Focus", purple)
        body.addView(focusButton, LinearLayout.LayoutParams(-1, 52.dp()).apply { topMargin = 20.dp() })
        breakButton = button("Take a Break", Color.rgb(231, 222, 251), purple)
        body.addView(breakButton, LinearLayout.LayoutParams(-1, 52.dp()).apply { topMargin = 6.dp() })
        distractionButton = button("+ Record a Distraction", Color.rgb(235, 231, 243), ink)
        body.addView(distractionButton, LinearLayout.LayoutParams(-1, 52.dp()).apply { topMargin = 6.dp() })
        val stop = button("Stop & Save", Color.rgb(112, 105, 135))
        body.addView(stop, LinearLayout.LayoutParams(-1, 52.dp()).apply { topMargin = 6.dp() })
        val complete = button("Mark Task Completed", Color.rgb(83, 143, 113))
        body.addView(complete, LinearLayout.LayoutParams(-1, 52.dp()).apply { topMargin = 6.dp() })
        body.addView(text("Breaks are tracked separately and never count toward task progress. Distractions are saved for your productivity review.", 12f, muted).apply { setPadding(2.dp(), 16.dp(), 2.dp(), 0) })
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(layoutInflater.inflate(R.layout.bottom_navigation_bar, root, false))
        setContentView(root); setupBottomNavigation(NavTab.NONE)
        focusButton.setOnClickListener {
            if (onBreak) resumeFromBreak()
            else if (sessionId == null) start()
            else pause(false)
        }
        breakButton.setOnClickListener {
            if (onBreak) resumeFromBreak()
            else if (sessionId != null) beginBreak()
            else Toast.makeText(this, "Resume focus before taking a break", Toast.LENGTH_SHORT).show()
        }
        distractionButton.setOnClickListener {
            val input = EditText(this).apply { hint = "What distracted you? (optional)"; setSingleLine(false) }
            AlertDialog.Builder(this).setTitle("Record a distraction")
                .setMessage("Log an interruption without stopping your focus timer.")
                .setView(input).setNegativeButton("Cancel", null)
                .setPositiveButton("Save") { _, _ ->
                    database.logDistraction(taskId, sessionId, input.text.toString().trim())
                    updateDisplay()
                    Toast.makeText(this, "Distraction recorded", Toast.LENGTH_SHORT).show()
                }.show()
        }
        stop.setOnClickListener { stopSession(); finish() }
        complete.setOnClickListener {
            stopSession(); database.completeTask(taskId)
            Toast.makeText(this, "Task completed", Toast.LENGTH_SHORT).show(); finish()
        }
        reconnect(autoStart = true)
    }
    private fun reconnect(autoStart: Boolean) {
        val active = database.activeFocusSession(taskId)
        val breakSession = database.breakSession(taskId)
        onBreak = breakSession != null
        sessionId = (breakSession ?: active)?.id
        if (sessionId == null && autoStart) start()
        else { syncButtons(); handler.removeCallbacks(tick); if (sessionId != null) handler.post(tick); updateDisplay() }
    }
    private fun syncButtons() {
        focusButton.text = when { onBreak -> "On Break"; sessionId == null -> "Resume Focus"; else -> "Pause Focus" }
        focusButton.isEnabled = !onBreak
        breakButton.text = if (onBreak) "End Break & Resume" else "Take a Break"
    }
    private fun start() {
        try {
            sessionId = database.startFocus(taskId); onBreak = false; syncButtons()
            handler.removeCallbacks(tick); handler.post(tick)
        } catch (e: Exception) { Toast.makeText(this, e.message ?: "Could not start focus", Toast.LENGTH_LONG).show() }
    }
    private fun beginBreak() {
        val id = sessionId ?: return
        database.startBreak(id); onBreak = true; syncButtons(); updateDisplay()
    }
    private fun resumeFromBreak() {
        val id = sessionId ?: return
        database.endBreak(id, resumeFocus = true); onBreak = false; syncButtons(); updateDisplay()
    }
    private fun pause(finished: Boolean) {
        val id = sessionId ?: return
        if (onBreak) database.endBreak(id, resumeFocus = false)
        else database.pauseFocus(id, finished = finished)
        sessionId = null; onBreak = false; handler.removeCallbacks(tick); syncButtons(); updateDisplay()
    }
    private fun stopSession() {
        if (onBreak) {
            val id = sessionId ?: return
            database.endBreak(id, resumeFocus = false)
            sessionId = null; onBreak = false
        } else pause(true)
        handler.removeCallbacks(tick)
    }
    private fun updateDisplay() {
        val task = database.task(taskId) ?: return
        val seconds = database.focusedSeconds(taskId)
        timer.text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
        val percent = if (task.status == "Completed") 100 else ((seconds * 100L) / (task.estimated.toLong() * 60L).coerceAtLeast(1)).toInt().coerceIn(0, 100)
        progress.progress = percent
        details.text = "${seconds / 60}m ${seconds % 60}s focused  •  ${task.estimated}m estimated  •  $percent%"
        val breaks = database.breakSeconds(taskId)
        breakDetails.text = "${if (onBreak) "Break running  •  " else ""}${breaks / 60}m ${breaks % 60}s break  •  ${database.distractionCount(taskId)} distractions"
    }
    override fun onResume() { super.onResume(); if (::database.isInitialized && taskId != -1L) reconnect(autoStart = false) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
}
