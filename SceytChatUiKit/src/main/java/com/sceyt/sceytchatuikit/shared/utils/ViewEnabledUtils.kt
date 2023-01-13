package com.sceyt.sceytchatuikit.shared.utils

import android.os.Looper
import android.view.View
import androidx.core.view.forEach
import com.google.android.material.bottomnavigation.BottomNavigationView

object ViewEnabledUtils {

    private var runnable: Runnable? = null

    fun disableViewForWhile(view: View, duration: Long = 1000) {
        view.isEnabled = false
        val handler = android.os.Handler(Looper.getMainLooper())
        runnable = Runnable {
            view.isEnabled = true
            runnable?.let { handler.removeCallbacks(it) }
        }
        runnable?.let { handler.postDelayed(it, duration) }
    }

    fun disableClickViewForWhile(view: View, duration: Long = 1000) {
        view.isClickable = false
        val handler = android.os.Handler(Looper.getMainLooper())
        runnable = Runnable {
            view.isClickable = true
            runnable?.let { handler.removeCallbacks(it) }
        }
        runnable?.let { handler.postDelayed(it, duration) }
    }

    fun disableBottomNavForWhile(view: BottomNavigationView, duration: Long = 1000) {
        view.menu.forEach { it.isEnabled = false }

        val handler = android.os.Handler(Looper.getMainLooper())
        runnable = Runnable {
            view.menu.forEach { it.isEnabled = true }
            runnable?.let { handler.removeCallbacks(it) }
        }
        runnable?.let { handler.postDelayed(it, duration) }
    }
}