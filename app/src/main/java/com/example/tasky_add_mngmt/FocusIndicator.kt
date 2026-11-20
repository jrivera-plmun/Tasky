package com.example.tasky_add_mngmt

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale


class FocusIndicator(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    private val db by lazy { TaskyDatabase(activity) }
    private var banner: LinearLayout? = null
    private val tick = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 1000L)
        }
    }

    fun start() {
        if (banner == null) {
            val content = activity.findViewById<FrameLayout>(android.R.id.content)
            val panel = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(116, 82, 215))
                    cornerRadius = dp(15).toFloat()
                }
                elevation = dp(7).toFloat()
                visibility = View.GONE
            }
            content.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                setMargins(dp(14), 0, dp(14), dp(83))
            })
            banner = panel
        }
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun update() {
        val session = db.runningFocusOrBreakAny()
        val panel = banner ?: return
        val task = session?.let { db.task(it.taskId) }
        if (session == null || task == null) {
            panel.visibility = View.GONE
            return
        }
        panel.visibility = View.VISIBLE
        panel.removeAllViews()
        val onBreak = session.status == "Paused"
        val seconds = if (onBreak) db.breakSeconds(session.taskId) else db.focusedSeconds(session.taskId)
        val elapsed = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
        val title = TextView(activity).apply {
            text = if (onBreak) "● BREAK RUNNING   •   $elapsed" else "● FOCUS ACTIVE   •   $elapsed"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val taskName = TextView(activity).apply {
            text = "${task.title}   ·   Tap to return to session"
            textSize = 13f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        panel.addView(title)
        panel.addView(taskName)
        panel.setOnClickListener {
            activity.startActivity(Intent(activity, FocusSessionActivity::class.java).putExtra("task_id", session.taskId))
        }
    }

    fun stop() {
        handler.removeCallbacks(tick)
        banner?.visibility = View.GONE
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
