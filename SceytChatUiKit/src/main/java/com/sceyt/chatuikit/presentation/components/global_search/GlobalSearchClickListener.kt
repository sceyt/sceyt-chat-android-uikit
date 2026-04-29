package com.sceyt.chatuikit.presentation.components.global_search

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult

interface GlobalSearchClickListener {
    fun onChannelClicked(channel: SceytChannel)
    fun onMessageClicked(messageId: Long, channel: SceytChannel)
    fun onAttachmentClicked(result: GlobalSearchAttachmentResult)
    fun onMediaAttachmentClicked(
        sharedView: View,
        result: GlobalSearchAttachmentResult,
        allResults: List<GlobalSearchAttachmentResult>,
    )
}
