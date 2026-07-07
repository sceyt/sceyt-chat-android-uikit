package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelMemberControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val memberInteractor = mock<ChannelMemberInteractor>()
    private var channel: SceytChannel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)

    private fun CoroutineScope.controller() = ChannelMemberController(
        scope = this,
        memberInteractor = memberInteractor,
        currentChannel = { channel },
        updateChannel = { action -> channel = channel.action() },
        ioDispatcher = dispatcher,
    )

    @Test
    fun `added members grow the channel members and count`() = runTest(dispatcher) {
        channel = channel.copy(members = emptyList(), memberCount = 1)
        val member = mock<SceytMember>()
        val controller = controller()

        controller.onMemberEvent(
            ChannelMembersEventData(channel, listOf(member), ChannelMembersEventEnum.Added)
        )

        assertThat(channel.memberCount).isEqualTo(2)
        assertThat(channel.members).containsExactly(member)
    }

    @Test
    fun `kicked members shrink the channel members and count`() = runTest(dispatcher) {
        val member = mock<SceytMember>()
        channel = channel.copy(members = listOf(member), memberCount = 2)
        val controller = controller()

        controller.onMemberEvent(
            ChannelMembersEventData(channel, listOf(member), ChannelMembersEventEnum.Kicked)
        )

        assertThat(channel.memberCount).isEqualTo(1)
        assertThat(channel.members).isEmpty()
    }

    @Test
    fun `loadIfNeeded loads when channel has more members than the db`() = runTest(dispatcher) {
        channel = channel.copy(memberCount = 10)
        wheneverBlocking { memberInteractor.getMembersCountFromDb(any()) }.thenReturn(3)
        whenever(memberInteractor.loadChannelMembers(any(), any(), any(), anyOrNullRole()))
            .thenReturn(emptyFlow())
        val controller = controller()

        controller.loadIfNeeded()
        advanceUntilIdle()

        verifyBlocking(memberInteractor) { getMembersCountFromDb(eq(1L)) }
        verify(memberInteractor).loadChannelMembers(eq(1L), eq(0), eq(""), anyOrNullRole())
    }

    @Test
    fun `loadIfNeeded does nothing when db already has all members`() = runTest(dispatcher) {
        channel = channel.copy(memberCount = 3)
        wheneverBlocking { memberInteractor.getMembersCountFromDb(any()) }.thenReturn(3)
        val controller = controller()

        controller.loadIfNeeded()
        advanceUntilIdle()

        verify(memberInteractor, never()).loadChannelMembers(any(), any(), any(), anyOrNullRole())
    }

    // role is a nullable String; matcher helper to keep call sites readable.
    private fun anyOrNullRole(): String? = org.mockito.kotlin.anyOrNull()
}