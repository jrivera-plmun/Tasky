package com.example.tasky_add_mngmt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class sign_inActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in)
        setupBackButton()
        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            if (AccountStore(this).login(email,password)) {
                startActivity(Intent(this,HomeDashboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            } else Toast.makeText(this,"Incorrect email or password",Toast.LENGTH_LONG).show()
        }
        findViewById<TextView>(R.id.tvCreateAccount).setOnClickListener { startActivity(Intent(this,create_accActivity::class.java)); finish() }
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
    }
}
