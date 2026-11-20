package com.example.tasky_add_mngmt

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TaskyDateHeaders {
    private val oldDate = Regex("(?i)^(?:wed,?\\s*oct\\s*28,?\\s*2026|wednesday,?\\s*october\\s*28,?\\s*2026)$")
    fun update(root: View) {
        val today = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date()).uppercase(Locale.US)
        fun visit(view: View) {
            if (view is TextView && oldDate.matches(view.text.toString().trim())) view.text = today
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
    }
}
