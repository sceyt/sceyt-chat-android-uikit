package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange

data class DraftMessage(
        val chatId: Long,
        val message: String?,
        val createdAt: Long,
        val metadata: String?,
        val mentionUsers: List<User>?,
        val replyOrEditMessage: SceytMessage?,
        val isReply: Boolean,
        val styleRanges: List<BodyStyleRange>?,)
