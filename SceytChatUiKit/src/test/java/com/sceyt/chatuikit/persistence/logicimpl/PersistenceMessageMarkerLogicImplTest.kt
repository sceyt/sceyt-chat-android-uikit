package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.dao.MarkerDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageIdAndTid
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

internal class PersistenceMessageMarkerLogicImplTest {

    private val messageMarkersRepository = mock<MessageMarkersRepository>()
    private val markerDao = mock<MarkerDao>()
    private val messageDao = mock<MessageDao>()
    private val messagesCache = mock<MessagesCache>()
    private val logic = PersistenceMessageMarkerLogicImpl(
        messageMarkersRepository = messageMarkersRepository,
        markerDao = markerDao,
        messageDao = messageDao,
        messagesCache = messagesCache
    )

    @Test
    fun `status event applies marker and numeric totals to previous lower status messages`() = runTest {
        val reader = SceytUser(READER_ID)
        val upgradedMessages = listOf(
            MessageIdAndTid(id = 10L, tid = 1L),
            MessageIdAndTid(id = 20L, tid = 2L),
            MessageIdAndTid(id = 30L, tid = 3L)
        )
        whenever {
            messageDao.updateMessageStatusWithBefore(
                channelId = eq(CHANNEL_ID),
                status = eq(MessageDeliveryStatus.Displayed),
                id = eq(30L)
            )
        }.thenReturn(upgradedMessages)
        whenever {
            messageDao.getExistMessagesIdTidByIdsChunked(eq(listOf(30L)))
        }.thenReturn(listOf(MessageIdAndTid(id = 30L, tid = 3L)))
        whenever {
            messageDao.insertUserMarkersIfExistMessage(any())
        }.thenReturn(listOf(10L, 20L, 30L))
        whenever {
            messageDao.getMessageEntitiesByIdsChunked(eq(listOf(10L, 20L, 30L)))
        }.thenReturn(
            listOf(
                messageEntity(id = 10L, tid = 1L, markerCount = listOf(MarkerTotal(DISPLAYED, 2))),
                messageEntity(id = 20L, tid = 2L),
                messageEntity(id = 30L, tid = 3L)
            )
        )
        whenever { messageDao.updateMessagesIgnored(any()) }.thenReturn(3)

        logic.onMessageStatusChangeEvent(
            MessageStatusChangeData(
                channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1),
                from = reader,
                status = MessageDeliveryStatus.Displayed,
                marker = MessageListMarker(longArrayOf(30L), CHANNEL_ID, DISPLAYED, 100L)
            )
        )

        val cacheMarkersCaptor = argumentCaptor<Map<Long, SceytMarker>>()
        val statusTidsCaptor = argumentCaptor<LongArray>()
        verifyBlocking(messagesCache) {
            applyMessageMarkerChanges(
                channelId = eq(CHANNEL_ID),
                markersByTid = cacheMarkersCaptor.capture(),
                status = eq(MessageDeliveryStatus.Displayed),
                statusTids = statusTidsCaptor.capture()
            )
        }
        assertThat(cacheMarkersCaptor.firstValue.keys).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(cacheMarkersCaptor.firstValue.values.map { it.messageId }).containsExactly(10L, 20L, 30L)
        assertThat(statusTidsCaptor.firstValue.asList()).containsExactly(1L, 2L, 3L).inOrder()

        val markerRowsCaptor = argumentCaptor<List<MarkerEntity>>()
        verifyBlocking(messageDao) { insertUserMarkersIfExistMessage(markerRowsCaptor.capture()) }
        assertThat(markerRowsCaptor.firstValue.map { it.messageId }).containsExactly(10L, 20L, 30L).inOrder()
        assertThat(markerRowsCaptor.firstValue.map { it.userId }).containsExactly(READER_ID, READER_ID, READER_ID)
        assertThat(markerRowsCaptor.firstValue.map { it.name }).containsExactly(DISPLAYED, DISPLAYED, DISPLAYED)

        val updatedMessagesCaptor = argumentCaptor<List<MessageEntity>>()
        verifyBlocking(messageDao) { updateMessagesIgnored(updatedMessagesCaptor.capture()) }
        val totalsByMessageId = updatedMessagesCaptor.firstValue.associate {
            it.id to it.markerCount.orEmpty().single { total -> total.name == DISPLAYED }.count
        }
        assertThat(totalsByMessageId).containsExactly(10L, 3L, 20L, 1L, 30L, 1L)
    }

    @Test
    fun `marker event applies marker and numeric totals to each local message`() = runTest {
        val markerUser = SceytUser(MARKER_USER_ID)
        val messageIds = listOf(101L, 102L)
        whenever {
            messageDao.getMessageEntitiesByIdsChunked(eq(messageIds))
        }.thenReturn(
            listOf(
                messageEntity(id = 101L, tid = 11L, markerCount = listOf(MarkerTotal(CUSTOM_MARKER, 4))),
                messageEntity(id = 102L, tid = 12L, markerCount = listOf(MarkerTotal("other", 7)))
            )
        )
        whenever {
            messageDao.insertUserMarkersIfExistMessage(any())
        }.thenReturn(listOf(101L, 102L))
        whenever { messageDao.updateMessagesIgnored(any()) }.thenReturn(2)

        logic.onMessageMarkerEvent(
            MessageMarkerEventData(
                channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1),
                user = markerUser,
                marker = MessageListMarker(longArrayOf(101L, 102L), CHANNEL_ID, CUSTOM_MARKER, 200L)
            )
        )

        val cacheMarkersCaptor = argumentCaptor<Map<Long, SceytMarker>>()
        verifyBlocking(messagesCache) {
            applyMessageMarkerChanges(
                channelId = eq(CHANNEL_ID),
                markersByTid = cacheMarkersCaptor.capture(),
                status = isNull(),
                statusTids = any()
            )
        }
        assertThat(cacheMarkersCaptor.firstValue.keys).containsExactly(11L, 12L).inOrder()
        assertThat(cacheMarkersCaptor.firstValue.values.map { it.messageId }).containsExactly(101L, 102L)
        assertThat(cacheMarkersCaptor.firstValue.values.map { it.userId }).containsExactly(MARKER_USER_ID, MARKER_USER_ID)
        assertThat(cacheMarkersCaptor.firstValue.values.map { it.name }).containsExactly(CUSTOM_MARKER, CUSTOM_MARKER)

        val markerRowsCaptor = argumentCaptor<List<MarkerEntity>>()
        verifyBlocking(messageDao) { insertUserMarkersIfExistMessage(markerRowsCaptor.capture()) }
        assertThat(markerRowsCaptor.firstValue.map { it.messageId }).containsExactly(101L, 102L).inOrder()
        assertThat(markerRowsCaptor.firstValue.map { it.userId }).containsExactly(MARKER_USER_ID, MARKER_USER_ID)
        assertThat(markerRowsCaptor.firstValue.map { it.name }).containsExactly(CUSTOM_MARKER, CUSTOM_MARKER)

        val updatedMessagesCaptor = argumentCaptor<List<MessageEntity>>()
        verifyBlocking(messageDao) { updateMessagesIgnored(updatedMessagesCaptor.capture()) }
        val totalsByMessageId = updatedMessagesCaptor.firstValue.associate {
            it.id to it.markerCount.orEmpty().single { total -> total.name == CUSTOM_MARKER }.count
        }
        assertThat(totalsByMessageId).containsExactly(101L, 5L, 102L, 1L)
    }

    private fun messageEntity(
        id: Long,
        tid: Long,
        markerCount: List<MarkerTotal>? = null,
    ) = MessageEntity(
        tid = tid,
        id = id,
        channelId = CHANNEL_ID,
        body = "",
        type = "",
        metadata = null,
        createdAt = tid,
        updatedAt = tid,
        incoming = false,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = MessageDeliveryStatus.Sent,
        state = MessageState.Unmodified,
        fromId = null,
        markerCount = markerCount,
        mentionedUsersIds = null,
        parentId = null,
        replyCount = 0,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetailsDb = null,
        bodyAttribute = null,
        disableMentionsCount = false,
        unList = false
    )

    private companion object {
        const val CHANNEL_ID = 77L
        const val READER_ID = "reader"
        const val MARKER_USER_ID = "marker-user"
        const val CUSTOM_MARKER = "custom-marker"
        val DISPLAYED = MarkerType.Displayed.value
    }
}
