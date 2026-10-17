package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage

sealed class PinUpdateEvent(
    val channelId: Long
) {

    class Pinned(
        channelId: Long,
        val messages: List<SceytPinnedMessage>,
    ) : PinUpdateEvent(channelId)

    class Unpinned(
        channelId: Long,
        val messages: List<SceytPinnedMessage>,
    ) : PinUpdateEvent(channelId)
}