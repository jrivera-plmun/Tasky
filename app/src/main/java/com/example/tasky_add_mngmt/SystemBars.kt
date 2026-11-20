package com.example.tasky_add_mngmt

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding






fun ComponentActivity.setupEdgeToEdge(root: View, bottomNav: View) {
    val light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = light, navigationBarStyle = light)

    val navBasePadding = bottomNav.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        root.updatePadding(left = bars.left, top = bars.top, right = bars.right)
        bottomNav.updatePadding(bottom = navBasePadding + bars.bottom)
        insets
    }
}
