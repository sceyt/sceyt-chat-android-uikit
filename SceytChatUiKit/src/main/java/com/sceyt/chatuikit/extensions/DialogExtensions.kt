package com.sceyt.chatuikit.extensions

import android.app.Dialog
import androidx.fragment.app.DialogFragment

fun DialogFragment.dismissSafety() {
    try {
        dismissAllowingStateLoss()
    } catch (_: Exception) {
    }
}

fun DialogFragment?.isNullOrNotAdded(): Boolean {
    return this == null || !this.isAdded
}

fun Dialog.dismissSafety() {
    try {
        if (isShowing)
            dismiss()
    } catch (_: Exception) {
    }
}

fun Dialog.showSafety() {
    try {
        show()
    } catch (_: Exception) {
    }
}

fun Dialog.checkAndShowSafety() {
    try {
        if (isShowing)
            return
        showSafety()
    } catch (_: Exception) {
    }
}

fun Dialog.checkAndDismissSafety() {
    try {
        if (!isShowing)
            return
        dismissSafety()
    } catch (_: Exception) {
    }
}

fun Dialog?.isNullOrNotShowing(): Boolean {
    return if (this == null)
        return true
    else !isShowing
}


fun Dialog?.isShowing(): Boolean {
    return this?.isShowing ?: return false
}