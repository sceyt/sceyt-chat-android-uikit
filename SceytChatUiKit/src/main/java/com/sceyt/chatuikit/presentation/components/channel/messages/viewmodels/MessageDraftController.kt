package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.text.Editable
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.data.models.channels.DraftAttachment
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.media.audio.AudioRecordData
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.mappers.toBodyAttribute
import com.sceyt.chatuikit.persistence.mappers.toVoiceAttachmentData
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MessageDraftController(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val channelInteractor: ChannelInteractor,
    private val conversationId: () -> Long,
    private val isViewOnceSelected: () -> Boolean,
    private val setViewOnceSelected: (Boolean) -> Unit,
) {
    fun updateDraftMessage(
        text: Editable?,
        attachments: List<Attachment>,
        audioRecordData: AudioRecordData?,
        mentionUsers: List<Mention>,
        styling: List<BodyStyleRange>?,
        replyOrEditMessage: SceytMessage?,
        isReply: Boolean,
    ) {
        val viewOnce = normalizedViewOnce(attachments.size)
        scope.launch(ioDispatcher) {
            withContext(NonCancellable) {
                channelInteractor.updateDraftMessage(
                    buildDraftMessage(
                        text = text,
                        attachments = attachments,
                        audioRecordData = audioRecordData,
                        mentionUsers = mentionUsers,
                        styling = styling,
                        replyOrEditMessage = replyOrEditMessage,
                        isReply = isReply,
                        viewOnce = viewOnce
                    )
                )
            }
        }
    }

    private fun normalizedViewOnce(attachmentsCount: Int): Boolean {
        val selected = isViewOnceSelected()
        if (selected && attachmentsCount != 1) {
            setViewOnceSelected(false)
            return false
        }
        return selected
    }

    private fun buildDraftMessage(
        text: Editable?,
        attachments: List<Attachment>,
        audioRecordData: AudioRecordData?,
        mentionUsers: List<Mention>,
        styling: List<BodyStyleRange>?,
        replyOrEditMessage: SceytMessage?,
        isReply: Boolean,
        viewOnce: Boolean,
    ): DraftMessage {
        val channelId = conversationId()
        val bodyAttributes = mentionUsers.map { it.toBodyAttribute() }.toMutableSet()
        styling?.let {
            bodyAttributes.addAll(it.map { styleRange -> styleRange.toBodyAttribute() })
        }

        val draftAttachments = attachments.mapNotNull { attachment ->
            DraftAttachment(
                channelId = channelId,
                filePath = attachment.filePath ?: return@mapNotNull null,
                type = AttachmentTypeEnum.entries.find {
                    it.value == attachment.type
                } ?: return@mapNotNull null
            )
        }

        return DraftMessage(
            channelId = channelId,
            body = text?.toString(),
            createdAt = System.currentTimeMillis(),
            mentionUsers = mentionUsers.map {
                createEmptyUser(it.recipientId, it.name)
            },
            replyOrEditMessage = replyOrEditMessage,
            isReply = isReply,
            bodyAttributes = bodyAttributes.toList(),
            attachments = draftAttachments,
            voiceAttachment = audioRecordData?.toVoiceAttachmentData(channelId),
            viewOnce = viewOnce
        )
    }
}
