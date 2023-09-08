package com.sceyt.sceytchatuikit.extensions

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.os.Build

@Suppress("DEPRECATION")
fun Activity.overrideTransitions(enterAnim: Int, exitAnim: Int, isOpen: Boolean) {
    if (Build.VERSION.SDK_INT >= 34) {
        val type = if (isOpen) OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE
        overrideActivityTransition(type, enterAnim, exitAnim)
    } else
        overridePendingTransition(enterAnim, exitAnim)
}