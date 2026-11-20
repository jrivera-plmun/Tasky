package com.example.tasky_add_mngmt

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import android.widget.LinearLayout
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class ProfileActivity : AppCompatActivity() {
    private val focusIndicator by lazy { FocusIndicator(this) }


    private lateinit var optLight: MaterialCardView
    private lateinit var optDark: MaterialCardView
    private lateinit var optSystem: MaterialCardView
    private lateinit var tvLight: TextView
    private lateinit var tvDark: TextView
    private lateinit var tvSystem: TextView


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2026 && grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            TaskReminderScheduler.refresh(this)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupEdgeToEdge(findViewById(R.id.root), findViewById(R.id.bottom_navigation))

        
        findViewById<View>(R.id.btnEdit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        findViewById<View>(R.id.tvEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        
        optLight = findViewById(R.id.optLight)
        optDark = findViewById(R.id.optDark)
        optSystem = findViewById(R.id.optSystem)
        tvLight = findViewById(R.id.tvLight)
        tvDark = findViewById(R.id.tvDark)
        tvSystem = findViewById(R.id.tvSystem)

        val store = AccountStore(this)
        optLight.setOnClickListener { selectTheme(optLight, tvLight); store.setPreference("theme", "light"); AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        optDark.setOnClickListener { selectTheme(optDark, tvDark); store.setPreference("theme", "dark"); AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        optSystem.setOnClickListener { selectTheme(optSystem, tvSystem); store.setPreference("theme", "system"); AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        when(store.preference("theme", "system")) {
            "light" -> selectTheme(optLight,tvLight)
            "dark" -> selectTheme(optDark,tvDark)
            else -> selectTheme(optSystem,tvSystem)
        }
        listOf("reminders" to R.id.switchReminders, "overdue" to R.id.switchOverdue, "weekly" to R.id.switchWeekly).forEach { (key,id) ->
            findViewById<CompoundButton>(id)?.apply {
                isChecked = store.preference(key,if(key=="weekly") "0" else "1") == "1"
                setOnCheckedChangeListener { _,checked -> store.setPreference(key,if(checked) "1" else "0"); TaskReminderScheduler.refresh(this@ProfileActivity) }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2026)
        }

        
        TaskReminderScheduler.refresh(this)

        
        findViewById<View>(R.id.rowPersonal).setOnClickListener {
            startActivity(Intent(this, PersonalInformationActivity::class.java))
        }
        findViewById<View>(R.id.rowPassword).setOnClickListener {
            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(36, 16, 36, 8)
            }
            fun field(hint: String) = android.widget.EditText(this).apply {
                this.hint = hint
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                container.addView(this)
            }
            val old = field("Current password")
            val next = field("New password (8+ characters)")
            val confirm = field("Confirm new password")
            android.app.AlertDialog.Builder(this).setTitle("Change password").setView(container)
                .setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                    if (next.text.toString() != confirm.text.toString()) toast("Passwords do not match")
                    else if (AccountStore(this).changePassword(old.text.toString(), next.text.toString())) toast("Password updated")
                    else toast("Incorrect current password or password too short")
                }.show()
        }
        findViewById<View>(R.id.rowPrivacy).setOnClickListener {
            android.app.AlertDialog.Builder(this).setTitle("Privacy & data")
                .setMessage("TASKY stores your account and tasks in a local SQLite database on this device. This version does not provide cloud synchronization or an account-data export. Clearing app storage can permanently erase local data. Keep a backup before doing so.")
                .setPositiveButton("OK", null).show()
        }
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            store.logout()
            startActivity(Intent(this,sign_inActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }

        
        setupBottomNavigation(NavTab.PROFILE)
    }

    override fun onResume() {
        focusIndicator.start()
        super.onResume()
        TaskyDateHeaders.update(window.decorView)
        val account = AccountStore(this).current() ?: return
        
        
        val root = findViewById<View>(R.id.root)
        fun walk(view: View, action: (TextView) -> Unit) {
            if (view is TextView) action(view)
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i), action)
        }
        walk(root) { label ->
            when (label.text.toString()) {
                "Alex", account.name -> label.text = account.name
                "alex@example.com", account.email -> label.text = account.email
                "AL", account.name.take(2).uppercase() -> label.text = account.name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "?" }
            }
        }
    }

    
    private fun selectTheme(selectedCard: MaterialCardView, selectedText: TextView) {
        val cards = listOf(optLight to tvLight, optDark to tvDark, optSystem to tvSystem)
        val purple = ContextCompat.getColor(this, R.color.td_purple)
        val muted = ContextCompat.getColor(this, R.color.td_muted)
        val white = ContextCompat.getColor(this, R.color.td_white)

        for ((card, text) in cards) {
            val selected = card === selectedCard && text === selectedText
            card.setCardBackgroundColor(if (selected) white else android.graphics.Color.TRANSPARENT)
            val color = if (selected) purple else muted
            text.setTextColor(color)
            text.compoundDrawableTintList = ColorStateList.valueOf(color)
            text.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    override fun onPause() {
        focusIndicator.stop()
        super.onPause()
    }
}
