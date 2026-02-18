package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call
import com.sceyt.audiorouting.AudioDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Main interface for call management that orchestrates CallClient, ToneManager, and AudioRouter SDKs.
 * Provides a unified API for the UI layer to interact with calling functionality.
 *
 * This is a singleton that enforces single active call only.
 */
interface CallManager {

    // ========== State Observation ==========

    /**
     * Current call UI state. Observe this to update UI.
     */
    val callUiState: StateFlow<CallUiState>

    /**
     * Current media state (mute, camera, speaker, video tracks).
     */
    val mediaState: StateFlow<MediaState>

    /**
     * Call duration in seconds since connected.
     * Only updates when state is [CallUiState.Connected].
     */
    val callDuration: StateFlow<Long>

    /**
     * Available audio devices (Bluetooth, Wired, Earpiece, Speaker).
     * Proxied from AudioRouter SDK.
     */
    val availableAudioDevices: StateFlow<List<AudioDevice>>

    /**
     * Currently selected audio device.
     * Proxied from AudioRouter SDK.
     */
    val selectedAudioDevice: StateFlow<AudioDevice?>

    /**
     * Remote participant info (name, avatar) for display.
     */
    val remoteParticipant: StateFlow<RemoteParticipantInfo?>

    /**
     * Current active Call object, if any.
     */
    val currentCall: Call?

    // ========== Call Control ==========

    /**
     * Start an outgoing call to the specified user.
     *
     * @param userId The remote user ID to call
     * @param channelId The channel ID for context (used to fetch user info)
     * @param isVideo Whether to start with video enabled
     * @return Result containing the Call object on success
     */
    suspend fun startOutgoingCall(
        userId: String,
        channelId: Long,
        isVideo: Boolean
    ): Result<Call>

    /**
     * Answer an incoming call.
     * Only valid when state is [CallUiState.Incoming].
     *
     * @return Result indicating success or failure
     */
    suspend fun answerIncomingCall(call: Call): Result<Unit>

    /**
     * Decline an incoming call.
     * Only valid when state is [CallUiState.Incoming].
     *
     * @param reason Optional reason for declining
     * @return Result indicating success or failure
     */
    fun declineIncomingCall(reason: String? = null): Result<Unit>

    /**
     * End the current call.
     * Valid in any active call state.
     *
     * @return Result indicating success or failure
     */
    fun endCall(): Result<Unit>

    /**
     * Send ringing signal to caller.
     * Should be called when incoming call UI is displayed.
     */
    fun sendRinging()

    // ========== Media Control ==========

    /**
     * Toggle microphone mute state.
     *
     * @return New mute state (true = muted)
     */
    fun toggleMute(): Boolean

    /**
     * Toggle camera enabled state.
     *
     * @return New camera state (true = enabled)
     */
    fun toggleCamera(): Boolean

    /**
     * Switch between front and back camera.
     *
     * @return Result indicating success or failure
     */
    fun switchCamera(): Result<Unit>

    /**
     * Select a specific audio output device.
     *
     * @param device The audio device to route audio to
     */
    fun selectAudioDevice(device: AudioDevice)

    /**
     * Toggle between speaker and earpiece/headset.
     *
     * @return New speaker state (true = speaker on)
     */
    fun toggleSpeaker(): Boolean

    /**
     * Clear manual audio device selection and return to automatic routing.
     */
    fun clearManualAudioSelection()

    /**
     * Refresh the list of available audio devices.
     * Call this after Bluetooth permission is granted or when devices may have changed.
     */
    fun refreshAudioDevices()

    // ========== Lifecycle ==========

    /**
     * Handle incoming call from push notification or SDK callback.
     * Called by the Application class when a call invitation is received.
     *
     * @param from The caller's user ID
     * @param call The incoming Call object from SDK
     */
    suspend fun handleIncomingCall(from: String, call: Call)

    /**
     * Release all resources.
     * Should be called when the app is being destroyed.
     */
    fun release()
}
