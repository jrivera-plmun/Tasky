package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TaskDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)
        setupEdgeToEdge(findViewById(R.id.root), findViewById(R.id.bottom_navigation))

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvProgressPercent = findViewById<TextView>(R.id.tvProgressPercent)
        val progressTask = findViewById<ProgressBar>(R.id.progressTask)

        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId <= 0L || TaskyDatabase(this).task(taskId) == null) {
            toast("Task not found"); finish(); return
        }
        renderTask(taskId)
        findViewById<View>(R.id.btnFocusSession).setOnClickListener {
            if (TaskyDatabase(this).task(taskId)?.status == "Completed") {
                toast("This task is already completed")
            } else {
                startActivity(Intent(this, FocusSessionActivity::class.java).putExtra("task_id", taskId))
            }
        }
        
        setupBackButton()

        findViewById<View>(R.id.btnComplete).setOnClickListener {
            if (taskId <= 0) { toast("Select a saved task first"); return@setOnClickListener }
            TaskyDatabase(this).completeTask(taskId)
            tvStatus.setText(R.string.completed)
            progressTask.progress = 100
            tvProgressPercent.setText(R.string.progress_100)
            toast("Task marked as completed")
            renderTask(taskId)
        }
        findViewById<View>(R.id.btnEdit).setOnClickListener {
            startActivity(Intent(this, EditTaskActivity::class.java).putExtra("task_id", taskId))
        }
        findViewById<View>(R.id.btnDelete).setOnClickListener {
            if (taskId <= 0) { toast("Select a saved task first"); return@setOnClickListener }
            android.app.AlertDialog.Builder(this).setMessage("Delete this task?")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                    TaskyDatabase(this).deleteTask(taskId)
                    toast("Task deleted")
                    finish()
                }.show()
        }

        
        setupBottomNavigation(NavTab.TASKS)
    }

    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        intent.getLongExtra("task_id", -1L).takeIf { it > 0 }?.let { renderTask(it) }
    }
    private fun renderTask(id: Long) {
        val task = TaskyDatabase(this).task(id) ?: return
        findViewById<TextView>(R.id.tvTaskTitle).text = task.title
        findViewById<TextView>(R.id.tvDescription).text = task.description
        findViewById<TextView>(R.id.tvCategory).text = task.category
        findViewById<TextView>(R.id.tvPriority).text = "${task.priority} · ${task.difficulty} difficulty"
        findViewById<TextView>(R.id.tvStatus).text = if(task.overdue) "Overdue" else task.status
        findViewById<TextView>(R.id.tvDueDate).text = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(java.util.Date(task.deadline))
        findViewById<TextView>(R.id.tvDueTime).text = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(task.deadline))
        findViewById<TextView>(R.id.tvDuration).text = "${task.estimated} min"
        findViewById<TextView>(R.id.tvProgressPercent).text = "${task.percent}%"
        findViewById<ProgressBar>(R.id.progressTask).progress = task.percent
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
