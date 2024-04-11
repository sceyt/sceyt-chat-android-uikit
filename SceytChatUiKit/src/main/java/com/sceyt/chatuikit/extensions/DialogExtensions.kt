package com.sceyt.chatuikit.extensions

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

fun DialogFragment.dismissSafety() {
    try {
        dismissAllowingStateLoss()
    } catch (ex: Exception) {
    }
}

fun DialogFragment?.isNullOrNotAdded(): Boolean {
    return this == null || !this.isAdded
}

fun Dialog.dismissSafety() {
    try {
        if (isShowing)
            dismiss()
    } catch (ex: Exception) {
    }
}

fun Dialog.showSafety() {
    try {
        show()
    } catch (ex: Exception) {
    }
}

fun Dialog.checkAndShowSafety() {
    try {
        if (isShowing)
            return
        showSafety()
    } catch (ex: Exception) {
    }
}

fun Dialog.checkAndDismissSafety() {
    try {
        if (!isShowing)
            return
        dismissSafety()
    } catch (ex: Exception) {
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

fun DialogFragment.setBundleArguments(init: Bundle.() -> Unit = {}): DialogFragment {
    arguments = Bundle().apply { init() }
    return this
}
