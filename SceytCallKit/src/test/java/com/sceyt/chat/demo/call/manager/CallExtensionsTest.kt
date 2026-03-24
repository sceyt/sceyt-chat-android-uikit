package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call
import com.callclient.call.data.Participant
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.signal.MediaFlow
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CallExtensionsTest {

    @Test
    fun `sfu call is recognized as group`() {
        val call = mockCall(mediaFlow = MediaFlow.SFU)

        assertThat(call.isGroupCall).isTrue()
        assertThat(call.isDirectCall).isFalse()
    }

    @Test
    fun `channel metadata is extracted from call`() {
        val call = mockCall(
            mediaFlow = MediaFlow.SFU,
            metadata = mapOf(
                GroupCallMetadata.CHANNEL_ID to "42",
                GroupCallMetadata.CHANNEL_NAME to "Annual Meeting",
            )
        )

        assertThat(call.channelIdOrNull).isEqualTo(42L)
        assertThat(call.channelSubjectOrNull).isEqualTo("Annual Meeting")
    }

    @Test
    fun `primary remote user id comes from first remote participant`() {
        val call = mockCall(mediaFlow = MediaFlow.P2P, remoteIds = listOf("alice", "bob"))

        assertThat(call.primaryRemoteUserIdOrNull).isEqualTo("alice")
    }

    @Test
    fun `group title prefers channel subject`() {
        val call = mockCall(
            mediaFlow = MediaFlow.SFU,
            metadata = mapOf(GroupCallMetadata.CHANNEL_NAME to "Design Review"),
        )

        val title = call.displayTitle(
            participants = listOf(
                CallParticipantUiState(userId = "me", isSelf = true, name = "You"),
                CallParticipantUiState(userId = "alice", name = "Alice"),
            )
        )

        assertThat(title).isEqualTo("Design Review")
    }

    @Test
    fun `direct title uses remote participant name`() {
        val call = mockCall(mediaFlow = MediaFlow.P2P, remoteIds = listOf("alice"))

        val title = call.displayTitle(
            participants = listOf(
                CallParticipantUiState(userId = "me", isSelf = true, name = "You"),
                CallParticipantUiState(userId = "alice", name = "Alice"),
            )
        )

        assertThat(title).isEqualTo("Alice")
    }

    private fun mockCall(
        mediaFlow: MediaFlow,
        metadata: Map<String, String> = emptyMap(),
        remoteIds: List<String> = listOf("remote"),
    ): Call {
        val call = mock<Call>()
        whenever(call.mediaFlow).thenReturn(mediaFlow)
        whenever(call.metadata).thenReturn(metadata)
        whenever(call.videoCall).thenReturn(false)
        whenever(call.getRemoteParticipants()).thenReturn(
            remoteIds.map { remoteId ->
                Participant(
                    id = remoteId,
                    clientId = "$remoteId-client"
                )
            }
        )
        return call
    }
}
