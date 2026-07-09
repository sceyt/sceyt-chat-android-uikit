package com.sceyt.chat.demo.call.ui

import com.callclient.call.Call
import com.google.common.truth.Truth.assertThat
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.chat.demo.call.manager.CallParticipantUiState
import com.sceyt.chat.demo.call.manager.CallUiState
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CallProximityPolicyTest {

    @Test
    fun `connected audio call on earpiece enables proximity`() {
        val state = callState(phase = CallUiState.CallPhase.Connected)

        val enabled = shouldEnableProximityWakeLock(
            callState = state,
            selectedAudioDevice = AudioDevice.Earpiece(),
            isInPictureInPictureMode = false,
        )

        assertThat(enabled).isTrue()
    }

    @Test
    fun `incoming call does not enable proximity`() {
        val state = callState(phase = CallUiState.CallPhase.Incoming)

        val enabled = shouldEnableProximityWakeLock(
            callState = state,
            selectedAudioDevice = AudioDevice.Earpiece(),
            isInPictureInPictureMode = false,
        )

        assertThat(enabled).isFalse()
    }

    @Test
    fun `speaker route does not enable proximity`() {
        val state = callState(phase = CallUiState.CallPhase.Connected)

        val enabled = shouldEnableProximityWakeLock(
            callState = state,
            selectedAudioDevice = AudioDevice.Speakerphone(),
            isInPictureInPictureMode = false,
        )

        assertThat(enabled).isFalse()
    }

    @Test
    fun `video call does not enable proximity`() {
        val state = callState(
            phase = CallUiState.CallPhase.Connected,
            isVideoCall = true,
        )

        val enabled = shouldEnableProximityWakeLock(
            callState = state,
            selectedAudioDevice = AudioDevice.Earpiece(),
            isInPictureInPictureMode = false,
        )

        assertThat(enabled).isFalse()
    }

    @Test
    fun `pip mode does not enable proximity`() {
        val state = callState(phase = CallUiState.CallPhase.Connected)

        val enabled = shouldEnableProximityWakeLock(
            callState = state,
            selectedAudioDevice = AudioDevice.Earpiece(),
            isInPictureInPictureMode = true,
        )

        assertThat(enabled).isFalse()
    }

    private fun callState(
        phase: CallUiState.CallPhase,
        isVideoCall: Boolean = false,
    ): CallUiState {
        return CallUiState(
            phase = phase,
            call = mockCall(isVideoCall),
            localParticipant = CallParticipantUiState(userId = "me", isSelf = true),
            remoteParticipants = listOf(CallParticipantUiState(userId = "remote")),
        )
    }

    private fun mockCall(isVideoCall: Boolean): Call {
        val call = mock<Call>()
        whenever(call.videoCall).thenReturn(isVideoCall)
        return call
    }
}
