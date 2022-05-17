package com.sceyt.chat.ui.data

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_listeners.ChannelListener
import com.sceyt.chat.sceyt_listeners.MessageListener
import com.sceyt.chat.ui.extensions.TAG
import kotlinx.coroutines.flow.MutableStateFlow

class ChannelEventsObserverService {
    val onMessageFlow: MutableStateFlow<Pair<Channel, Message>?> = MutableStateFlow(null)
    val onMessageStatusFlow: MutableStateFlow<MessageStatusChange?> = MutableStateFlow(null)

    init {
        ChatClient.getClient().addMessageListener(TAG, object : MessageListener {

            override fun onMessage(channel: Channel, message: Message) {

                onMessageFlow.value = Pair(channel, message)
                /* if (message.from.id == ChatClient.getClient().user.id || message.channelId != channelId
                         || message.replyInThread)
                     return*/

                // channelScreenItemsCRUD.insertMessage(message)
                // liveDataDownload.postValue(convertAttachmentsLinksToMap(message.attachments))

                ClientWrapper.markMessagesAsDisplayed(channel.id, longArrayOf(message.id)) { _, _ ->
                }
            }

            override fun onMessageDeleted(message: Message?) {
                /* if (channelId == message?.channelId) {
                      channelScreenItemsCRUD.markAsDeleted(message)
                 }*/
            }

            override fun onMessageEdited(message: Message?) {
                /*  if (channelId == message?.channelId) {
                      channelScreenItemsCRUD.updateEditedMessageBodyAndType(message)
                  }*/
            }

            override fun onReactionAdded(message: Message?, reaction: Reaction?) {
                /* if (channelId == message?.channelId) {
                      channelScreenItemsCRUD.updateMessageReactions(message)
                 }*/
            }

            override fun onReactionDeleted(message: Message?, reaction: Reaction?) {
                /*   if (channelId == message?.channelId) {
                      channelScreenItemsCRUD.updateMessageReactions(message)
                   }*/
            }
        })

        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {

            override fun onDeliveryReceiptReceived(
                    channel: Channel?,
                    from: User?,
                    messageIds: MutableList<Long>
            ) {
                /* if (channelId == channel?.id) {
                     channelScreenItemsCRUD.updateMessagesStatus(
                         DeliveryStatus.Delivered,
                         *messageIds.toLongArray()
                     )
                 }*/

                onMessageStatusFlow.value = MessageStatusChange(channel, from, DeliveryStatus.Delivered, messageIds)
            }

            override fun onReadReceiptReceived(
                    channel: Channel?,
                    from: User?,
                    messageIds: MutableList<Long>
            ) {
                /*    if (channelId == channel?.id) {
                        channelScreenItemsCRUD.updateMessagesStatus(
                            DeliveryStatus.Read,
                            *messageIds.toLongArray(),
                        )
                    }*/
                onMessageStatusFlow.value = MessageStatusChange(channel, from, DeliveryStatus.Read, messageIds)
            }

            override fun onChannelUpdated(channel: Channel?) {
                /*channel?.let {
                    channelLiveData.postValue(it)
                }*/
            }

            override fun onClearedHistory(channel: Channel?) {
                //  notifyUpdate.postValue(true)
            }
        })
        /*     channelRepository.registerOnChannelChange(TAG, object : ChannelRepository.OnChangeListener {
                 override fun onChange(changeType: ChannelRepository.ChangeType) {
                     when (changeType) {
                         ChannelRepository.ChangeType.CLEAR_MESSAGES -> {
                             notifyUpdate.postValue(true)
                         }
                         else -> {
                         }
                     }
                 }
             })*/


    }

    data class MessageStatusChange(
            val channel: Channel?,
            val from: User?,
            val status: DeliveryStatus,
            val messageIds: MutableList<Long>
    )
}