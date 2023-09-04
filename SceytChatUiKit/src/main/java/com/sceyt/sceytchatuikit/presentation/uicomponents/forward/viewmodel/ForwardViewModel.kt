package com.sceyt.sceytchatuikit.presentation.uicomponents.forward.viewmodel

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Message.MessageBuilder
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.inject

class ForwardViewModel : BaseViewModel(), SceytKoinComponent {
    private val messagesMiddleWare by inject<PersistenceMessagesMiddleWare>()

    fun sendForwardMessage(vararg channelIds: Long, markOwnMessageAsForwarded: Boolean, messages: List<SceytMessage>) = callbackFlow {
        trySend(State.Loading)
        channelIds.forEach { channelId ->
            val messagesToSend = mutableListOf<Message>()
            messages.forEach {
                val message = MessageBuilder(channelId)
                    .setBody(it.body)
                    .setTid(ClientWrapper.generateTid())
                    .setType(it.type)
                    .apply {
                        if (markOwnMessageAsForwarded || it.incoming)
                            setForwardingMessageId(it.id)
                    }
                    .setAttachments(initAttachments(it.attachments).toTypedArray())
                    .setMetadata(it.metadata)
                    .setMentionedUserIds(it.mentionedUsers?.map { user -> user.id }?.toTypedArray()
                            ?: arrayOf())
                    //.setReplyInThread(it.replyInThread)
                    .build()

                messagesToSend.add(message)
            }

            messagesMiddleWare.sendFrowardMessages(channelId, *messagesToSend.toTypedArray())
        }
        trySend(State.Finish)
        awaitClose()
    }.flowOn(Dispatchers.IO)


    private fun initAttachments(list: Array<SceytAttachment>?): List<Attachment> {
        return list?.map {
            Attachment.Builder(it.filePath ?: "", it.url, it.type)
                .withTid(ClientWrapper.generateTid())
                .withUserId(it.userId)
                .setFileSize(it.fileSize)
                .setMetadata(it.metadata)
                .setName(it.name)
                .setCreatedAt(System.currentTimeMillis())
                .setUpload(false)
                .build()
        } ?: emptyList()
    }


    enum class State {
        Loading,
        Finish
    }
}