package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.extensions.getPrintableStackTrace
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import org.koin.core.component.inject

internal class InsertChannelWithMembersUseCase(
    private val channelDao: ChannelDao,
    private val usersDao: UserDao,
) : SceytKoinComponent {
    // Lazy, because PersistenceMessagesLogic injects PersistenceChannelsLogic back.
    private val messageLogic: PersistenceMessagesLogic by inject()

    suspend operator fun invoke(
        channel: SceytChannel,
        members: List<SceytMember> = channel.members.orEmpty(),
    ) {
        if (members.isEmpty()) {
            SceytLog.w(
                tag = TAG,
                message = "Warning insert Channel with empty members ${channel.id}, trace: \n ${getPrintableStackTrace()}"
            )
        }
        var users = members.map { it.toUserDb() }
        channel.lastMessage?.let { message ->
            message.userReactions?.mapNotNull { it.user?.toUserDb() }?.let { userList ->
                users = users.plus(userList)
            }
            messageLogic.saveChannelLastMessagesToDb(listOf(message))
        }
        usersDao.insertUsersWithMetadata(users)
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), members.map {
            UserChatLinkEntity(userId = it.id, chatId = channel.id, role = it.role.name)
        })
    }

    private companion object {
        const val TAG = "PersistenceChannelsLogic"
    }
}