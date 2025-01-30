package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

object ChatReactionMessagesCache : SceytKoinComponent {
    private val messageCache: MessagesCache by inject()
    private val channelsCache: ChannelsCache by inject()
    private val messageInteractor: MessageInteractor by inject()
    private val list = mutableMapOf<Long, SceytMessage>()

    suspend fun getNeededMessages(ids: Map<Long, Long>) {
        ids.forEach {
            getMessage(it.key, it.value)
        }
    }

    suspend fun getNeededMessages(channels: List<SceytChannel>) {
        channels.forEach { channel ->
            val messageId = channel.pendingReactions?.maxByOrNull { reactionData -> reactionData.createdAt }?.messageId
                    ?: run { channel.newReactions?.maxByOrNull { reactionData -> reactionData.id } }?.messageId

            if (messageId != null && !list.containsKey(messageId))
                getMessage(channel.id, messageId)
        }
    }

    private suspend fun getMessage(channelId: Long, messageId: Long) {
        messageCache.get(channelId, messageId)?.let {
            list[it.id] = it
            channelsCache.channelLastReactionLoaded(channelId)
        } ?: run {
            messageInteractor.getMessageFromDbById(messageId)?.let {
                list[it.id] = it
                channelsCache.channelLastReactionLoaded(channelId)
            } ?: run {
                ConnectionEventManager.awaitToConnectSceytWithTimeout(5.seconds.inWholeMilliseconds)
                val result = messageInteractor.getMessageFromServerById(channelId, messageId)
                if (result is SceytResponse.Success) {
                    result.data?.let {
                        list[it.id] = it
                        channelsCache.channelLastReactionLoaded(channelId)
                    }
                }
            }
        }
    }

    fun addMessage(message: SceytMessage) {
        list[message.id] = message
    }

    fun getMessageById(messageId: Long): SceytMessage? {
        return list[messageId]
    }
}