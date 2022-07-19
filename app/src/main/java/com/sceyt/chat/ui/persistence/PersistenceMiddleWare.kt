package com.sceyt.chat.ui.persistence

import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.persistence.dao.ChannelDao
import com.sceyt.chat.ui.persistence.dao.MessageDao
import com.sceyt.chat.ui.persistence.dao.UserDao
import com.sceyt.chat.ui.persistence.entity.UserEntity
import com.sceyt.chat.ui.persistence.entity.channel.UserChatLink
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity
import com.sceyt.chat.ui.persistence.mappers.toChannel
import com.sceyt.chat.ui.persistence.mappers.toChannelEntity
import com.sceyt.chat.ui.persistence.mappers.toMemberEntity
import com.sceyt.chat.ui.persistence.mappers.toMessageEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class PersistenceMiddleWare : KoinComponent {
    private val channelsRepository by inject<ChannelsRepository>()
    private val channelDao by inject<ChannelDao>()
    private val usersDao by inject<UserDao>()
    private val messageDao by inject<MessageDao>()

    suspend fun getChannels(query: String): Flow<SceytResponse<List<SceytChannel>>> {
        return callbackFlow {
            trySend(channelsRepository.getChannels(query))
            awaitClose()
        }.onStart {
            val dbChannels = getChannelsDb(0)
            if (dbChannels.isNotEmpty())
                emit(SceytResponse.Success(dbChannels))
        }.onEach {
            if (it is SceytResponse.Success)
                saveToDb(it.data ?: return@onEach)
        }
    }

    suspend fun loadMore(offset: Int): Flow<SceytResponse<List<SceytChannel>>> {
        return callbackFlow {
            trySend(channelsRepository.loadMoreChannels())
            awaitClose()
        }.onStart {
            val dbChannels = getChannelsDb(offset)
            if (dbChannels.isNotEmpty())
                emit(SceytResponse.Success(dbChannels))
        }.onEach {
            if (it is SceytResponse.Success)
                saveToDb(it.data ?: return@onEach)
        }
    }

    private fun getChannelsDb(offset: Int): List<SceytChannel> {
        return channelDao.getChannels(offset)
            .map { channel -> channel.toChannel() }
    }

    private fun saveToDb(list: List<SceytChannel>) {
        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()
        val lastMessages = arrayListOf<MessageEntity>()

        list.forEach { channel ->
            if (channel.isGroup) {
                (channel as SceytGroupChannel).members.forEach { member ->
                    links.add(UserChatLink(userId = member.id, chatId = channel.id, role = member.role.name))
                    users.add(member.toMemberEntity())
                    channel.lastMessage?.let {
                        lastMessages.add(it.toMessageEntity())
                    }
                }
            } else {
                val peer = (channel as SceytDirectChannel).peer ?: return
                links.add(UserChatLink(userId = peer.id, chatId = channel.id, role = peer.role.name))
                users.add(peer.toMemberEntity())
                channel.lastMessage?.let {
                    lastMessages.add(it.toMessageEntity())
                }
            }
        }

        channelDao.insertChannels(list.map { it.toChannelEntity() }, links)
        usersDao.insertUsers(*users.toTypedArray())

        messageDao.insertMessages(lastMessages)
    }
}