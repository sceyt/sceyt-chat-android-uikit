package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftMessage(
    val channelId: Long,
    val body: String?,
    val createdAt: Long,
    val mentionUsers: List<SceytUser>?,
    val replyOrEditMessage: SceytMessage?,
    val isReply: Boolean,
    val bodyAttributes: List<BodyAttribute>?,
    val attachments: List<DraftAttachment>?,
    val voiceAttachment: DraftVoiceAttachment?,
    val viewOnce: Boolean,
) : Parcelable {

    fun hasContent() = !body.isNullOrBlank()
            || !attachments.isNullOrEmpty()
            || voiceAttachment != null
            || replyOrEditMessage != null
}
