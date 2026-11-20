package com.example.tasky_add_mngmt

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity

enum class NavTab { HOME, TASKS, CALENDAR, PROGRESS, PROFILE, NONE }





fun ComponentActivity.setupBackButton(button: View? = findViewById(R.id.btnBack)) {
    button?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
}








fun Activity.openTab(target: Class<*>) {
    if (this.javaClass == target) return
    startActivity(
        Intent(this, target)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    overridePendingTransition(0, 0)
    if (this !is HomeDashboardActivity) {
        finish()
        overridePendingTransition(0, 0)
    }
}






fun Activity.setupBottomNavigation(currentTab: NavTab) {
    if (findViewById<View>(R.id.bottom_navigation) == null) return

    val items = mapOf(
        NavTab.HOME to Pair(
            findViewById<View>(R.id.nav_item_home),
            Pair(findViewById<ImageView>(R.id.nav_icon_home), findViewById<TextView>(R.id.nav_label_home))
        ),
        NavTab.TASKS to Pair(
            findViewById<View>(R.id.nav_item_tasks),
            Pair(findViewById<ImageView>(R.id.nav_icon_tasks), findViewById<TextView>(R.id.nav_label_tasks))
        ),
        NavTab.CALENDAR to Pair(
            findViewById<View>(R.id.nav_item_calendar),
            Pair(findViewById<ImageView>(R.id.nav_icon_calendar), findViewById<TextView>(R.id.nav_label_calendar))
        ),
        NavTab.PROGRESS to Pair(
            findViewById<View>(R.id.nav_item_progress),
            Pair(findViewById<ImageView>(R.id.nav_icon_progress), findViewById<TextView>(R.id.nav_label_progress))
        ),
        NavTab.PROFILE to Pair(
            findViewById<View>(R.id.nav_item_profile),
            Pair(findViewById<ImageView>(R.id.nav_icon_profile), findViewById<TextView>(R.id.nav_label_profile))
        )
    )

    val activeColor = Color.parseColor("#7656DB")
    val inactiveColor = Color.parseColor("#7A778F")

    items.forEach { (tab, views) ->
        val (itemView, iconAndLabel) = views
        val (icon, label) = iconAndLabel
        val isActive = (tab == currentTab)
        val color = if (isActive) activeColor else inactiveColor

        icon?.imageTintList = ColorStateList.valueOf(color)
        label?.setTextColor(color)

        itemView?.setOnClickListener {
            val targetClass = when (tab) {
                NavTab.HOME -> HomeDashboardActivity::class.java
                NavTab.TASKS -> TaskManagementActivity::class.java
                NavTab.CALENDAR -> CalendarActivity::class.java
                NavTab.PROGRESS -> WorkloadMonitoringActivity::class.java
                NavTab.PROFILE -> ProfileActivity::class.java
                NavTab.NONE -> null
            }
            targetClass?.let { openTab(it) }
        }
    }
}
