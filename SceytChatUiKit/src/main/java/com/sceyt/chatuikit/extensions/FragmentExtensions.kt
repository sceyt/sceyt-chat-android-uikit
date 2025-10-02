package com.sceyt.chatuikit.extensions

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.customToastSnackBar(message: String?) {
    try {
        if (isAdded)
            customToastSnackBar(view, message)
        else Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } catch (ex: Exception) {
        view?.context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }
}

fun <T : Fragment> T.setBundleArguments(init: Bundle.() -> Unit = {}): T {
    arguments = Bundle().apply { init() }
    return this
}