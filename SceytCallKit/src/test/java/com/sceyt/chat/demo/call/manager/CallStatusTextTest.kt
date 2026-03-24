package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call
import com.callclient.call.data.Participant
import com.callclient.call.data.ParticipantConnectionState
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.signal.MediaFlow
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CallStatusTextTest {

    @Test
    fun `direct outgoing with remote ringing shows ringing`() {
        val state = CallUiState(
            phase = CallUiState.CallPhase.Outgoing,
            call = mockCall(MediaFlow.P2P),
            isRemoteRinging = true,
        )

        assertThat(state.resolveStatusText("00:00")).isEqualTo("ringing…")
    }

    @Test
    fun `group connected without connected remotes waits for others`() {
        val state = CallUiState(
            phase = CallUiState.CallPhase.Connected,
            call = mockCall(MediaFlow.SFU),
            participants = listOf(
                CallParticipantUiState(userId = "me", isSelf = true, connectionState = ParticipantConnectionState.Connected),
                CallParticipantUiState(userId = "alice", connectionState = ParticipantConnectionState.Connecting),
            ),
        )

        assertThat(state.resolveStatusText("00:56")).isEqualTo("Waiting for others…")
    }

    @Test
    fun `group connected with connected remote shows timer`() {
        val state = CallUiState(
            phase = CallUiState.CallPhase.Connected,
            call = mockCall(MediaFlow.SFU),
            participants = listOf(
                CallParticipantUiState(userId = "me", isSelf = true, connectionState = ParticipantConnectionState.Connected),
                CallParticipantUiState(userId = "alice", connectionState = ParticipantConnectionState.Connected),
            ),
        )

        assertThat(state.resolveStatusText("00:56")).isEqualTo("00:56")
    }

    private fun mockCall(mediaFlow: MediaFlow): Call {
        val call = mock<Call>()
        whenever(call.mediaFlow).thenReturn(mediaFlow)
        whenever(call.metadata).thenReturn(emptyMap())
        whenever(call.videoCall).thenReturn(false)
        whenever(call.getRemoteParticipants()).thenReturn(listOf(mock<Participant>()))
        return call
    }
}
