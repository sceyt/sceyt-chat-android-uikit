package com.sceyt.sceytchatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftMessage(
        val chatId: Long,
        val message: String?,
        val createdAt: Long,
        val mentionUsers: List<User>?,
        val replyOrEditMessage: SceytMessage?,
        val isReply: Boolean,
        val bodyAttributes: List<BodyAttribute>?) : Parcelable
