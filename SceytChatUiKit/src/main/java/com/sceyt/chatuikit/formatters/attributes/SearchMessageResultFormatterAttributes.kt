package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.styles.search.ChatsSearchMessageItemStyle

data class SearchMessageResultFormatterAttributes(
    val channel: SceytChannel,
    val message: SceytMessage,
    val searchMessageItemStyle: ChatsSearchMessageItemStyle
)