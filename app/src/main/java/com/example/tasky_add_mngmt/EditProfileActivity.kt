package com.example.tasky_add_mngmt

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        val root = findViewById<View>(R.id.root)
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
                insets
            }
        }

        setupBackButton()
        val store = AccountStore(this)
        store.current()?.let { account ->
            findViewById<EditText>(R.id.etFullName)?.setText(account.name)
            findViewById<EditText>(R.id.etEmail)?.setText(account.email)
            findViewById<EditText>(R.id.etBio)?.setText(account.bio)
            findViewById<TextView>(R.id.tvBirthday)?.text = account.birthday
        }

        findViewById<View>(R.id.btnCancel)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSaveProfile)?.setOnClickListener {
            val ok = store.update(findViewById<EditText>(R.id.etFullName).text.toString(),
                findViewById<EditText>(R.id.etEmail).text.toString(),
                findViewById<EditText>(R.id.etBio).text.toString(),
                findViewById<TextView>(R.id.tvBirthday).text.toString())
            Toast.makeText(this, if (ok) "Profile changes saved" else "Sign in and enter valid, unique details", Toast.LENGTH_LONG).show()
            if (ok) finish()
        }

        findViewById<View>(R.id.tvChangePhoto)?.setOnClickListener {
            Toast.makeText(this, "Change photo", Toast.LENGTH_SHORT).show()
        }

        val tvBirthday = findViewById<TextView>(R.id.tvBirthday)
        tvBirthday?.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, y, m, d ->
                val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                tvBirthday.text = "${months[m]} $d, $y"
            }, year, month, day)
            dpd.show()
        }
    }
    override fun onResume() {
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
    }
}
