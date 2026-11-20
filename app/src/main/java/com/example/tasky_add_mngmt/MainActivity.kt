package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, sign_inActivity::class.java))
        }

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            startActivity(Intent(this, create_accActivity::class.java))
        }

        
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        if (isTaskRoot) btnBack.visibility = android.view.View.INVISIBLE else setupBackButton(btnBack)
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
    }
}