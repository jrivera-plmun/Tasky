package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EditTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, AddTaskActivity::class.java).putExtra("task_id", intent.getLongExtra("task_id", -1L)))
        finish()
    }
}
