package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Regression tests for the last-message pointer update in [PersistenceChannelsLogicImpl.updateLastMessageWithLastRead].
 *
 * Reproduces the bug where sending many images (out-of-order send confirmations via separate
 * WorkManager workers) left the channel's `lastMessageTid` / `lastDisplayedMessageId` pointing at
 * a non-newest message. The pointer must advance to the newest message by `createdAt`, regardless
 * of confirmation order. Message `tid`s are random longs and must NOT be used for ordering.
 */
class PersistenceChannelsLogicImplLastMessageTest {

    private companion object {
        const val CHANNEL_ID = 1L
    }

    private fun logicWith(channelDao: ChannelDao, channelsCache: ChannelsCache) =
        PersistenceChannelsLogicImpl(
            context = mock(),
            channelsRepository = mock(),
            channelDao = channelDao,
            globalSearchDao = mock(),
            usersDao = mock(),
            messageDao = mock(),
            rangeDao = mock(),
            draftMessageDao = mock(),
            chatUserReactionDao = mock(),
            pendingReactionDao = mock(),
            channelsCache = channelsCache,
            channelSyncStateStore = mock(),
            pendingChannelCoordinator = mock(),
            insertChannelWithMembersUseCase = mock(),
        )

    private fun channelWithLastMessage(lastMessage: SceytMessage): SceytChannel =
        createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 0, lastMessage = lastMessage)
            .copy(lastDisplayedMessageId = lastMessage.id)

    private suspend fun cacheReturning(channel: SceytChannel): ChannelsCache {
        val cache = mock<ChannelsCache>()
        whenever(cache.getOneOf(CHANNEL_ID)).thenReturn(channel)
        return cache
    }

    private fun sentMessage(createdAt: Long, id: Long, tid: Long): SceytMessage =
        createMessage(createdAt = createdAt, id = id, tid = tid)
            .copy(deliveryStatus = MessageDeliveryStatus.Sent)

    @Test
    fun `newer message by createdAt becomes last message`() = runTest {
        val current = sentMessage(createdAt = 100, id = 10, tid = 111)
        val cache = cacheReturning(channelWithLastMessage(current))
        val dao = mock<ChannelDao>()

        val newer = sentMessage(createdAt = 200, id = 20, tid = 222)
        logicWith(dao, cache).updateLastMessageWithLastRead(CHANNEL_ID, newer)

        verifyBlocking(dao) {
            updateLastMessageWithLastRead(CHANNEL_ID, newer.tid, newer.id, newer.createdAt)
        }
    }

    @Test
    fun `older message by createdAt does not overwrite last message`() = runTest {
        val current = sentMessage(createdAt = 200, id = 20, tid = 222)
        val cache = cacheReturning(channelWithLastMessage(current))
        val dao = mock<ChannelDao>()

        // Out-of-order confirmation of an earlier image (smaller createdAt, random smaller tid).
        val older = sentMessage(createdAt = 100, id = 10, tid = 111)
        logicWith(dao, cache).updateLastMessageWithLastRead(CHANNEL_ID, older)

        verifyBlocking(dao, never()) {
            updateLastMessageWithLastRead(any(), anyOrNull(), any(), anyOrNull())
        }
    }

    @Test
    fun `confirming current last message by tid updates pointer even with lower createdAt`() = runTest {
        // Same message (same tid) confirming its own pending pointer: server createdAt may be
        // lower than the client tmp createdAt, but it must still update.
        val pendingTid = 999L
        val current = createMessage(createdAt = 500, id = 0, tid = pendingTid)
            .copy(deliveryStatus = MessageDeliveryStatus.Pending)
        val cache = cacheReturning(channelWithLastMessage(current))
        val dao = mock<ChannelDao>()

        val confirmed = sentMessage(createdAt = 400, id = 50, tid = pendingTid)
        logicWith(dao, cache).updateLastMessageWithLastRead(CHANNEL_ID, confirmed)

        verifyBlocking(dao) {
            updateLastMessageWithLastRead(CHANNEL_ID, confirmed.tid, confirmed.id, confirmed.createdAt)
        }
    }

    @Test
    fun `equal createdAt uses id as tiebreak`() = runTest {
        val current = sentMessage(createdAt = 100, id = 10, tid = 111)
        val cache = cacheReturning(channelWithLastMessage(current))
        val dao = mock<ChannelDao>()

        val sameTimeHigherId = sentMessage(createdAt = 100, id = 20, tid = 222)
        logicWith(dao, cache).updateLastMessageWithLastRead(CHANNEL_ID, sameTimeHigherId)

        verifyBlocking(dao) {
            updateLastMessageWithLastRead(
                CHANNEL_ID, sameTimeHigherId.tid, sameTimeHigherId.id, sameTimeHigherId.createdAt
            )
        }
    }
}
