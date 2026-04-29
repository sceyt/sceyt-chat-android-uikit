package com.sceyt.chatuikit.data.models.search

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser

data class GlobalSearchAttachmentResult(
    val attachment: SceytAttachment,
    val message: SceytMessage,
    val channel: SceytChannel,
    val sender: SceytUser?,
    val kind: GlobalSearchAttachmentKind,
)
