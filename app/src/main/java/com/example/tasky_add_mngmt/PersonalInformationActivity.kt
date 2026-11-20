package com.example.tasky_add_mngmt

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PersonalInformationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_information)

        val root = findViewById<View>(R.id.root)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
                insets
            }
        }

        setupBackButton()
        AccountStore(this).current()?.let { account ->
            findViewById<TextView>(R.id.tvProfileName)?.text = account.name
            val initials = account.name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                .take(2).map { it.first().uppercaseChar() }.joinToString("")
            findViewById<TextView>(R.id.tvProfileInitials)?.text = initials.ifEmpty { "?" }
            findViewById<TextView>(R.id.tvProfileEmail)?.text = account.email
            findViewById<TextView>(R.id.tvFullName)?.text = account.name
            findViewById<TextView>(R.id.tvEmail)?.text = account.email
            findViewById<TextView>(R.id.tvBio)?.text = account.bio
            findViewById<TextView>(R.id.tvBirthday)?.text = account.birthday
        }

        findViewById<View>(R.id.btnDeleteAccount)?.setOnClickListener {
            Toast.makeText(this, "Delete Account clicked", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        AccountStore(this).current()?.let { account ->
            findViewById<TextView>(R.id.tvProfileName)?.text = account.name
            findViewById<TextView>(R.id.tvProfileEmail)?.text = account.email
            findViewById<TextView>(R.id.tvFullName)?.text = account.name
            findViewById<TextView>(R.id.tvEmail)?.text = account.email
            findViewById<TextView>(R.id.tvBio)?.text = account.bio
            findViewById<TextView>(R.id.tvBirthday)?.text = account.birthday
            findViewById<TextView>(R.id.tvProfileInitials)?.text = account.name.trim().split(Regex("\\s+"))
                .filter { it.isNotEmpty() }.take(2).map { it.first().uppercaseChar() }.joinToString("").ifEmpty { "?" }
        }
    }
}
