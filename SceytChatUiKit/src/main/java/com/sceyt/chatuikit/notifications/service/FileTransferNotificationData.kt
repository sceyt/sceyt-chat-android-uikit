package com.sceyt.chatuikit.notifications.service

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage

data class FileTransferNotificationData (
    val channel: SceytChannel,
    val message: SceytMessage
)