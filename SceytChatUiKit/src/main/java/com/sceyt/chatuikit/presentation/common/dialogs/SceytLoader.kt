package com.sceyt.chatuikit.presentation.common.dialogs

import android.content.Context
import com.sceyt.chatuikit.extensions.checkAndDismissSafety
import com.sceyt.chatuikit.extensions.showSafety
import com.sceyt.chatuikit.presentation.common.dialogs.SceytLoadingDialog

object SceytLoader {
    private var progressDialog: SceytLoadingDialog? = null

    @JvmStatic
    fun showLoading(context: Context) {
        if (progressDialog?.isShowing == true)
            return
        progressDialog = SceytLoadingDialog(context).apply { showSafety() }
    }

    @JvmStatic
    fun hideLoading() {
        progressDialog?.checkAndDismissSafety()
    }
}