package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.mappers.getTid
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toPinnedMessageEntity
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.presentation.extensions.isDisappearing
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class StorePinsUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(channelId: Long, pins: List<SceytPinnedMessage>) = withContext(ioDispatcher) {
        val entities = pins.filter { it.message.shouldStore() }.map { pin ->
            pin.toPinnedMessageEntity(channelId, messageTid = storeMessage(pin.message))
        }
        if (entities.isEmpty()) return@withContext

        pinnedMessageDao.insertAllIfMessagesExist(entities)
        refreshPinnedMessageCache(channelId, *entities.map { it.messageTid }.toLongArray())
    }

    private suspend fun storeMessage(message: SceytMessage): Long {
        val existing = messageDao.getMessageById(message.id)?.messageEntity
        val tid = existing?.tid ?: getTid(message.id, message.tid, message.incoming)
        val parent = message.parentMessage?.takeIf { it.id != 0L }
        userDao.insertUsersWithMetadata(listOfNotNull(message.user, parent?.user).map { it.toUserDb() })
        parent?.let {
            messageDao.insertMessagesIgnored(listOf(it.toMessageDb(unList = true)))
        }
        if (existing != null && message.updatedAt <= existing.updatedAt) return tid

        messageDao.upsertMessage(message.copy(tid = tid).toMessageDb(unList = existing?.unList ?: true))
        return tid
    }

    private fun SceytMessage.shouldStore() = id != 0L && !isDisappearing() &&
            state != MessageState.Deleted && state != MessageState.DeletedHard
}
