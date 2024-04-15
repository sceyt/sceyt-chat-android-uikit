package com.sceyt.chatuikit.presentation.common

import android.content.Context
import com.sceyt.chatuikit.extensions.checkAndDismissSafety
import com.sceyt.chatuikit.extensions.showSafety

object SceytLoader {
    private var progressDialog: SceytProgressDialogLoading? = null

    @JvmStatic
    fun showLoading(context: Context) {
        if (progressDialog?.isShowing == true)
            return
        progressDialog = SceytProgressDialogLoading(context).apply { showSafety() }
    }

    @JvmStatic
    fun hideLoading() {
        progressDialog?.checkAndDismissSafety()
    }
}