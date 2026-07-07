package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesCacheTest {

    @Test
    fun applyMessageMarkerChanges_mapsBatchMarkersPerMessageAndEmitsOnce() = runTest {
        val cache = MessagesCache()
        val events = mutableListOf<Pair<ChannelId, List<SceytMessage>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            MessagesCache.messageUpdatedFlow
                .filter { it.first == CHANNEL_ID }
                .collect(events::add)
        }
        cache.addAll(
            channelId = CHANNEL_ID,
            list = listOf(message(tid = TID_1, id = MESSAGE_ID_1), message(tid = TID_2, id = MESSAGE_ID_2)),
            checkDifference = false,
            checkDiffAndNotifyUpdate = false
        )
        val marker1 = marker(MESSAGE_ID_1)
        val marker2 = marker(MESSAGE_ID_2)

        cache.applyMessageMarkerChanges(
            channelId = CHANNEL_ID,
            markersByTid = mapOf(TID_1 to marker1, TID_2 to marker2),
            status = MessageDeliveryStatus.Displayed,
            statusTids = longArrayOf(TID_1, TID_2)
        )
        runCurrent()

        assertThat(events).hasSize(1)
        val updated1 = requireNotNull(cache.get(CHANNEL_ID, TID_1))
        val updated2 = requireNotNull(cache.get(CHANNEL_ID, TID_2))
        assertThat(updated1.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
        assertThat(updated2.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
        assertThat(updated1.userMarkers).containsExactly(marker1)
        assertThat(updated2.userMarkers).containsExactly(marker2)
        assertThat(updated1.markerTotals?.single()?.count).isEqualTo(1)
        assertThat(updated2.markerTotals?.single()?.count).isEqualTo(1)
    }

    @Test
    fun applyMessageMarkerChanges_doesNotEmitWhenMarkerAlreadyApplied() = runTest {
        val cache = MessagesCache()
        val events = mutableListOf<Pair<ChannelId, List<SceytMessage>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            MessagesCache.messageUpdatedFlow
                .filter { it.first == DUPLICATE_CHANNEL_ID }
                .collect(events::add)
        }
        cache.addAll(
            channelId = DUPLICATE_CHANNEL_ID,
            list = listOf(message(tid = TID_1, id = MESSAGE_ID_1, channelId = DUPLICATE_CHANNEL_ID)),
            checkDifference = false,
            checkDiffAndNotifyUpdate = false
        )
        val marker = marker(MESSAGE_ID_1)

        cache.applyMessageMarkerChanges(
            channelId = DUPLICATE_CHANNEL_ID,
            markersByTid = mapOf(TID_1 to marker)
        )
        cache.applyMessageMarkerChanges(
            channelId = DUPLICATE_CHANNEL_ID,
            markersByTid = mapOf(TID_1 to marker)
        )
        runCurrent()

        val updated = requireNotNull(cache.get(DUPLICATE_CHANNEL_ID, TID_1))
        assertThat(events).hasSize(1)
        assertThat(updated.userMarkers).containsExactly(marker)
        assertThat(updated.markerTotals?.single()?.count).isEqualTo(1)
    }

    @Test
    fun applyMessageMarkerChanges_updatesStatusTidsWithoutMarkers() = runTest {
        val cache = MessagesCache()
        val events = mutableListOf<Pair<ChannelId, List<SceytMessage>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            MessagesCache.messageUpdatedFlow
                .filter { it.first == STATUS_ONLY_CHANNEL_ID }
                .collect(events::add)
        }
        cache.addAll(
            channelId = STATUS_ONLY_CHANNEL_ID,
            list = listOf(
                message(tid = TID_1, id = MESSAGE_ID_1, channelId = STATUS_ONLY_CHANNEL_ID),
                message(tid = TID_2, id = MESSAGE_ID_2, channelId = STATUS_ONLY_CHANNEL_ID)
            ),
            checkDifference = false,
            checkDiffAndNotifyUpdate = false
        )
        val marker = marker(MESSAGE_ID_1)

        cache.applyMessageMarkerChanges(
            channelId = STATUS_ONLY_CHANNEL_ID,
            markersByTid = mapOf(TID_1 to marker),
            status = MessageDeliveryStatus.Displayed,
            statusTids = longArrayOf(TID_1, TID_2)
        )
        runCurrent()

        val marked = requireNotNull(cache.get(STATUS_ONLY_CHANNEL_ID, TID_1))
        val statusOnly = requireNotNull(cache.get(STATUS_ONLY_CHANNEL_ID, TID_2))
        assertThat(events).hasSize(1)
        assertThat(events.single().second.map { it.tid }).containsExactly(TID_1, TID_2)
        assertThat(marked.userMarkers).containsExactly(marker)
        assertThat(statusOnly.userMarkers).isNull()
        assertThat(statusOnly.deliveryStatus).isEqualTo(MessageDeliveryStatus.Displayed)
    }

    private fun marker(messageId: Long) = SceytMarker(
        messageId = messageId,
        userId = USER_ID,
        user = SceytUser(USER_ID),
        name = MarkerType.Displayed.value,
        createdAt = 10L
    )

    private fun message(
        tid: Long,
        id: Long,
        channelId: Long = CHANNEL_ID,
    ) = SceytMessage(
        id = id,
        tid = tid,
        channelId = channelId,
        body = "",
        type = "",
        metadata = null,
        createdAt = 0L,
        updatedAt = 0L,
        incoming = false,
        isTransient = false,
        silent = false,
        viewOnce = false,
        deliveryStatus = MessageDeliveryStatus.Sent,
        state = MessageState.Unmodified,
        user = null,
        attachments = null,
        userReactions = null,
        reactionTotals = null,
        markerTotals = null,
        userMarkers = null,
        mentionedUsers = null,
        parentMessage = null,
        replyCount = 0L,
        displayCount = 0,
        autoDeleteAt = null,
        forwardingDetails = null,
        pendingReactions = null,
        bodyAttributes = null,
        disableMentionsCount = false,
        poll = null
    )

    private companion object {
        const val CHANNEL_ID = 1001L
        const val DUPLICATE_CHANNEL_ID = 1002L
        const val STATUS_ONLY_CHANNEL_ID = 1003L
        const val TID_1 = 11L
        const val TID_2 = 12L
        const val MESSAGE_ID_1 = 101L
        const val MESSAGE_ID_2 = 102L
        const val USER_ID = "user-1"
    }
}
