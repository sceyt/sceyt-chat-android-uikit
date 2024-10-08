package com.sceyt.chatuikit.config

import android.content.Intent

data class UploadNotificationPendingIntentData(
        val classToOpen: Class<*>,
        val extraKey: String?,
        val intentFlags: Int? = Intent.FLAG_ACTIVITY_CLEAR_TOP
)