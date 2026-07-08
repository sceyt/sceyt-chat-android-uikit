package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageIdAndTid
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class SyncedMessageStatusUpdaterTest {

    private val messageDao = mock<MessageDao>()
    private val messagesCache = mock<MessagesCache>()
    private val updater = SyncedMessageStatusUpdater(
        messageDao = messageDao,
        messagesCache = messagesCache
    )

    @Test
    fun `displayed newest outgoing message advances previous lower statuses`() = runTest {
        whenever {
            messageDao.getMessageDeliveryStatus(CHANNEL_ID, MESSAGE_ID_3)
        }.thenReturn(MessageDeliveryStatus.Sent)
        whenever {
            messageDao.updateMessageStatusWithBefore(
                channelId = CHANNEL_ID,
                status = MessageDeliveryStatus.Displayed,
                id = MESSAGE_ID_3
            )
        }.thenReturn(
            listOf(
                MessageIdAndTid(id = MESSAGE_ID_1, tid = TID_1),
                MessageIdAndTid(id = MESSAGE_ID_2, tid = TID_2),
                MessageIdAndTid(id = MESSAGE_ID_3, tid = TID_3)
            )
        )

        updater.updatePreviousMessagesIfNeeded(
            channelId = CHANNEL_ID,
            newestMessage = message(
                id = MESSAGE_ID_3,
                tid = TID_3,
                deliveryStatus = MessageDeliveryStatus.Displayed
            )
        )

        val markersCaptor = argumentCaptor<Map<Long, SceytMarker>>()
        val statusTidsCaptor = argumentCaptor<LongArray>()
        verifyBlocking(messagesCache) {
            applyMessageMarkerChanges(
                channelId = eq(CHANNEL_ID),
                markersByTid = markersCaptor.capture(),
                status = eq(MessageDeliveryStatus.Displayed),
                statusTids = statusTidsCaptor.capture()
            )
        }
        assertThat(markersCaptor.firstValue).isEmpty()
        assertThat(statusTidsCaptor.firstValue.asList()).containsExactly(TID_1, TID_2, TID_3).inOrder()
    }

    @Test
    fun `same or newer local status skips previous status cascade`() = runTest {
        whenever {
            messageDao.getMessageDeliveryStatus(CHANNEL_ID, MESSAGE_ID_3)
        }.thenReturn(MessageDeliveryStatus.Displayed)

        updater.updatePreviousMessagesIfNeeded(
            channelId = CHANNEL_ID,
            newestMessage = message(
                id = MESSAGE_ID_3,
                tid = TID_3,
                deliveryStatus = MessageDeliveryStatus.Received
            )
        )

        verifyBlocking(messageDao, never()) {
            updateMessageStatusWithBefore(any(), any(), any())
        }
        verifyNoInteractions(messagesCache)
    }

    @Test
    fun `incoming newest message skips local status lookup`() = runTest {
        updater.updatePreviousMessagesIfNeeded(
            channelId = CHANNEL_ID,
            newestMessage = message(
                id = MESSAGE_ID_3,
                tid = TID_3,
                deliveryStatus = MessageDeliveryStatus.Displayed,
                incoming = true
            )
        )

        verifyBlocking(messageDao, never()) {
            getMessageDeliveryStatus(any(), any())
        }
        verifyBlocking(messageDao, never()) {
            updateMessageStatusWithBefore(any(), any(), any())
        }
        verifyNoInteractions(messagesCache)
    }

    private fun message(
        id: Long,
        tid: Long,
        deliveryStatus: MessageDeliveryStatus,
        incoming: Boolean = false,
    ) = SceytMessage(
        id = id,
        tid = tid,
        channelId = CHANNEL_ID,
        body = "",
        type = "",
        metadata = null,
        createdAt = id,
        updatedAt = id,
        incoming = incoming,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = deliveryStatus,
        state = MessageState.Unmodified,
        user = null,
        attachments = null,
        userReactions = null,
        reactionTotals = null,
        markerTotals = null,
        userMarkers = null,
        mentionedUsers = null,
        parentMessage = null,
        replyCount = 0,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetails = null,
        pendingReactions = null,
        bodyAttributes = null,
        disableMentionsCount = false,
        poll = null
    )

    private companion object {
        const val CHANNEL_ID = 7L
        const val MESSAGE_ID_1 = 101L
        const val MESSAGE_ID_2 = 102L
        const val MESSAGE_ID_3 = 103L
        const val TID_1 = 1L
        const val TID_2 = 2L
        const val TID_3 = 3L
    }
}
