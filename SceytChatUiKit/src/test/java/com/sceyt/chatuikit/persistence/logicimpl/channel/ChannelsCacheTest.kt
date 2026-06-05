package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.PendingReactionData
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChannelsCacheTest {
    private val facade = mock<SceytChatUIFacade>()
    private val allTypesConfig = ChannelListConfig(
        types = emptyList(),
        order = ChannelListOrder.ListQueryChannelOrderLastMessage,
        queryLimit = 20,
        queryParam = ChannelQueryParam(1, 10, 1, true)
    )
    private val directConfig = allTypesConfig.copy(types = listOf("direct"))
    private val groupConfig = allTypesConfig.copy(types = listOf("group"))

    @Before
    fun setUp() {
        stopKoin()
        whenever(facade.myId).thenReturn("me")
        SceytKoinApp.koinApp = startKoin {
            modules(module { single { facade } })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `addAll stores channels and reports whether data changed`() = runTest {
        val cache = ChannelsCache()
        val channel = channel(id = 1)

        assertThat(cache.addAll(allTypesConfig, listOf(channel), checkDifference = false)).isFalse()
        assertThat(cache.getOneOf(channel.id, allTypesConfig)).isEqualTo(channel)
        assertThat(cache.getCachedData()[allTypesConfig]?.get(channel.id)).isEqualTo(channel)

        assertThat(cache.addAll(allTypesConfig, listOf(channel), checkDifference = true)).isFalse()

        val updated = channel.copy(metadata = "Updated metadata")
        assertThat(cache.addAll(allTypesConfig, listOf(updated), checkDifference = true)).isTrue()
        assertThat(cache.getOneOf(channel.id)).isEqualTo(updated)
    }

    @Test
    fun `addAll tracks pending channels separately and getData only includes visible pending channels`() = runTest {
        val cache = ChannelsCache()
        val pendingWithoutMessage = channel(id = 2).copy(pending = true, lastMessage = null)
        val pendingWithMessage = channel(id = 3, lastMessage = createMessage(createdAt = 10, id = 10))
            .copy(pending = true)

        cache.addAll(allTypesConfig, listOf(pendingWithoutMessage, pendingWithMessage), checkDifference = false)

        assertThat(cache.isPending(pendingWithoutMessage.id)).isTrue()
        assertThat(cache.isPending(pendingWithMessage.id)).isTrue()
        assertThat(cache.getOneOf(pendingWithoutMessage.id, allTypesConfig)).isEqualTo(pendingWithoutMessage)
        assertThat(cache.getData(allTypesConfig).map { it.id }).containsExactly(pendingWithMessage.id)
    }

    @Test
    fun `addPendingChannel stores pending channel and emits added event only when it has last message`() = runTest {
        val cache = ChannelsCache()
        val hiddenPending = channel(id = 4).copy(pending = true, lastMessage = null)
        val visiblePending = channel(id = 5, lastMessage = createMessage(createdAt = 10, id = 10)).copy(pending = true)
        val addedEvent = expectFirst(ChannelsCache.channelAddedFlow) { it.id == visiblePending.id }
        yield()

        cache.addPendingChannel(hiddenPending)
        cache.addPendingChannel(visiblePending)

        assertThat(addedEvent.await()).isEqualTo(visiblePending)
        assertThat(cache.isPending(hiddenPending.id)).isTrue()
        assertThat(cache.getData(allTypesConfig).map { it.id }).containsExactly(visiblePending.id)
    }

    @Test
    fun `getSorted returns channels sorted with config order including pending visible channels`() = runTest {
        val cache = ChannelsCache()
        val oldChannel = channel(id = 6, createdAt = 1, lastMessage = createMessage(createdAt = 100, id = 100))
        val newChannel = channel(id = 7, createdAt = 2, lastMessage = createMessage(createdAt = 300, id = 300))
        val pending = channel(id = 8, createdAt = 3, lastMessage = createMessage(createdAt = 200, id = 200))
            .copy(pending = true)

        cache.addAll(allTypesConfig, listOf(oldChannel, newChannel, pending), checkDifference = false)

        assertThat(cache.getSorted(allTypesConfig).map { it.id }).containsExactly(7L, 8L, 6L).inOrder()
    }

    @Test
    fun `clear removes one config and clearAll removes cached pending and mapped channels`() = runTest {
        val cache = ChannelsCache()
        val directChannel = channel(id = 9, type = "direct")
        val groupChannel = channel(id = 10, type = "group")
        val pendingChannel = channel(id = 11, lastMessage = createMessage(createdAt = 11, id = 11))
            .copy(pending = true)

        cache.addAll(directConfig, listOf(directChannel), checkDifference = false)
        cache.addAll(groupConfig, listOf(groupChannel), checkDifference = false)
        cache.addPendingChannel(pendingChannel)

        cache.clear(directConfig)

        assertThat(cache.getData(directConfig).map { it.id }).containsExactly(pendingChannel.id)
        assertThat(cache.getData(groupConfig).map { it.id }).containsExactly(groupChannel.id)

        cache.pendingChannelCreated(pendingChannel.id, channel(id = 12))
        assertThat(cache.getRealChannelIdWithPendingChannelId(pendingChannel.id)).isEqualTo(12)

        cache.clearAll()

        assertThat(cache.getData(directConfig)).isEmpty()
        assertThat(cache.getData(groupConfig)).isEmpty()
        assertThat(cache.isPending(pendingChannel.id)).isFalse()
        assertThat(cache.getRealChannelIdWithPendingChannelId(pendingChannel.id)).isNull()
    }

    @Test
    fun `upsert adds synced channel that is missing from cache`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(id = 13)
        val syncedChannel = channel(id = 14)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val addedEvent = expectFirst(ChannelsCache.channelAddedFlow) { it.id == syncedChannel.id }
        yield()

        cache.upsertChannels(listOf(syncedChannel))

        assertThat(addedEvent.await()).isEqualTo(syncedChannel)
        assertThat(cache.getOneOf(syncedChannel.id, allTypesConfig)).isEqualTo(syncedChannel)
    }

    @Test
    fun `upsert ignores channels that do not match an existing config`() = runTest {
        val cache = ChannelsCache()
        val directChannel = channel(id = 15, type = "direct")
        val groupChannel = channel(id = 16, type = "group")
        cache.addAll(directConfig, listOf(directChannel), checkDifference = false)

        cache.upsertChannel(groupChannel)

        assertThat(cache.getOneOf(groupChannel.id, directConfig)).isNull()
    }

    @Test
    fun `upsert turns pending channel into real channel and stores pending to real mapping`() = runTest {
        val cache = ChannelsCache()
        val pendingChannel = channel(id = 17, lastMessage = createMessage(createdAt = 17, id = 17))
            .copy(pending = true)
        val realChannel = pendingChannel.copy(pending = false)
        cache.addAll(allTypesConfig, listOf(pendingChannel), checkDifference = false)
        val createdEvent = expectFirst(ChannelsCache.pendingChannelCreatedFlow) { it.first == pendingChannel.id }
        yield()

        cache.upsertChannel(realChannel)

        assertThat(createdEvent.await()).isEqualTo(pendingChannel.id to realChannel)
        assertThat(cache.isPending(pendingChannel.id)).isFalse()
        assertThat(cache.getOneOf(realChannel.id, allTypesConfig)).isEqualTo(realChannel)
        assertThat(cache.getRealChannelIdWithPendingChannelId(pendingChannel.id)).isEqualTo(realChannel.id)

        cache.removeFromPendingToRealChannelsData(pendingChannel.id)
        assertThat(cache.getRealChannelIdWithPendingChannelId(pendingChannel.id)).isNull()
    }

    @Test
    fun `newChannelsOnSync inserts non pending missing channels and emits sync event`() = runTest {
        val cache = ChannelsCache()
        val existingChannel = channel(id = 19)
        val newChannel = channel(id = 20)
        val pendingChannel = channel(id = 21).copy(pending = true)
        cache.addAll(allTypesConfig, listOf(existingChannel), checkDifference = false)
        val syncEvent = expectFirst(ChannelsCache.newChannelsOnSync) { it.first == allTypesConfig }
        yield()

        cache.newChannelsOnSync(allTypesConfig, listOf(existingChannel, newChannel, pendingChannel))

        assertThat(syncEvent.await().second.map { it.id }).containsExactly(19L, 20L, 21L).inOrder()
        assertThat(cache.getOneOf(newChannel.id, allTypesConfig)).isEqualTo(newChannel)
        assertThat(cache.getOneOf(pendingChannel.id, allTypesConfig)).isNull()
    }

    @Test
    fun `upsert emits specific update type from channel diff`() = runTest {
        assertUpsertType(channel(id = 22).copy(muted = true), ChannelUpdatedType.MuteState)
    }

    @Test
    fun `upsert emits clear history when messages cleared timestamp changes`() = runTest {
        assertUpsertType(channel(id = 23).copy(messagesClearedAt = 10), ChannelUpdatedType.ClearedHistory)
    }

    @Test
    fun `upsert emits remaining specific update types from channel diff`() = runTest {
        assertUpsertType(
            channel(id = 24).copy(lastMessage = createMessage(createdAt = 1, id = 1, tid = 1)),
            ChannelUpdatedType.LastMessage
        )
        assertUpsertType(channel(id = 25).copy(pinnedAt = 10), ChannelUpdatedType.PinnedAt)
        assertUpsertType(channel(id = 26).copy(messageRetentionPeriod = 10), ChannelUpdatedType.AutoDeleteState)
        assertUpsertType(channel(id = 27).copy(memberCount = 2, userRole = "owner"), ChannelUpdatedType.Members)
        assertUpsertType(channel(id = 28).copy(newMessageCount = 4), ChannelUpdatedType.UnreadCount)
        assertUpsertType(channel(id = 29).copy(metadata = "new metadata"), ChannelUpdatedType.Updated)
    }

    @Test
    fun `upsert emits presence only when presence is the only changed field`() = runTest {
        val cache = ChannelsCache()
        val peer = user("peer", presenceState = PresenceState.Offline)
        val base = channel(id = 23, type = "direct", members = listOf(SceytMember(peer, "participant")))
        val updatedPeer = peer.copy(presence = SceytPresence(PresenceState.Online, "", 1))
        val updated = base.copy(members = listOf(SceytMember(updatedPeer, "participant")))
        cache.addAll(allTypesConfig, listOf(base), checkDifference = false)
        val event = expectChannelUpdate(updated.id, ChannelUpdatedType.Presence)
        yield()

        cache.upsertChannel(updated)

        assertThat(event.await().eventType).isEqualTo(ChannelUpdatedType.Presence)
    }

    @Test
    fun `updateLastMessage updates channel emits LastMessage and marks sorting only when order key changes`() = runTest {
        val cache = ChannelsCache()
        val oldMessage = createMessage(createdAt = 1, id = 1, tid = 1)
        val newMessage = createMessage(createdAt = 2, id = 2, tid = 2).copy(body = "new body")
        val cachedChannel = channel(id = 24, lastMessage = oldMessage)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.LastMessage)
        yield()

        cache.updateLastMessage(cachedChannel.id, newMessage)

        val update = event.await()
        assertThat(update.channel.lastMessage).isEqualTo(newMessage)
        assertThat(update.needSorting).isTrue()
        assertThat(cache.getOneOf(cachedChannel.id)?.lastMessage).isEqualTo(newMessage)
    }

    @Test
    fun `updateLastMessageWithLastRead updates last displayed message id`() = runTest {
        val cache = ChannelsCache()
        val message = createMessage(createdAt = 1, id = 100, tid = 100)
        val cachedChannel = channel(id = 25)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.LastMessage)
        yield()

        cache.updateLastMessageWithLastRead(cachedChannel.id, message)

        val update = event.await()
        assertThat(update.channel.lastMessage).isEqualTo(message)
        assertThat(update.channel.lastDisplayedMessageId).isEqualTo(message.id)
        assertThat(cache.getOneOf(cachedChannel.id)?.lastDisplayedMessageId).isEqualTo(message.id)
    }

    @Test
    fun `clearedHistory resets message counters reactions and emits cleared history`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(
            id = 26,
            lastMessage = createMessage(createdAt = 1, id = 1),
            newReactions = listOf(reaction(messageId = 1)),
            pendingReactions = listOf(pendingReaction(messageId = 1))
        ).copy(newMessageCount = 2, newMentionCount = 3, newReactedMessageCount = 4)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.ClearedHistory)
        yield()

        cache.clearedHistory(cachedChannel.id)

        val update = event.await()
        assertThat(update.channel.lastMessage).isNull()
        assertThat(update.channel.newMessageCount).isEqualTo(0)
        assertThat(update.channel.newMentionCount).isEqualTo(0)
        assertThat(update.channel.newReactedMessageCount).isEqualTo(0)
        assertThat(update.channel.newReactions).isNull()
        assertThat(update.channel.pendingReactions).isNull()
        assertThat(update.needSorting).isTrue()
    }

    @Test
    fun `specific state update methods mutate cached channel and emit expected types`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(id = 27)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)

        expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.MuteState)
            .also { yield(); cache.updateMuteState(cachedChannel.id, muted = true, muteUntil = 50) }
            .await()
            .also {
                assertThat(it.channel.muted).isTrue()
                assertThat(it.channel.mutedTill).isEqualTo(50)
            }

        expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.AutoDeleteState)
            .also { yield(); cache.updateAutoDeleteState(cachedChannel.id, period = 60) }
            .await()
            .also { assertThat(it.channel.messageRetentionPeriod).isEqualTo(60) }

        expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.Updated)
            .also { yield(); cache.updateChannelUri(cachedChannel.id, newUri = "new-uri") }
            .await()
            .also { assertThat(it.channel.uri).isEqualTo("new-uri") }

        expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.PinnedAt)
            .also { yield(); cache.updatePinState(cachedChannel.id, pinnedAt = 70) }
            .await()
            .also {
                assertThat(it.channel.pinnedAt).isEqualTo(70)
                assertThat(it.needSorting).isTrue()
            }

        expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.Updated)
            .also { yield(); cache.onChannelMarkedAsReadOrUnread(cachedChannel.copy(unread = true, newMessageCount = 5)) }
            .await()
            .also {
                assertThat(it.channel.unread).isTrue()
                assertThat(it.channel.newMessageCount).isEqualTo(5)
            }
    }

    @Test
    fun `deleteChannel removes cached channels and emits deleted ids when at least one exists`() = runTest {
        val cache = ChannelsCache()
        val first = channel(id = 28)
        val second = channel(id = 29)
        cache.addAll(allTypesConfig, listOf(first, second), checkDifference = false)
        val event = expectFirst(ChannelsCache.channelsDeletedFlow) { it.contains(first.id) }
        yield()

        cache.deleteChannel(first.id, 999)

        assertThat(event.await()).containsExactly(first.id, 999L).inOrder()
        assertThat(cache.getOneOf(first.id)).isNull()
        assertThat(cache.getOneOf(second.id)).isEqualTo(second)
    }

    @Test
    fun `updateChannelDraftMessage updates cache and emits draft changes`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(id = 30)
        val draft = DraftMessage(
            channelId = cachedChannel.id,
            body = "draft",
            createdAt = 1,
            mentionUsers = null,
            replyOrEditMessage = null,
            isReply = false,
            bodyAttributes = null,
            attachments = null,
            voiceAttachment = null,
            viewOnce = false
        )
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectFirst(ChannelsCache.channelDraftMessageChangesFlow) { it.id == cachedChannel.id }
        yield()

        cache.updateChannelDraftMessage(cachedChannel.id, draft)

        assertThat(event.await().draftMessage).isEqualTo(draft)
        assertThat(cache.getOneOf(cachedChannel.id)?.draftMessage).isEqualTo(draft)
    }

    @Test
    fun `updateChannelPeer updates matching member and emits presence event`() = runTest {
        val cache = ChannelsCache()
        val peer = user("peer-31", firstName = "Old", presenceState = PresenceState.Offline)
        val cachedChannel = channel(
            id = 31,
            type = "direct",
            members = listOf(SceytMember(peer, "participant"))
        )
        val updatedPeer = peer.copy(firstName = "New", presence = SceytPresence(PresenceState.Online, "", 10))
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectChannelUpdate(cachedChannel.id, ChannelUpdatedType.Presence)
        yield()

        cache.updateChannelPeer(cachedChannel.id, updatedPeer)

        val update = event.await()
        assertThat(update.channel.members?.first()?.user).isEqualTo(updatedPeer)
        assertThat(update.diff.presenceStateChanged).isTrue()
        assertThat(cache.getOneOf(cachedChannel.id)?.members?.first()?.user).isEqualTo(updatedPeer)
    }

    @Test
    fun `removeChannelMessageReactions filters reactions from cached channel`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(
            id = 32,
            newReactions = listOf(reaction(id = 1, messageId = 1), reaction(id = 2, messageId = 2))
        )
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)

        cache.removeChannelMessageReactions(cachedChannel.id, messageId = 1)

        assertThat(cache.getOneOf(cachedChannel.id)?.newReactions?.map { it.messageId }).containsExactly(2L)
    }

    @Test
    fun `channelLastReactionLoaded emits channel from cache`() = runTest {
        val cache = ChannelsCache()
        val cachedChannel = channel(id = 33)
        cache.addAll(allTypesConfig, listOf(cachedChannel), checkDifference = false)
        val event = expectFirst(ChannelsCache.channelReactionMsgLoadedFlow) { it.id == cachedChannel.id }
        yield()

        cache.channelLastReactionLoaded(cachedChannel.id)

        assertThat(event.await()).isEqualTo(cachedChannel)
    }

    private suspend fun CoroutineScope.assertUpsertType(
        updatedChannel: SceytChannel,
        expectedType: ChannelUpdatedType
    ) {
        val cache = ChannelsCache()
        cache.addAll(allTypesConfig, listOf(channel(id = updatedChannel.id)), checkDifference = false)
        val event = expectChannelUpdate(updatedChannel.id, expectedType)
        yield()
        cache.upsertChannel(updatedChannel)
        assertThat(event.await().eventType).isEqualTo(expectedType)
    }

    private fun CoroutineScope.expectChannelUpdate(
        channelId: Long,
        type: ChannelUpdatedType
    ): Deferred<ChannelUpdateData> {
        return expectFirst(ChannelsCache.channelUpdatedFlow) {
            it.channel.id == channelId && it.eventType == type
        }
    }

    private fun <T> CoroutineScope.expectFirst(
        flow: Flow<T>,
        predicate: (T) -> Boolean
    ): Deferred<T> {
        return async {
            withTimeout(1_000) {
                flow.first(predicate)
            }
        }
    }

    private fun channel(
        id: Long,
        type: String = "direct",
        createdAt: Long = id,
        lastMessage: com.sceyt.chatuikit.data.models.messages.SceytMessage? = null,
        members: List<SceytMember>? = null,
        newReactions: List<SceytReaction>? = null,
        pendingReactions: List<PendingReactionData>? = null
    ): SceytChannel {
        return createChannel(id = id, pinnedAt = 0, createdAt = createdAt, lastMessage = lastMessage)
            .copy(
                type = type,
                subject = "Channel $id",
                members = members,
                newReactions = newReactions,
                pendingReactions = pendingReactions
            )
    }

    private fun user(
        id: String,
        firstName: String = id,
        presenceState: PresenceState = PresenceState.Online
    ): SceytUser {
        return SceytUser(
            id = id,
            username = id,
            firstName = firstName,
            lastName = "",
            avatarURL = "",
            metadataMap = null,
            presence = SceytPresence(presenceState, "", 0),
            state = UserState.Active,
            blocked = false
        )
    }

    private fun reaction(id: Long = 1, messageId: Long): SceytReaction {
        return SceytReaction(
            id = id,
            messageId = messageId,
            key = "like",
            score = 1,
            reason = "",
            createdAt = 1,
            user = user("reaction-user-$id"),
            pending = false
        )
    }

    private fun pendingReaction(messageId: Long): PendingReactionData {
        return PendingReactionData(
            messageId = messageId,
            key = "like",
            score = 1,
            count = 1,
            createdAt = 1,
            isAdd = true,
            incomingMsg = false
        )
    }
}
