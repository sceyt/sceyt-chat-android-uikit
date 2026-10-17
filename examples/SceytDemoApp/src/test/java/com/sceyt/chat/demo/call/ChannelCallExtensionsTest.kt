package com.sceyt.chat.demo.call

import com.callclient.call.Call
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChannelCallExtensionsTest {

    @Test
    fun `group call excludes self blank ids and duplicate members`() {
        val channel = channel(
            isGroup = true,
            memberIds = listOf("self", "alice", "", "bob", "alice", " "),
        )

        val options = requireNotNull(channel.toCreateCallOptions(true, "self"))

        assertThat(options.participantsIds).containsExactly("alice", "bob").inOrder()
        assertThat(options.mediaFlow).isEqualTo(MediaFlow.SFU)
        assertThat(options.videoCall).isTrue()
        assertThat(options.metadata).containsExactly(
            "channel_id", "42",
            "channel_name", "Design review",
        )
    }

    @Test
    fun `direct call uses one remote peer and p2p without group metadata`() {
        val channel = channel(isGroup = false, memberIds = listOf("self", "alice"))

        val options = requireNotNull(channel.toCreateCallOptions(false, "self"))

        assertThat(options.participantsIds).containsExactly("alice")
        assertThat(options.mediaFlow).isEqualTo(MediaFlow.P2P)
        assertThat(options.videoCall).isFalse()
        assertThat(options.metadata).isEmpty()
    }

    @Test
    fun `channel without remote members cannot create a call`() {
        val channel = channel(isGroup = true, memberIds = listOf("self", ""))

        assertThat(channel.toCreateCallOptions(false, "self")).isNull()
    }

    @Test
    fun `channel call can be found through legacy channel metadata`() {
        val call = mock<Call>()
        whenever(call.metadata).thenReturn(mapOf("channel_id" to "42"))

        assertThat(call.channelIdOrNull).isEqualTo(42L)
    }

    @Test
    fun `call without channel metadata has no channel id`() {
        val call = mock<Call>()
        whenever(call.metadata).thenReturn(null)

        assertThat(call.channelIdOrNull).isNull()
    }

    @Test
    fun `invalid channel metadata has no channel id`() {
        val call = mock<Call>()
        whenever(call.metadata).thenReturn(mapOf("channel_id" to "invalid"))

        assertThat(call.channelIdOrNull).isNull()
    }

    private fun channel(isGroup: Boolean, memberIds: List<String>): SceytChannel {
        val members = memberIds.map { id ->
            mock<SceytMember>().also { whenever(it.id).thenReturn(id) }
        }
        return mock<SceytChannel>().also {
            whenever(it.id).thenReturn(42L)
            whenever(it.isGroup).thenReturn(isGroup)
            whenever(it.members).thenReturn(members)
            whenever(it.subject).thenReturn("Design review")
        }
    }
}
