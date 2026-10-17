package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageDeleteByTidDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.mappers.toChannel

internal class MigratePendingChannelToRealChannelUseCase(
    private val channelDao: ChannelDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val rangeDao: LoadRangeDao,
    private val draftMessageDao: DraftMessageDao,
    private val pendingReactionDao: PendingReactionDao,
    private val pendingMessageStateDao: PendingMessageStateDao,
    private val pendingMessageDeleteByTidDao: PendingMessageDeleteByTidDao,
    private val channelsCache: ChannelsCache,
    private val messagesCache: MessagesCache,
    private val channelSyncStateStore: ChannelSyncStateStore
) {
    private val tag = "PendingChannelMigration"

    suspend operator fun invoke(
        pendingChannel: SceytChannel,
        realChannel: SceytChannel
    ): SceytChannel {
        val pendingChannelId = pendingChannel.id
        val realChannelId = realChannel.id
        val pendingLastMessage = pendingChannel.lastMessage?.copy(channelId = realChannelId)
        val lastMessage = newerLastMessage(realChannel.lastMessage, pendingLastMessage)

        messageDao.updateMessagesChannelId(pendingChannelId, realChannelId)
        attachmentDao.updateAttachmentsChannelId(pendingChannelId, realChannelId)
        messagesCache.moveMessagesToNewChannel(pendingChannelId, realChannelId)
        moveDraftToChannel(pendingChannelId, realChannelId)
        pendingReactionDao.updateChannelId(pendingChannelId, realChannelId)
        pendingMessageStateDao.updateChannelId(pendingChannelId, realChannelId)
        pendingMessageDeleteByTidDao.updateChannelId(pendingChannelId, realChannelId)

        if (lastMessage != realChannel.lastMessage) {
            channelDao.updateLastMessage(
                channelId = realChannelId,
                lastMessageTid = lastMessage?.tid,
                lastMessageAt = lastMessage?.createdAt
            )
        }

        channelDao.deleteChannelAndLinks(pendingChannelId)
        rangeDao.deleteChannelLoadRanges(pendingChannelId)
        channelSyncStateStore.deleteSyncState(pendingChannelId)

        val mergedChannel = channelDao.getChannelById(realChannelId)?.toChannel()
            ?: realChannel.copy(lastMessage = lastMessage)
        channelsCache.pendingChannelCreated(pendingChannelId, mergedChannel)
        SceytLog.i(
            tag = tag,
            message = "Pending channel migrated: pendingId=$pendingChannelId, realId=$realChannelId"
        )
        return mergedChannel
    }

    private suspend fun moveDraftToChannel(
        pendingChannelId: Long,
        realChannelId: Long
    ) {
        val pendingDraft = draftMessageDao.getDraftByChannelId(pendingChannelId)
            ?: return
        val realDraft = draftMessageDao.getDraftByChannelId(realChannelId)
        val shouldMovePendingDraft = realDraft == null ||
                pendingDraft.draftMessageEntity.createdAt >= realDraft.draftMessageEntity.createdAt

        if (shouldMovePendingDraft) {
            draftMessageDao.deleteDraftByChannelId(realChannelId)
            draftMessageDao.updateDraftChannelId(
                oldChatId = pendingChannelId,
                newChatId = realChannelId
            )
        } else {
            draftMessageDao.deleteDraftByChannelId(pendingChannelId)
        }
    }

    private fun newerLastMessage(
        currentLastMessage: SceytMessage?,
        pendingLastMessage: SceytMessage?
    ): SceytMessage? {
        return when {
            pendingLastMessage == null -> currentLastMessage
            currentLastMessage == null -> pendingLastMessage
            pendingLastMessage.createdAt > currentLastMessage.createdAt -> pendingLastMessage
            else -> currentLastMessage
        }
    }
}
