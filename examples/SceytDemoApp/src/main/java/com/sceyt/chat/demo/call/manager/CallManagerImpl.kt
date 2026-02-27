package com.sceyt.chat.demo.call.manager

import android.content.Context
import android.util.Log
import com.callclient.CallClient
import com.callclient.call.Call
import com.callclient.call.calllisteners.CallEventsListener
import com.callclient.call.data.AudioSettings
import com.callclient.call.data.CallState
import com.callclient.call.data.CreateCallOptions
import com.callclient.call.data.JoinCallOptions
import com.callclient.call.data.Participant
import com.callclient.call.data.ParticipantConnectionState
import com.callclient.call.data.ParticipantEvent
import com.callclient.call.data.VideoSettings
import com.callclient.call.data.fold
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.audiorouting.AudioRouter
import com.sceyt.audiorouting.AudioRouterConfig
import com.sceyt.audiorouting.AudioRouterListener
import com.sceyt.audiorouting.RoutingState
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.chat.models.signal.ParticipantState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.tonemanager.audio.tone.ToneConfig
import com.sceyt.tonemanager.manager.ToneManager
import com.sceyt.tonemanager.manager.ToneManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import kotlin.reflect.KClass

/**
 * Implementation of [CallManager] that orchestrates CallClient, ToneManager, and AudioRouter SDKs.
 */
class CallManagerImpl(
    private val context: Context
) : CallManager {

    companion object {
        private const val TAG = "CallManagerImpl"
        private const val CALL_LISTENER_KEY = "call_manager_listener"
    }

    // SDK instances
    private val callClient: CallClient by lazy { CallClient.requireInstance() }
    private val toneManager: ToneManager by lazy { ToneManagerFactory.getInstance(context) }
    private val audioRouter: AudioRouter by lazy {
        AudioRouter.create(context, AudioRouterConfig(loggingEnabled = true))
    }

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State flows
    private val _callUiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    override val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState())
    override val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    override val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _remoteParticipant = MutableStateFlow<RemoteParticipantInfo?>(null)
    override val remoteParticipant: StateFlow<RemoteParticipantInfo?> =
        _remoteParticipant.asStateFlow()

    override val availableAudioDevices: StateFlow<List<AudioDevice>> = audioRouter.availableDevices
    override val selectedAudioDevice: StateFlow<AudioDevice?> = audioRouter.selectedDevice

    // Current call
    private var _currentCall: Call? = null
    override val currentCall: Call? get() = _currentCall

    // Timers and jobs
    private var durationJob: Job? = null
    private var noAnswerJob: Job? = null
    private var reconnectTimeoutJob: Job? = null
    private var endedDismissJob: Job? = null

    // State tracking
    private var reconnectAttempts = 0
    private var lastConnectedAt: Long = 0

    // ========== Call Control ==========

    override suspend fun startOutgoingCall(
        userId: String,
        channelId: Long,
        isVideo: Boolean,
        callPrepared: (Call) -> Unit
    ): Result<Call> {
        if (_callUiState.value !is CallUiState.Idle) {
            return Result.failure(IllegalStateException("Call already in progress"))
        }

        return try {
            // Fetch user info
            val userInfo = fetchUserInfo(userId)

            // Update state to Outgoing
            _callUiState.value = CallUiState.Outgoing(
                remoteUserId = userId,
                remoteUserName = userInfo?.name,
                remoteUserAvatar = userInfo?.avatar,
                isVideo = isVideo
            )

            // Set audio routing preference based on call type
            setupAudioRouting(isVideo)


            // Join call
            val result = runCatching {
                callClient.prepareCall(
                    callId = generateCallId(),
                    CreateCallOptions(
                        participantsIds = listOf(userId),
                        videoCall = isVideo,
                        mediaFlow = MediaFlow.P2P
                    )
                )
            }
            result.onSuccess { call ->
                callPrepared(call)
                // Build join options
                val joinCallOptions = JoinCallOptions.default().copy(
                    audioSettings = AudioSettings(
                        disableManageAudioRoute = true
                    ),
                    videoSettings = if (isVideo) {
                        VideoSettings(publishVideo = true)
                    } else null
                )

                setupCallListeners(call)
                _currentCall = call
                startNoAnswerTimeout()

                // Update remote participant info
                _remoteParticipant.value = userInfo
                call.join(joinCallOptions)
                playTone(ToneConfig.ringback())

                Log.d(TAG, "Outgoing call started: ${call.id}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to start outgoing call", error)
                cleanupCall()
                _callUiState.value =
                    CallUiState.Ended.Failed(error.message ?: "Failed to start call")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error starting outgoing call", e)
            cleanupCall()
            _callUiState.value = CallUiState.Ended.Failed(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override suspend fun answerIncomingCall(call: Call): Result<Unit> {
        val currentState = _callUiState.value
        if (currentState !is CallUiState.Incoming) {
            return Result.failure(IllegalStateException("No incoming call to answer"))
        }

        return try {
            // Stop ringtone
            stopTone()

            // Update state to Connecting
            _callUiState.value = CallUiState.Connecting

            // Set audio routing preference
            setupAudioRouting(currentState.isVideo)

            // Build join options
            val options = JoinCallOptions.default().copy(
                audioSettings = AudioSettings(
                    disableManageAudioRoute = true
                ),
                videoSettings = VideoSettings(
                    publishVideo = currentState.isVideo
                )
            )
            setupCallListeners(call)

            // Join call
            val result = call.join(options)

            result.fold(
                onSuccess = { call ->
                    _currentCall = call

                    // Update remote participant info
                    _remoteParticipant.value = RemoteParticipantInfo(
                        id = currentState.callerId,
                        name = currentState.callerName,
                        avatar = currentState.callerAvatar
                    )

                    Log.d(TAG, "Answered incoming call: ${call.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to answer call", error)
                    cleanupCall()
                    _callUiState.value =
                        CallUiState.Ended.Failed(error.message ?: "Failed to answer call")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
            cleanupCall()
            _callUiState.value = CallUiState.Ended.Failed(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override fun declineIncomingCall(reason: String?): Result<Unit> {
        val currentState = _callUiState.value
        if (currentState !is CallUiState.Incoming) {
            return Result.failure(IllegalStateException("No incoming call to decline"))
        }

        return try {
            currentState.call.reject(reason)
            stopTone()
            _callUiState.value = CallUiState.Idle
            Log.d(TAG, "Declined incoming call")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining call", e)
            Result.failure(e)
        }
    }

    override fun endCall(): Result<Unit> {
        val call = _currentCall ?: return Result.failure(IllegalStateException("No active call"))

        return try {
            call.leave()
            handleCallEnded(CallUiState.Ended.LocalHangup)
            Log.d(TAG, "Ended call")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
            Result.failure(e)
        }
    }

    override fun sendRinging() {
        _currentCall?.sendRinging()
    }

    // ========== Media Control ==========

    override fun toggleMute(): Boolean {
        val newMuteState = !_mediaState.value.isMuted
        _currentCall?.mute(newMuteState)
        Log.d(TAG, "Mute toggled: $newMuteState")
        return newMuteState
    }

    override fun toggleCamera(): Boolean {
        val newCameraState = !_mediaState.value.isCameraEnabled
        _currentCall?.setVideoEnabled(newCameraState)

        Log.d(TAG, "Camera toggled: $newCameraState")
        return newCameraState
    }

    override fun switchCamera(): Result<Unit> {
        return try {
            val capturer = _currentCall?.localParticipant?.getVideoTracks()?.firstOrNull()?.capturer
            capturer?.switch()
            val newFrontCamera = !_mediaState.value.isFrontCamera
            _mediaState.value = _mediaState.value.copy(isFrontCamera = newFrontCamera)
            Log.d(TAG, "Camera switched: front=$newFrontCamera")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching camera", e)
            Result.failure(e)
        }
    }

    override fun selectAudioDevice(device: AudioDevice) {
        audioRouter.selectDevice(device)
        updateSpeakerState(device)
        Log.d(TAG, "Audio device selected: ${device.name}")
    }

    override fun toggleSpeaker(): Boolean {
        val currentDevice = selectedAudioDevice.value
        val newSpeakerState = currentDevice !is AudioDevice.Speakerphone

        if (newSpeakerState) {
            val speaker = availableAudioDevices.value.filterIsInstance<AudioDevice.Speakerphone>()
                .firstOrNull()
            speaker?.let { audioRouter.selectDevice(it) }
        } else {
            // Switch to earpiece or available device
            val earpiece =
                availableAudioDevices.value.filterIsInstance<AudioDevice.Earpiece>().firstOrNull()
            val wired = availableAudioDevices.value.filterIsInstance<AudioDevice.WiredHeadset>()
                .firstOrNull()
            val bluetooth =
                availableAudioDevices.value.filterIsInstance<AudioDevice.BluetoothHeadset>()
                    .firstOrNull()
            audioRouter.selectDevice(bluetooth ?: wired ?: earpiece)
        }

        _mediaState.value = _mediaState.value.copy(isSpeakerOn = newSpeakerState)
        Log.d(TAG, "Speaker toggled: $newSpeakerState")
        return newSpeakerState
    }

    override fun clearManualAudioSelection() {
        audioRouter.clearManualSelection()
    }

    override fun refreshAudioDevices() {
        audioRouter.refreshDevices()
        Log.d(TAG, "Audio devices refreshed")
    }

    // ========== Incoming Call Handling ==========

    override suspend fun handleIncomingCall(from: String, call: Call) {
        if (_callUiState.value !is CallUiState.Idle) {
            Log.w(TAG, "Rejecting incoming call - already in a call")
            call.reject("Busy")
            return
        }

        // Fetch caller info
        val userInfo = fetchUserInfo(from)

        setupCallListeners(call)
        _callUiState.value = CallUiState.Incoming(
            callerId = from,
            callerName = userInfo?.name,
            callerAvatar = userInfo?.avatar,
            isVideo = call.videoCall,
            call = call
        )

        // Play ringtone
        playTone(ToneConfig.ring())

        call.sendRinging()
        Log.d(TAG, "Incoming call from: $from, video: ${call.videoCall}")
    }

    // ========== Lifecycle ==========

    override fun release() {
        cleanupCall()
        toneManager.stopAll()
        audioRouter.stop()
        Log.d(TAG, "CallManager released")
    }

    // ========== Private Methods ==========

    private fun setupCallListeners(call: Call) {
        call.addListener(CALL_LISTENER_KEY, object : CallEventsListener.CallAllEventsListener {

            override fun onCallStateChanged(call: Call, state: CallState) {
                Log.d(TAG, "Call state changed: $state")
                when (state) {
                    is CallState.Connecting -> {
                        if (_callUiState.value is CallUiState.Outgoing) {
                            // Already in outgoing state, wait for remote ringing
                        }
                    }

                    is CallState.Connected -> {
                        // Will be handled by participant connection state
                    }

                    is CallState.Closed -> {
                        handleCallEnded(CallUiState.Ended.RemoteHangup)
                    }

                    is CallState.Idle -> { /* Ignore */
                    }
                }
            }

            override fun onParticipantsAdded(call: Call, participants: List<Participant>) {
                Log.d(TAG, "Participants added: ${participants.map { it.id }}")
            }

            override fun onParticipantStateChanged(
                call: Call,
                participant: Participant,
                state: ParticipantState,
                reason: String?
            ) {
                Log.d(TAG, "Participant ${participant.id} state changed: $state, reason: $reason")

                // Only handle remote participant state changes
                if (participant.id == call.localParticipant.id) return

                when (state) {
                    ParticipantState.Ringing -> {
                        _remoteParticipant.value =
                            _remoteParticipant.value?.copy(ringing = true)
                    }

                    ParticipantState.Joined -> {
                        // Remote answered - state will be updated by connection state
                    }

                    ParticipantState.Left -> {
                        call.leave()
                        handleCallEnded(CallUiState.Ended.RemoteHangup)
                    }

                    ParticipantState.Declined -> {
                        call.leave()
                        handleCallEnded(CallUiState.Ended.Declined(reason))
                    }

                    ParticipantState.NoAnswer -> {
                        call.leave()
                        handleCallEnded(CallUiState.Ended.NoAnswer)
                    }

                    else -> { /* Ignore other states */
                    }
                }
            }

            override fun onParticipantConnectionStateChanged(
                call: Call,
                participant: Participant,
                state: ParticipantConnectionState
            ) {
                Log.d(TAG, "Participant ${participant.id} connection state: $state")

                // Only handle remote participant connection changes
                if (participant.id == call.localParticipant.id) return

                when (state) {
                    ParticipantConnectionState.Connecting -> {
                        _callUiState.value = CallUiState.Connecting
                        stopTone()
                    }

                    ParticipantConnectionState.Connected -> {
                        cancelNoAnswerTimeout()
                        cancelReconnectTimeout()
                        reconnectAttempts = 0
                        lastConnectedAt = System.currentTimeMillis()
                        _callUiState.value = CallUiState.Connected(connectedAt = lastConnectedAt)
                        stopTone()
                        startDurationTimer()
                    }

                    ParticipantConnectionState.Reconnecting -> {
                        handleReconnecting()
                    }

                    ParticipantConnectionState.Disconnected -> {
                        // May transition to reconnecting or ended
                        if (_callUiState.value is CallUiState.Reconnecting) {
                            // Already handling reconnection
                        } else {
                            handleCallEnded(CallUiState.Ended.Failed("Connection lost"))
                        }
                    }

                    ParticipantConnectionState.Idle -> { /* Ignore */
                    }
                }
            }

            override fun onRemoteParticipantEvent(
                call: Call,
                participant: Participant,
                event: ParticipantEvent
            ) {
                Log.d(TAG, "Participant ${participant.id} event: $event")

                // Update remote participant state
                if (participant.id != call.localParticipant.id) {
                    when (event) {
                        is ParticipantEvent.Mute -> {
                            _mediaState.value = _mediaState.value.copy(isRemoteMuted = event.muted)
                        }

                        is ParticipantEvent.Video -> {
                            _mediaState.value =
                                _mediaState.value.copy(isRemoteVideoEnabled = event.enabled)
                        }

                        is ParticipantEvent.Hold -> {
                            _remoteParticipant.value =
                                _remoteParticipant.value?.copy(isOnHold = event.hold)
                        }

                        is ParticipantEvent.ScreenShare -> {
                            // Handle screen share if needed
                        }
                    }
                }
            }

            override fun onLocalParticipantEvent(
                call: Call,
                participant: Participant,
                event: ParticipantEvent
            ) {
                when (event) {
                    is ParticipantEvent.Hold -> {
                        _mediaState.value = _mediaState.value.copy(isOnHold = event.hold)
                    }

                    is ParticipantEvent.Mute -> {
                        _mediaState.value = _mediaState.value.copy(isMuted = event.muted)
                    }

                    is ParticipantEvent.ScreenShare -> {}
                    is ParticipantEvent.Video -> {
                        _mediaState.value = _mediaState.value.copy(
                            isCameraEnabled = event.enabled,
                            localVideoTrack = if (event.enabled)
                                participant.getVideoTracks().firstOrNull()?.videoTrack else null
                        )
                    }
                }
            }

            override fun onRemoteVideoTrackAdded(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack
            ) {
                Log.d(TAG, "Remote video track added from ${participant.id}")
                _mediaState.value = _mediaState.value.copy(
                    remoteVideoTrack = videoTrack,
                    isRemoteVideoEnabled = participant.videoEnabled
                )
            }

            override fun onRemoteVideoTrackRemoved(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack
            ) {
                Log.d(TAG, "Remote video track removed from ${participant.id}")
                _mediaState.value = _mediaState.value.copy(
                    remoteVideoTrack = null,
                    isRemoteVideoEnabled = false
                )
            }

            override fun onRemoteAudioTrackAdded(
                call: Call,
                participant: Participant,
                audioTrack: org.webrtc.AudioTrack
            ) {
                Log.d(TAG, "Remote audio track added from ${participant.id}")
            }

            override fun onRemoteAudioTrackRemoved(
                call: Call,
                participant: Participant,
                audioTrack: org.webrtc.AudioTrack
            ) {
                Log.d(TAG, "Remote audio track removed from ${participant.id}")
            }

            override fun onCallMediaFlowChanged(call: Call) {
                Log.d(TAG, "Media flow changed: ${call.mediaFlow}")
            }

            override fun onActiveSpeakersChanged(
                call: Call,
                speakers: List<com.callclient.call.data.ActiveSpeakerInfo>
            ) {
                // Handle active speakers if needed
            }

            override fun onDominantSpeakerChanged(
                call: Call,
                speaker: com.callclient.call.data.ActiveSpeakerInfo?
            ) {
                // Handle dominant speaker if needed
            }

            override fun onParticipantsRemoved(
                call: Call,
                participants: List<Participant>
            ) {

            }

            override fun onSessionRenewed(call: Call) {

            }
        })
    }

    private fun handleCallEnded(reason: CallUiState.Ended) {
        Log.d(TAG, "Call ended: $reason")

        // Cancel all timers
        cancelDurationTimer()
        cancelNoAnswerTimeout()
        cancelReconnectTimeout()

        // Play hangup tone
        scope.launch {
            playTone(ToneConfig.hangup())
            delay(1000) // Let hangup tone play
            stopTone()
        }

        // Update state
        _callUiState.value = reason

        // Schedule dismiss and cleanup
        endedDismissJob?.cancel()
        endedDismissJob = scope.launch {
            delay(reason.dismissTimeoutMs)
            cleanupCall()
            _callUiState.value = CallUiState.Idle
        }
    }

    private fun handleReconnecting() {
        reconnectAttempts++
        Log.d(TAG, "Reconnecting attempt: $reconnectAttempts")

        if (reconnectAttempts > CallUiState.MAX_RECONNECT_ATTEMPTS) {
            handleCallEnded(CallUiState.Ended.Failed("Connection lost"))
            return
        }

        _callUiState.value = CallUiState.Reconnecting(
            attempt = reconnectAttempts,
            lastConnectedAt = lastConnectedAt
        )

        // Play reconnecting tone
        scope.launch { playTone(ToneConfig.reconnecting()) }

        // Start reconnect timeout
        startReconnectTimeout()
    }

    private fun cleanupCall() {
        Log.d(TAG, "Cleaning up call")

        // Remove listeners
        _currentCall?.removeListener(CALL_LISTENER_KEY)
        _currentCall = null

        audioRouter.stop()

        // Reset state
        _mediaState.value = MediaState()
        _remoteParticipant.value = null
        _callDuration.value = 0
        reconnectAttempts = 0
        lastConnectedAt = 0

        // Stop any playing tones
        scope.launch { stopTone() }
    }

    private val audioRouterListener = object : AudioRouterListener {
        override fun onAudioDevicesChanged(
            devices: List<AudioDevice>,
            selectedDevice: AudioDevice?
        ) {
            Log.d(
                TAG,
                "Audio devices changed: ${devices.map { it.name }}, selected: ${selectedDevice?.name}"
            )
            updateSpeakerState(selectedDevice)
        }

        override fun onRoutingStateChanged(state: RoutingState) {
            Log.d(TAG, "Audio routing state changed: $state")
        }

        override fun onBluetoothScoConnectionFailed(
            device: AudioDevice.BluetoothHeadset,
            fallbackDevice: AudioDevice?
        ) {
            Log.w(
                TAG,
                "Bluetooth SCO connection failed for ${device.name}, fallback: ${fallbackDevice?.name}"
            )
        }

        override fun onPermissionMissing(permission: String) {
            Log.w(TAG, "Audio routing permission missing: $permission")
        }
    }

    private fun setupAudioRouting(isVideo: Boolean) {
        // Start audio router with listener
        audioRouter.start(audioRouterListener)

        // Set device priority based on call type
        val priority: List<KClass<out AudioDevice>> = if (isVideo) {
            // Video calls: prefer speaker after Bluetooth/wired
            listOf(
                AudioDevice.BluetoothHeadset::class,
                AudioDevice.WiredHeadset::class,
                AudioDevice.Speakerphone::class,
                AudioDevice.Earpiece::class
            )
        } else {
            // Audio calls: prefer earpiece after Bluetooth/wired
            listOf(
                AudioDevice.BluetoothHeadset::class,
                AudioDevice.WiredHeadset::class,
                AudioDevice.Earpiece::class,
                AudioDevice.Speakerphone::class
            )
        }

        audioRouter.setPreferredDeviceOrder(priority)

        // Update speaker state based on initial selection
        scope.launch {
            // Small delay to let audio router initialize
            delay(100)
            updateSpeakerState(selectedAudioDevice.value)
        }

        Log.d(TAG, "Audio routing setup: video=$isVideo")
    }

    private fun updateSpeakerState(device: AudioDevice?) {
        _mediaState.value = _mediaState.value.copy(
            isSpeakerOn = device is AudioDevice.Speakerphone
        )
    }

    private suspend fun fetchUserInfo(userId: String): RemoteParticipantInfo? {
        return try {
            val result = SceytChatUIKit.chatUIFacade.userInteractor.getUserById(userId)
            result.fold(
                onSuccess = { user ->
                    user ?: return@fold null
                    RemoteParticipantInfo(
                        id = user.id,
                        name = user.getPresentableName(),
                        avatar = user.avatarURL
                    )
                },
                onError = {
                    Log.e(TAG, "Error fetching user info: ${it?.message}")
                    null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user info", e)
            null
        }
    }

    private fun generateCallId(): String {
        return "call_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // ========== Timer Methods ==========

    private fun startDurationTimer() {
        durationJob?.cancel()
        _callDuration.value = 0

        durationJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDuration.value += 1
            }
        }
    }

    private fun cancelDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    private fun startNoAnswerTimeout() {
        noAnswerJob?.cancel()
        noAnswerJob = scope.launch {
            delay(CallUiState.NO_ANSWER_TIMEOUT_MS)
            if (_callUiState.value is CallUiState.Outgoing) {
                handleCallEnded(CallUiState.Ended.NoAnswer)
            }
        }
    }

    private fun cancelNoAnswerTimeout() {
        noAnswerJob?.cancel()
        noAnswerJob = null
    }

    private fun startReconnectTimeout() {
        reconnectTimeoutJob?.cancel()
        reconnectTimeoutJob = scope.launch {
            delay(CallUiState.RECONNECT_TIMEOUT_MS)
            if (_callUiState.value is CallUiState.Reconnecting) {
                handleCallEnded(CallUiState.Ended.Failed("Reconnection timed out"))
            }
        }
    }

    private fun cancelReconnectTimeout() {
        reconnectTimeoutJob?.cancel()
        reconnectTimeoutJob = null
    }

    // ========== Tone Methods ==========

    private suspend fun playTone(config: ToneConfig) {
        try {
            toneManager.playTone(config)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tone", e)
        }
    }

    private fun stopTone() {
        scope.launch {
            try {
                toneManager.stopCurrentTone()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping tone", e)
            }
        }
    }
}
