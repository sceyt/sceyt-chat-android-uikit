package com.sceyt.chat.demo.call.manager

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
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
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.CallUiState.EndedReason
import com.sceyt.chat.demo.call.ui.CallActivity
import com.sceyt.chat.demo.call.worker.IncomingCallWorker
import com.sceyt.chat.demo.call.worker.OngoingCallWorker
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.chat.models.signal.ParticipantState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.isAppOnForeground
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.VideoTrack
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [CallManager] that orchestrates CallClient, ToneManager, and AudioRouter SDKs.
 */
class CallManagerImpl(
    private val context: Context,
    private val connectionProvider: SceytConnectionProvider
) : CallManager {

    companion object {
        private const val TAG = "CallManagerImpl"
        private const val CALL_LISTENER_KEY = "call_manager_listener"
        private const val CALL_CLIENT_LISTENER_KEY = "app_call_listener"
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
    private val _callUiState = MutableStateFlow(CallUiState.IDLE)
    override val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState())
    override val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    override val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

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

    // Last outgoing call info for "Call Again" feature
    private var lastOutgoingUserId: String? = null
    private var lastOutgoingIsVideo: Boolean = false

    override fun init() {
        // Register listener for incoming calls
        callClient.addListener(CALL_CLIENT_LISTENER_KEY, object : CallClient.ClientListener {
            override fun onInvitedToCall(from: String, call: Call) {
                Log.d(TAG, "Invited to call from: $from, callId: ${call.id}")
                handleIncomingCall(from, call)
            }
        })
    }

    // ========== Call Control ==========

    override suspend fun startOutgoingCall(
        userId: String,
        isVideo: Boolean,
        isCallAgain: Boolean,
        callPrepared: (Call) -> Unit
    ): Result<Call> {
        if (!_callUiState.value.phase.canAnswerOrMakeCall() && !isCallAgain) {
            return Result.failure(IllegalStateException("Call already in progress"))
        }

        return try {
            endedDismissJob?.cancel()

            lastOutgoingUserId = userId
            lastOutgoingIsVideo = isVideo

            _callUiState.update {
                CallUiState(
                    phase = CallPhase.Outgoing,
                    remoteUserId = userId,
                    isVideo = isVideo
                )
            }

            // Fetch user info in background and update state when ready
            scope.launch {
                fetchUserInfo(userId)?.let { userInfo ->
                    _callUiState.update { state ->
                        if (state.remoteUserId == userId)
                            state.copy(
                                remoteUserName = userInfo.name,
                                remoteUserAvatar = userInfo.avatar
                            )
                        else state
                    }
                }
            }

            setupAudioRouting(isVideo)

            val result = runCatching {
                callClient.prepareCall(
                    callId = UUID.randomUUID().toString(),
                    CreateCallOptions(
                        participantsIds = listOf(userId),
                        videoCall = isVideo,
                        mediaFlow = MediaFlow.P2P
                    )
                )
            }
            result.onSuccess { call ->
                callPrepared(call)

                OngoingCallWorker.start(context)

                val joinCallOptions = JoinCallOptions.default().copy(
                    audioSettings = AudioSettings(disableManageAudioRoute = true),
                    videoSettings = if (isVideo) VideoSettings(publishVideo = true) else null
                )

                setupCallListeners(call)
                _currentCall = call
                startNoAnswerTimeout()
                call.join(joinCallOptions)
                playTone(ToneConfig.ringback())

                Log.d(TAG, "Outgoing call started: ${call.id}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to start outgoing call", error)
                cleanupCall()
                setEndedState(EndedReason.Failed(error.message ?: "Failed to start call"))
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error starting outgoing call", e)
            cleanupCall()
            setEndedState(EndedReason.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    override suspend fun answerIncomingCall(): Result<Unit> {
        val currentState = _callUiState.value
        if (currentState.phase != CallPhase.Incoming) {
            return Result.failure(IllegalStateException("No incoming call to answer"))
        }
        val call = currentState.incomingCall
            ?: return Result.failure(IllegalStateException("Missing Call object in state"))

        return try {
            stopTone()

            // Transition to Connecting — user info is preserved via .update { it.copy() }
            _callUiState.update {
                it.copy(
                    phase = CallPhase.Connecting,
                    incomingCall = null
                )
            }

            // Start active worker (PHONE_CALL + MICROPHONE); IncomingCallWorker auto-stops
            OngoingCallWorker.start(context)

            setupAudioRouting(currentState.isVideo)

            val options = JoinCallOptions.default().copy(
                audioSettings = AudioSettings(disableManageAudioRoute = true),
                videoSettings = VideoSettings(publishVideo = currentState.isVideo)
            )
            setupCallListeners(call)

            val result = call.join(options)

            result.fold(
                onSuccess = { answeredCall ->
                    _currentCall = answeredCall
                    Log.d(TAG, "Answered incoming call: ${answeredCall.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to answer call", error)
                    cleanupCall()
                    setEndedState(EndedReason.Failed(error.message ?: "Failed to answer call"))
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
            cleanupCall()
            setEndedState(EndedReason.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    override fun declineIncomingCall(reason: String?): Result<Unit> {
        val currentState = _callUiState.value
        if (currentState.phase != CallPhase.Incoming) {
            return Result.failure(IllegalStateException("No incoming call to decline"))
        }
        val call = currentState.incomingCall
            ?: return Result.failure(IllegalStateException("Missing Call object in state"))

        return try {
            call.reject(reason)
            stopTone()
            endedDismissJob?.cancel()
            _callUiState.update { CallUiState.IDLE }
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
            handleCallEnded(EndedReason.LocalHangup)
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

    override suspend fun callAgain(): Result<Unit> {
        val userId = lastOutgoingUserId
            ?: return Result.failure(IllegalStateException("No previous outgoing call to retry"))

        endedDismissJob?.cancel()
        endedDismissJob = null
        cleanupCall()

        return startOutgoingCall(
            userId = userId,
            isVideo = lastOutgoingIsVideo,
            isCallAgain = true
        ).map { }
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

    override fun setCameraEnabled(enabled: Boolean) {
        if (_mediaState.value.isCameraEnabled == enabled) return
        _currentCall?.setVideoEnabled(enabled)
        Log.d(TAG, "Camera set to: $enabled")
    }

    override fun switchCamera(): Result<Unit> {
        return try {
            val capturer = _currentCall?.localParticipant?.getVideoTracks()?.firstOrNull()?.capturer
            capturer?.switch()
            val newFrontCamera = !_mediaState.value.isFrontCamera
            _mediaState.update { it.copy(isFrontCamera = newFrontCamera) }
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
            val earpiece =
                availableAudioDevices.value.filterIsInstance<AudioDevice.Earpiece>().firstOrNull()
            val wired = availableAudioDevices.value.filterIsInstance<AudioDevice.WiredHeadset>()
                .firstOrNull()
            val bluetooth =
                availableAudioDevices.value.filterIsInstance<AudioDevice.BluetoothHeadset>()
                    .firstOrNull()
            audioRouter.selectDevice(bluetooth ?: wired ?: earpiece)
        }

        _mediaState.update { it.copy(isSpeakerOn = newSpeakerState) }
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

    override fun handleIncomingCall(from: String, call: Call) {
        if (!_callUiState.value.phase.canAnswerOrMakeCall()) {
            Log.w(TAG, "Rejecting incoming call - already in a call")
            call.reject("Busy")
            return
        }
        endedDismissJob?.cancel()

        setupCallListeners(call)
        _callUiState.update {
            CallUiState(
                phase = CallPhase.Incoming,
                remoteUserId = from,
                isVideo = call.videoCall,
                incomingCall = call
            )
        }
        // Fetch user info in background and update state when ready
        scope.launch {
            fetchUserInfo(from)?.let { userInfo ->
                _callUiState.update { state ->
                    if (state.remoteUserId == from)
                        state.copy(
                            remoteUserName = userInfo.name,
                            remoteUserAvatar = userInfo.avatar
                        )
                    else state
                }
            }
        }


        if (context.isAppOnForeground()) {
            CallActivity.launchIncoming(
                context = context,
                callerId = from,
                isVideo = call.videoCall
            )
        }

        Log.d(TAG, "Incoming call from: $from, video: ${call.videoCall}")
        connectionProvider.connectChatClient()

        scope.launch {
            IncomingCallWorker.start(context)
            call.sendRinging()
            startRinging()
        }
    }

    /**
     * Starts ringtone and vibration for the incoming call.
     * Must be called after a foreground service is running (IncomingCallWorker.setForeground())
     * to satisfy Android 15+ audio focus requirements.
     */
    private suspend fun startRinging() {
        Log.d(TAG, "startRinging: ringtone and vibration started")
        if (!context.isAppOnForeground()) {
            awaitWorkStart()
        }
        toneManager.playRingtoneAndVibrate()
    }

    private suspend fun awaitWorkStart() = withTimeoutOrNull(5.seconds.inWholeMilliseconds) {
        WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(IncomingCallWorker.INCOMING_CALL_WORK_NAME)
            .first { infos ->
                val running = infos.find { it.state == WorkInfo.State.RUNNING }
                return@first running != null && running.progress.getBoolean(
                    key = IncomingCallWorker.KEY_FOREGROUND_READY,
                    defaultValue = false
                )
            }
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
                    is CallState.Connecting -> { /* wait for participant connection state */
                    }

                    is CallState.Connected -> { /* handled by participant connection state */
                    }

                    is CallState.Closed -> handleCallEnded(EndedReason.RemoteHangup)
                    is CallState.Idle -> { /* ignore */
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
                if (participant.id == call.localParticipant.id) return

                when (state) {
                    ParticipantState.Ringing -> {
                        _callUiState.update { it.copy(isRemoteRinging = true) }
                    }

                    ParticipantState.Joined -> { /* handled by connection state */
                    }

                    ParticipantState.Left -> {
                        call.leave()
                        handleCallEnded(EndedReason.RemoteHangup)
                    }

                    ParticipantState.Declined -> {
                        call.leave()
                        handleCallEnded(EndedReason.Declined(reason))
                    }

                    ParticipantState.NoAnswer -> {
                        call.leave()
                        handleCallEnded(EndedReason.NoAnswer)
                    }

                    else -> { /* ignore */
                    }
                }
            }

            override fun onParticipantConnectionStateChanged(
                call: Call,
                participant: Participant,
                state: ParticipantConnectionState
            ) {
                Log.d(TAG, "Participant ${participant.id} connection state: $state")
                if (participant.id == call.localParticipant.id) return

                when (state) {
                    ParticipantConnectionState.Connecting -> {
                        _callUiState.update { it.copy(phase = CallPhase.Connecting) }
                        stopTone()
                    }

                    ParticipantConnectionState.Connected -> {
                        cancelNoAnswerTimeout()
                        cancelReconnectTimeout()
                        reconnectAttempts = 0
                        lastConnectedAt = System.currentTimeMillis()
                        _callUiState.update {
                            it.copy(
                                phase = CallPhase.Connected,
                                connectedAt = lastConnectedAt
                            )
                        }
                        stopTone()
                        startDurationTimer()
                    }

                    ParticipantConnectionState.Reconnecting -> handleReconnecting()

                    ParticipantConnectionState.Disconnected -> {
                        if (_callUiState.value.phase != CallPhase.Reconnecting) {
                            handleCallEnded(EndedReason.Failed("Connection lost"))
                        }
                    }

                    ParticipantConnectionState.Idle -> { /* ignore */
                    }
                }
            }

            override fun onRemoteParticipantEvent(
                call: Call,
                participant: Participant,
                event: ParticipantEvent
            ) {
                Log.d(TAG, "Participant ${participant.id} event: $event")
                if (participant.id != call.localParticipant.id) {
                    when (event) {
                        is ParticipantEvent.Mute -> {
                            _mediaState.update { it.copy(isRemoteMuted = event.muted) }
                        }

                        is ParticipantEvent.Video -> {
                            _mediaState.update { it.copy(isRemoteVideoEnabled = event.enabled) }
                        }

                        is ParticipantEvent.Hold -> {
                            _mediaState.update { it.copy(isOnHold = event.hold) }
                        }

                        is ParticipantEvent.ScreenShare -> { /* handle if needed */
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
                        _mediaState.update { it.copy(isOnHold = event.hold) }
                    }

                    is ParticipantEvent.Mute -> {
                        _mediaState.update { it.copy(isMuted = event.muted) }
                    }

                    is ParticipantEvent.ScreenShare -> {}
                    is ParticipantEvent.Video -> {
                        _mediaState.update {
                            it.copy(
                                isCameraEnabled = event.enabled,
                                localVideoTrack = if (event.enabled)
                                    participant.getVideoTracks().firstOrNull()?.videoTrack else null
                            )
                        }
                    }
                }
            }

            override fun onRemoteVideoTrackAdded(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack
            ) {
                Log.d(TAG, "Remote video track added from ${participant.id}")
                _mediaState.update {
                    it.copy(
                        remoteVideoTrack = videoTrack,
                        isRemoteVideoEnabled = participant.videoEnabled
                    )
                }
            }

            override fun onRemoteVideoTrackRemoved(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack
            ) {
                Log.d(TAG, "Remote video track removed from ${participant.id}")
                _mediaState.update {
                    it.copy(
                        remoteVideoTrack = null,
                        isRemoteVideoEnabled = false
                    )
                }
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
            ) { /* handle if needed */
            }

            override fun onDominantSpeakerChanged(
                call: Call,
                speaker: com.callclient.call.data.ActiveSpeakerInfo?
            ) { /* handle if needed */
            }

            override fun onParticipantsRemoved(call: Call, participants: List<Participant>) {}

            override fun onSessionRenewed(call: Call) {}
        })
    }

    private fun handleCallEnded(reason: EndedReason) {
        // If already in a terminal Ended state, ignore duplicate signals
        val currentPhase = _callUiState.value.phase
        if (reason is EndedReason.RemoteHangup && currentPhase == CallPhase.Ended) {
            Log.d(TAG, "Ignoring RemoteHangup — already in Ended state")
            return
        }

        Log.d(TAG, "Call ended: $reason")

        cancelDurationTimer()
        cancelNoAnswerTimeout()
        cancelReconnectTimeout()

        scope.launch {
            playTone(ToneConfig.hangup())
            delay(1000)
            stopTone()
        }

        setEndedState(reason)

        endedDismissJob?.cancel()
        endedDismissJob = scope.launch {
            cleanupCall()
            delay(reason.dismissTimeoutMs)
            _callUiState.update { CallUiState.IDLE }
        }
    }

    private fun setEndedState(reason: EndedReason) {
        _callUiState.update {
            it.copy(phase = CallPhase.Ended, endedReason = reason, incomingCall = null)
        }
    }

    private fun handleReconnecting() {
        reconnectAttempts++
        Log.d(TAG, "Reconnecting attempt: $reconnectAttempts")

        if (reconnectAttempts > CallUiState.MAX_RECONNECT_ATTEMPTS) {
            handleCallEnded(EndedReason.Failed("Connection lost"))
            return
        }

        _callUiState.update {
            it.copy(
                phase = CallPhase.Reconnecting,
                reconnectAttempt = reconnectAttempts
            )
        }

        scope.launch { playTone(ToneConfig.reconnecting()) }
        startReconnectTimeout()
    }

    private fun cleanupCall() {
        Log.d(TAG, "Cleaning up call")
        _currentCall?.removeListener(CALL_LISTENER_KEY)
        _currentCall = null

        audioRouter.stop()

        _mediaState.update { MediaState() }
        _callDuration.update { 0 }
        reconnectAttempts = 0
        lastConnectedAt = 0

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
    }

    private fun setupAudioRouting(isVideo: Boolean) {
        audioRouter.start(audioRouterListener)

        val priority: List<KClass<out AudioDevice>> = if (isVideo) {
            listOf(
                AudioDevice.BluetoothHeadset::class,
                AudioDevice.WiredHeadset::class,
                AudioDevice.Speakerphone::class,
                AudioDevice.Earpiece::class
            )
        } else {
            listOf(
                AudioDevice.BluetoothHeadset::class,
                AudioDevice.WiredHeadset::class,
                AudioDevice.Earpiece::class,
                AudioDevice.Speakerphone::class
            )
        }

        audioRouter.setPreferredDeviceOrder(priority)

        scope.launch {
            delay(100)
            updateSpeakerState(selectedAudioDevice.value)
        }

        Log.d(TAG, "Audio routing setup: video=$isVideo")
    }

    private fun updateSpeakerState(device: AudioDevice?) {
        _mediaState.update { it.copy(isSpeakerOn = device is AudioDevice.Speakerphone) }
    }

    private suspend fun fetchUserInfo(userId: String): UserInfo? {
        return try {
            val userFromDb = SceytChatUIKit.chatUIFacade.userInteractor.getUserFromDbById(userId)
            if (userFromDb != null) {
                return UserInfo(
                    name = userFromDb.getPresentableName(),
                    avatar = userFromDb.avatarURL
                )
            }
            ConnectionEventManager.awaitToConnectSceytWithTimeout(10.seconds.inWholeMilliseconds)
            val result = SceytChatUIKit.chatUIFacade.userInteractor.getUserById(userId)
            result.fold(
                onSuccess = { user ->
                    user ?: return@fold null
                    UserInfo(name = user.getPresentableName(), avatar = user.avatarURL)
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

    private data class UserInfo(val name: String?, val avatar: String?)

    // ========== Timers ==========

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callDuration.update { it + 1 }
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
            if (_callUiState.value.phase == CallPhase.Outgoing) {
                handleCallEnded(EndedReason.NoAnswer)
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
            if (_callUiState.value.phase == CallPhase.Reconnecting) {
                handleCallEnded(EndedReason.Failed("Reconnection timed out"))
            }
        }
    }

    private fun cancelReconnectTimeout() {
        reconnectTimeoutJob?.cancel()
        reconnectTimeoutJob = null
    }

    // ========== Tones ==========

    private suspend fun playTone(config: ToneConfig) {
        toneManager.playTone(config)
    }

    private fun stopTone() {
        scope.launch {
            toneManager.stopCurrentTone()
        }
    }
}
