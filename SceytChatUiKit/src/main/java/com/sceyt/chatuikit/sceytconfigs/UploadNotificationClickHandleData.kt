package com.sceyt.chatuikit.sceytconfigs

import android.content.Intent

data class UploadNotificationClickHandleData(
        val classToOpen: Class<*>,
        val channelToParcelKey: String?,
        val intentFlags: Int? = Intent.FLAG_ACTIVITY_CLEAR_TOP
)