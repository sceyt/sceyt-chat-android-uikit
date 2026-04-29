package com.sceyt.chatuikit.navigation

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser

sealed class MediaPreviewParams {
    abstract val showInChatChannel: SceytChannel?
    abstract val sourceView: View?

    data class SingleAttachment(
        val attachment: SceytAttachment,
        val from: SceytUser?,
        val channelId: Long,
        val reversed: Boolean = false,
        override val showInChatChannel: SceytChannel? = null,
        override val sourceView: View? = null,
    ) : MediaPreviewParams()

    data class PreloadedList(
        val items: List<AttachmentWithUserData>,
        val initialIndex: Int,
        override val showInChatChannel: SceytChannel? = null,
        override val sourceView: View? = null,
    ) : MediaPreviewParams()
}
