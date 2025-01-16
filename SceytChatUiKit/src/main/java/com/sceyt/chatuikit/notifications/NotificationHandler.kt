package com.sceyt.chatuikit.notifications

import android.content.Context
import com.sceyt.chatuikit.push.PushData

interface NotificationHandler {
   suspend fun showNotification(context: Context, data: PushData)
}