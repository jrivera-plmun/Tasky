package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class create_accActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_acc)
        setupBackButton()
        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {
            val name = findViewById<EditText>(R.id.etName).text.toString()
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            try {
                if (AccountStore(this).register(name,email,password)) {
                    startActivity(Intent(this, HomeDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                } else Toast.makeText(this,"This email is already registered",Toast.LENGTH_LONG).show()
            } catch (e: IllegalArgumentException) { Toast.makeText(this,e.message,Toast.LENGTH_LONG).show() }
        }
        findViewById<TextView>(R.id.tvSignIn).setOnClickListener { startActivity(Intent(this,sign_inActivity::class.java)); finish() }
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
    }
}
