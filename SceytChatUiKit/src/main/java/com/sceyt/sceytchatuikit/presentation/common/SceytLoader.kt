package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import com.sceyt.sceytchatuikit.extensions.checkAndDismissSafety
import com.sceyt.sceytchatuikit.extensions.showSafety
import com.sceyt.sceytchatuikit.presentation.root.SceytProgressDialogLoading

object SceytLoader {
    private var progressDialog: SceytProgressDialogLoading? = null

    fun showLoading(context: Context) {
        if (progressDialog?.isShowing == true)
            return
        progressDialog = SceytProgressDialogLoading(context).apply { showSafety() }
    }

    fun hideLoading() {
        progressDialog?.checkAndDismissSafety()
    }
}