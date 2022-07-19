package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.getPresentableName
import kotlinx.parcelize.Parcelize

@Parcelize
class SceytDirectChannel(
        override var id: Long = 0,
        override var metadata: String? = null,
        override var label: String? = null,
        override var createdAt: Long,
        override var updatedAt: Long,
        override var unreadMessageCount: Long,
        override var lastMessage: SceytMessage? = null,
        override var muted: Boolean = false,
        override var muteUntil: Long?,
        override var channelType: ChannelTypeEnum = ChannelTypeEnum.Direct,
        var peer: SceytMember? = null,
) : SceytChannel(id, createdAt, updatedAt, unreadMessageCount, lastMessage, label, metadata, muted, null, muteUntil, channelType) {

    override val channelSubject: String
        get() = peer?.getPresentableName() ?: ""

    override val iconUrl: String?
        get() = peer?.avatarUrl

    override val isGroup: Boolean
        get() = false
}

