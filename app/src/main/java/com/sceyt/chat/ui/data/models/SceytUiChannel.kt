package com.sceyt.chat.ui.data.models

import com.sceyt.chat.models.message.Message
import java.util.*

open class SceytUiChannel(var id: Long,
                          var createdAt: Long,
                          var updatedAt: Long,
                          var unreadMessageCount: Long,
                          var lastMessage: Message?,
                          var label: String?,
                          var metadata: String?,
                          var muted: Boolean,
                          var muteExpireDate: Date?,
                          var channelType: ChannelTypeEnum?)
