package com.sceyt.chatuikit.presentation.components.channel.input.listeners

import android.text.Editable
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserAction
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange

interface MessageInputActionCallback {
    fun sendMessage(message: Message, linkDetails: LinkPreviewDetails?)
    fun sendMessages(message: List<Message>, linkDetails: LinkPreviewDetails?)
    fun sendEditMessage(message: SceytMessage, linkDetails: LinkPreviewDetails?)
    fun sendChannelEvent(action: InputUserAction)
    fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>, styling: List<BodyStyleRange>?,
                           replyOrEditMessage: SceytMessage?, isReply: Boolean)

    fun mention(query: String)
    fun join()
    fun clearChat()
    fun scrollToNext()
    fun scrollToPrev()
}