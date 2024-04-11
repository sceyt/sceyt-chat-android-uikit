package com.sceyt.chatuikit.sceytconfigs

import android.content.Intent

data class BackgroundUploadNotificationClickData(
        val classToOpen: Class<*>,
        val channelToParcelKey: String?,
        val intentFlags: Int? = Intent.FLAG_ACTIVITY_CLEAR_TOP
)