package com.sceyt.chatuikit.data.models.messages

import com.sceyt.chatuikit.data.models.channels.DraftAttachment
import com.sceyt.chatuikit.data.models.channels.DraftVoiceAttachment
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention

data class UpdateDraftMessageData(
        val channelId: Long,
        val message: String?,
        val attachments: List<DraftAttachment>,
        val voiceAttachment: DraftVoiceAttachment?,
        val mentionUsers: List<Mention>,
        val styling: List<BodyStyleRange>?,
        val replyOrEditMessage: SceytMessage?,
        val isReply: Boolean,
) {
    fun hasContent() = !message.isNullOrBlank() || replyOrEditMessage != null
            || attachments.isNotEmpty() || voiceAttachment != null
}