package com.sceyt.chat.demo.call.manager

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.callclient.CallClient
import com.callclient.call.Call
import com.callclient.call.calllisteners.CallEventsListener
import com.callclient.call.data.ActiveSpeakerInfo
import com.callclient.call.data.AudioSettings
import com.callclient.call.data.CallState
import com.callclient.call.data.CreateCallOptions
import com.callclient.call.data.JoinCallOptions
import com.callclient.call.data.Participant
import com.callclient.call.data.ParticipantConnectionState
import com.callclient.call.data.ParticipantEvent
import com.callclient.call.data.VideoSettings
import com.sceyt.audiorouting.AudioDevice
import com.sceyt.audiorouting.AudioRouter
import com.sceyt.audiorouting.AudioRouterConfig
import com.sceyt.audiorouting.AudioRouterListener
import com.sceyt.chat.demo.call.manager.CallUiState.CallPhase
import com.sceyt.chat.demo.call.manager.CallUiState.EndedReason
import com.sceyt.chat.demo.call.notification.CallNotificationChannels
import com.sceyt.chat.demo.call.ui.CallActivity
import com.sceyt.chat.demo.call.worker.IncomingCallWorker
import com.sceyt.chat.demo.call.worker.OngoingCallWorker
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.chat.models.signal.ParticipantState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
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
import com.callclient.call.data.fold as callFold
import com.sceyt.chatuikit.data.models.fold as sceytFold

/**
 * Implementation of [CallManager] that orchestrates CallClient, ToneManager, and AudioRouter SDKs.
 */
class CallManagerImpl(
    private val context: Context,
    private val onChatConnectNeeded: () -> Unit = {}
) : CallManager {

    companion object {
        private const val TAG = "CallManagerImpl"
        private const val CALL_LISTENER_KEY = "call_manager_listener"
        private const val CALL_CLIENT_LISTENER_KEY = "app_call_listener"
    }

    private data class UserInfo(
        val name: String?,
        val avatar: String?,
    )

    private val callClient: CallClient by lazy { CallClient.requireInstance() }
    private val toneManager: ToneManager by lazy { ToneManagerFactory.getInstance(context) }
    private val audioRouter: AudioRouter by lazy {
        AudioRouter.create(context, AudioRouterConfig(loggingEnabled = true))
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val groupCallParticipantResolver = GroupCallParticipantResolver()
    private val participantInfoCache = LinkedHashMap<String, UserInfo>()

    private val _callUiState = MutableStateFlow(CallUiState.IDLE)
    override val callUiState: StateFlow<CallUiState> = _callUiState.asStateFlow()

    private val _mediaState = MutableStateFlow(MediaState())
    override val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    override val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    override val availableAudioDevices: StateFlow<List<AudioDevice>> = audioRouter.availableDevices
    override val selectedAudioDevice: StateFlow<AudioDevice?> = audioRouter.selectedDevice

    private var _currentCall: Call? = null
    override val currentCall: Call? get() = _currentCall

    private var durationJob: Job? = null
    private var noAnswerJob: Job? = null
    private var reconnectTimeoutJob: Job? = null
    private var endedDismissJob: Job? = null

    private var lastConnectedAt: Long = 0

    override fun init() {
        CallNotificationChannels.createChannels(context)
        callClient.addListener(CALL_CLIENT_LISTENER_KEY, object : CallClient.ClientListener {
            override fun onInvitedToCall(from: String, call: Call) {
                Log.d(TAG, "Invited to call from: $from, callId: ${call.id}")
                handleIncomingCall(from, call)
            }
        })
    }

    override suspend fun startOutgoingCall(
        userId: String,
        isVideo: Boolean,
        isCallAgain: Boolean,
        callPrepared: (Call) -> Unit,
    ): Result<Call> {
        return startOutgoingCallInternal(
            participantIds = listOf(userId),
            isVideo = isVideo,
            mediaFlow = MediaFlow.P2P,
            metadata = emptyMap(),
            includeRemotePlaceholders = true,
            shouldPlayRingback = true,
            isCallAgain = isCallAgain,
            callPrepared = callPrepared,
        )
    }

    override suspend fun startOutgoingGroupCall(
        channel: SceytChannel,
        isVideo: Boolean,
        isCallAgain: Boolean,
        callPrepared: (Call) -> Unit,
    ): Result<Call> {
        primeParticipantInfos(channel.members.orEmpty())
        val participantIds = groupCallParticipantResolver.resolveParticipantIds(channel)
        if (participantIds.isEmpty()) {
            return Result.failure(IllegalStateException("No remote participants available"))
        }

        return startOutgoingCallInternal(
            participantIds = participantIds,
            isVideo = isVideo,
            mediaFlow = MediaFlow.SFU,
            metadata = mapOf(
                GroupCallMetadata.CHANNEL_ID to channel.id.toString(),
                GroupCallMetadata.CHANNEL_NAME to channel.subject.orEmpty().ifBlank { DEFAULT_GROUP_NAME },
            ),
            includeRemotePlaceholders = false,
            shouldPlayRingback = false,
            isCallAgain = isCallAgain,
            callPrepared = callPrepared,
        )
    }

    override suspend fun answerIncomingCall(): Result<Unit> {
        val currentState = _callUiState.value
        if (currentState.phase != CallPhase.Incoming) {
            return Result.failure(IllegalStateException("No incoming call to answer"))
        }
        val call = currentState.call
            ?: return Result.failure(IllegalStateException("Missing Call object in state"))

        return try {
            stopTone()
            _callUiState.update { it.copy(phase = CallPhase.Connecting) }
            OngoingCallWorker.start(context)
            setupAudioRouting(call.isVideoCall)

            val options = JoinCallOptions.default().copy(
                audioSettings = AudioSettings(disableManageAudioRoute = true),
                videoSettings = VideoSettings(publishVideo = call.isVideoCall),
            )

            setupCallListeners(call)
            val result = call.join(options)
            result.callFold(
                onSuccess = { answeredCall ->
                    _currentCall = answeredCall
                    syncParticipantsFromCall(answeredCall)
                    refreshDurationTimer()
                    Log.d(TAG, "Answered incoming call: ${answeredCall.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to answer call", error)
                    cleanupCall()
                    setEndedState(EndedReason.Failed(error.message ?: "Failed to answer call"))
                    Result.failure(error)
                },
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
        val call = currentState.call
            ?: return Result.failure(IllegalStateException("Missing Call object in state"))

        return try {
            call.reject(reason)
            stopTone()
            endedDismissJob?.cancel()
            cleanupCall()
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
        (_currentCall ?: _callUiState.value.call)?.sendRinging()
    }

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
            availableAudioDevices.value.filterIsInstance<AudioDevice.Speakerphone>()
                .firstOrNull()
                ?.let(audioRouter::selectDevice)
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

    override fun handleIncomingCall(from: String, call: Call) {
        if (!_callUiState.value.phase.canAnswerOrMakeCall()) {
            Log.w(TAG, "Rejecting incoming call - already in a call")
            call.reject("Busy")
            return
        }

        endedDismissJob?.cancel()
        setupCallListeners(call)

        val remoteIds = buildIncomingRemoteIds(from = from, call = call)
        _callUiState.update {
            CallUiState(
                phase = CallPhase.Incoming,
                call = call,
                participants = buildInitialParticipants(
                    remoteIds = remoteIds,
                    includeRemotePlaceholders = !call.isGroupCall,
                ),
            )
        }

        syncParticipantsFromCall(call)
        if (call.isGroupCall) {
            call.channelIdOrNull?.let(::primeGroupParticipantInfo)
            remoteIds.forEach(::loadParticipantInfoAsync)
        } else {
            loadParticipantInfoAsync(from)
        }

        if (context.isAppOnForeground()) {
            CallActivity.launchIncoming(
                context = context,
                callerId = from,
                isVideo = call.isVideoCall
            )
        }

        Log.d(TAG, "Incoming call from: $from, video: ${call.videoCall}")
        onChatConnectNeeded()
        scope.launch {
            IncomingCallWorker.start(context)
            call.sendRinging()
            startRinging()
        }
    }

    override suspend fun callAgain(): Result<Unit> {
        val previousCall = _callUiState.value.call
            ?: return Result.failure(IllegalStateException("No previous outgoing call to retry"))

        endedDismissJob?.cancel()
        endedDismissJob = null
        cleanupCall()

        return if (previousCall.isGroupCall) {
            val channelId = previousCall.channelIdOrNull
                ?: return Result.failure(IllegalStateException("Missing group channel id"))
            val channel = groupCallParticipantResolver.getChannel(channelId)
                ?: return Result.failure(IllegalStateException("Unable to resolve group channel"))
            startOutgoingGroupCall(
                channel = channel,
                isVideo = previousCall.isVideoCall,
                isCallAgain = true,
            ).map { }
        } else {
            val userId = previousCall.primaryRemoteUserIdOrNull
                ?: return Result.failure(IllegalStateException("Missing direct participant"))
            startOutgoingCall(
                userId = userId,
                isVideo = previousCall.isVideoCall,
                isCallAgain = true,
            ).map { }
        }
    }

    override fun release() {
        cleanupCall()
        toneManager.stopAll()
        audioRouter.stop()
        Log.d(TAG, "CallManager released")
    }

    private suspend fun startOutgoingCallInternal(
        participantIds: List<String>,
        isVideo: Boolean,
        mediaFlow: MediaFlow,
        metadata: Map<String, String>,
        includeRemotePlaceholders: Boolean,
        shouldPlayRingback: Boolean,
        isCallAgain: Boolean,
        callPrepared: (Call) -> Unit,
    ): Result<Call> {
        if (!_callUiState.value.phase.canAnswerOrMakeCall() && !isCallAgain) {
            return Result.failure(IllegalStateException("Call already in progress"))
        }

        return try {
            endedDismissJob?.cancel()

            val result = runCatching {
                callClient.prepareCall(
                    callId = UUID.randomUUID().toString(),
                    CreateCallOptions(
                        participantsIds = participantIds,
                        videoCall = isVideo,
                        mediaFlow = mediaFlow,
                        metadata = metadata,
                    )
                )
            }

            result.onSuccess { call ->
                initialiseOutgoingState(
                    call = call,
                    remoteIds = participantIds,
                    includeRemotePlaceholders = includeRemotePlaceholders,
                )
                setupAudioRouting(isVideo)
                setupCallListeners(call)
                _currentCall = call
                syncParticipantsFromCall(call)
                if (call.isDirectCall) {
                    startNoAnswerTimeout()
                } else {
                    cancelNoAnswerTimeout()
                }
                callPrepared(call)
                OngoingCallWorker.start(context)

                val joinCallOptions = JoinCallOptions.default().copy(
                    audioSettings = AudioSettings(disableManageAudioRoute = true),
                    videoSettings = if (isVideo) VideoSettings(publishVideo = true) else null,
                )
                call.join(joinCallOptions)
                if (shouldPlayRingback) {
                    playTone(ToneConfig.ringback())
                }
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

    private fun initialiseOutgoingState(
        call: Call,
        remoteIds: List<String>,
        includeRemotePlaceholders: Boolean,
    ) {
        _callUiState.update {
            CallUiState(
                phase = CallPhase.Outgoing,
                call = call,
                participants = buildInitialParticipants(
                    remoteIds = remoteIds,
                    includeRemotePlaceholders = includeRemotePlaceholders,
                ),
            )
        }
        remoteIds.forEach(::loadParticipantInfoAsync)
    }

    private fun buildInitialParticipants(
        remoteIds: List<String>,
        includeRemotePlaceholders: Boolean = true,
    ): List<CallParticipantUiState> {
        val participants = mutableListOf(buildSelfParticipant())
        if (includeRemotePlaceholders) {
            remoteIds.distinct().forEach { userId ->
                participants += buildPlaceholderParticipant(userId = userId, isSelf = false)
            }
        }
        return participants.stabilizeParticipantOrder()
    }

    private fun buildSelfParticipant(): CallParticipantUiState {
        val currentUser = SceytChatUIKit.currentUser
        val userId = currentUser?.id ?: SceytChatUIKit.currentUserId.orEmpty()
        val info = participantInfoCache[userId] ?: UserInfo(
            name = currentUser?.getPresentableName(),
            avatar = currentUser?.avatarURL,
        )
        participantInfoCache[userId] = info
        return CallParticipantUiState(
            userId = userId,
            name = info.name,
            avatarUrl = info.avatar,
            isSelf = true,
            isMuted = _mediaState.value.isMuted,
            isVideoEnabled = _mediaState.value.isCameraEnabled,
            videoTrack = _mediaState.value.localVideoTrack,
        )
    }

    private fun buildPlaceholderParticipant(
        userId: String,
        isSelf: Boolean,
    ): CallParticipantUiState {
        val info = participantInfoCache[userId]
        return CallParticipantUiState(
            userId = userId,
            name = info?.name,
            avatarUrl = info?.avatar,
            isSelf = isSelf,
            isMuted = if (isSelf) _mediaState.value.isMuted else false,
            isVideoEnabled = if (isSelf) _mediaState.value.isCameraEnabled else false,
            videoTrack = if (isSelf) _mediaState.value.localVideoTrack else null,
        )
    }

    private fun buildIncomingRemoteIds(from: String, call: Call): List<String> {
        return call.getRemoteParticipants()
            .map(Participant::id)
            .ifEmpty { listOf(from) }
    }

    private fun setupCallListeners(call: Call) {
        call.addListener(CALL_LISTENER_KEY, object : CallEventsListener.CallAllEventsListener {
            override fun onCallStateChanged(call: Call, state: CallState) {
                Log.d(TAG, "Call state changed: $state")
                when (state) {
                    is CallState.Closed -> handleCallEnded(EndedReason.RemoteHangup)
                    is CallState.Connecting,
                    is CallState.Connected,
                    is CallState.Idle -> Unit
                }
            }

            override fun onParticipantsAdded(call: Call, participants: List<Participant>) {
                participants.forEach {
                    upsertParticipantFromSdk(it, isSelf = it.id == call.localParticipant.id)
                    if (it.id != call.localParticipant.id) {
                        loadParticipantInfoAsync(it.id)
                    }
                }
                refreshDurationTimer()
            }

            override fun onParticipantStateChanged(
                call: Call,
                participant: Participant,
                state: ParticipantState,
                reason: String?,
            ) {
                val isLocal = participant.id == call.localParticipant.id
                updateParticipant(participant.id) {
                    val base = participant.toUiState(existing = it, isSelf = isLocal)
                    base.copy(participantState = state)
                }

                if (!isLocal) {
                    when (state) {
                        ParticipantState.Ringing -> {
                            if (_callUiState.value.call?.isGroupCall != true) {
                                _callUiState.update { uiState -> uiState.copy(isRemoteRinging = true) }
                            }
                        }

                        ParticipantState.Left -> {
                            if (_callUiState.value.call?.isGroupCall == true) {
                                removeParticipant(participant.id)
                            } else {
                                call.leave()
                                handleCallEnded(EndedReason.RemoteHangup)
                            }
                        }

                        ParticipantState.Declined -> {
                            if (_callUiState.value.call?.isGroupCall == true) {
                                removeParticipant(participant.id)
                            } else {
                                call.leave()
                                handleCallEnded(EndedReason.Declined(reason))
                            }
                        }

                        ParticipantState.NoAnswer -> {
                            if (_callUiState.value.call?.isGroupCall == true) {
                                removeParticipant(participant.id)
                            } else {
                                call.leave()
                                handleCallEnded(EndedReason.NoAnswer)
                            }
                        }

                        else -> Unit
                    }
                }
                refreshDurationTimer()
            }

            override fun onParticipantConnectionStateChanged(
                call: Call,
                participant: Participant,
                state: ParticipantConnectionState,
            ) {
                val isLocal = participant.id == call.localParticipant.id
                updateParticipant(participant.id) {
                    participant.toUiState(existing = it, isSelf = isLocal)
                        .copy(connectionState = state)
                }

                if (isLocal && _callUiState.value.call?.isGroupCall == true) {
                    handleLocalConnectionStateChanged(state)
                } else if (!isLocal && _callUiState.value.call?.isGroupCall == true) {
                    if (state == ParticipantConnectionState.Connected) {
                        stopTone()
                    }
                } else if (!isLocal) {
                    handleDirectRemoteConnectionState(state)
                }

                refreshDurationTimer()
            }

            override fun onRemoteParticipantEvent(
                call: Call,
                participant: Participant,
                event: ParticipantEvent,
            ) {
                when (event) {
                    is ParticipantEvent.Mute -> {
                        updateParticipant(participant.id) { existing ->
                            participant.toUiState(existing = existing, isSelf = false)
                                .copy(isMuted = event.muted)
                        }
                    }

                    is ParticipantEvent.Video -> {
                        updateParticipant(participant.id) { existing ->
                            participant.toUiState(existing = existing, isSelf = false)
                                .copy(isVideoEnabled = event.enabled)
                        }
                    }

                    is ParticipantEvent.Hold,
                    is ParticipantEvent.ScreenShare -> Unit
                }
            }

            override fun onLocalParticipantEvent(
                call: Call,
                participant: Participant,
                event: ParticipantEvent,
            ) {
                when (event) {
                    is ParticipantEvent.Hold -> {
                        _mediaState.update { it.copy(isOnHold = event.hold) }
                    }

                    is ParticipantEvent.Mute -> {
                        _mediaState.update { it.copy(isMuted = event.muted) }
                        updateParticipant(participant.id) { existing ->
                            participant.toUiState(existing = existing, isSelf = true)
                                .copy(isMuted = event.muted)
                        }
                    }

                    is ParticipantEvent.Video -> {
                        val track = if (event.enabled) {
                            participant.getVideoTracks().firstOrNull()?.videoTrack
                        } else null
                        _mediaState.update {
                            it.copy(
                                isCameraEnabled = event.enabled,
                                localVideoTrack = track,
                            )
                        }
                        updateParticipant(participant.id) { existing ->
                            participant.toUiState(existing = existing, isSelf = true).copy(
                                isVideoEnabled = event.enabled,
                                videoTrack = track,
                            )
                        }
                    }

                    is ParticipantEvent.ScreenShare -> Unit
                }
            }

            override fun onRemoteVideoTrackAdded(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack,
            ) {
                updateParticipant(participant.id) { existing ->
                    participant.toUiState(existing = existing, isSelf = false).copy(
                        videoTrack = videoTrack,
                        isVideoEnabled = participant.videoEnabled,
                    )
                }
            }

            override fun onRemoteVideoTrackRemoved(
                call: Call,
                participant: Participant,
                videoTrack: VideoTrack,
            ) {
                updateParticipant(participant.id) { existing ->
                    participant.toUiState(existing = existing, isSelf = false).copy(
                        videoTrack = null,
                        isVideoEnabled = false,
                    )
                }
            }

            override fun onRemoteAudioTrackAdded(
                call: Call,
                participant: Participant,
                audioTrack: org.webrtc.AudioTrack,
            ) = Unit

            override fun onRemoteAudioTrackRemoved(
                call: Call,
                participant: Participant,
                audioTrack: org.webrtc.AudioTrack,
            ) = Unit

            override fun onCallMediaFlowChanged(call: Call) {
                Log.d(TAG, "Media flow changed: ${call.mediaFlow}")
            }

            override fun onActiveSpeakersChanged(call: Call, speakers: List<ActiveSpeakerInfo>) {
                updateActiveSpeakerUsers(speakers.map { it.participant.id }.toSet())
            }

            override fun onDominantSpeakerChanged(call: Call, speaker: ActiveSpeakerInfo?) {
                if (speaker != null && call.getActiveSpeakers().isEmpty()) {
                    updateActiveSpeakerUsers(setOf(speaker.participant.id))
                }
            }

            override fun onParticipantsRemoved(call: Call, participants: List<Participant>) {
                if (_callUiState.value.call?.isGroupCall == true) {
                    participants.forEach { participant ->
                        removeParticipant(participant.id)
                    }
                    refreshDurationTimer()
                }
            }

            override fun onSessionRenewed(call: Call) = Unit
        })
    }

    private fun handleDirectRemoteConnectionState(state: ParticipantConnectionState) {
        when (state) {
            ParticipantConnectionState.Connecting -> {
                _callUiState.update { it.copy(phase = CallPhase.Connecting) }
                stopTone()
            }

            ParticipantConnectionState.Connected -> {
                cancelNoAnswerTimeout()
                cancelReconnectTimeout()
                lastConnectedAt = System.currentTimeMillis()
                _callUiState.update {
                    it.copy(
                        phase = CallPhase.Connected,
                        connectedAt = lastConnectedAt
                    )
                }
                stopTone()
            }

            ParticipantConnectionState.Reconnecting -> handleReconnecting()

            ParticipantConnectionState.Disconnected -> {
                if (_callUiState.value.phase != CallPhase.Reconnecting) {
                    handleCallEnded(EndedReason.Failed("Connection lost"))
                }
            }

            ParticipantConnectionState.Idle -> Unit
        }
    }

    private fun handleLocalConnectionStateChanged(state: ParticipantConnectionState) {
        when (state) {
            ParticipantConnectionState.Connecting -> {
                _callUiState.update { it.copy(phase = CallPhase.Connecting) }
                stopTone()
            }

            ParticipantConnectionState.Connected -> {
                cancelReconnectTimeout()
                if (_callUiState.value.connectedAt == 0L) {
                    lastConnectedAt = System.currentTimeMillis()
                }
                _callUiState.update {
                    it.copy(
                        phase = CallPhase.Connected,
                        connectedAt = if (it.connectedAt == 0L) lastConnectedAt else it.connectedAt,
                    )
                }
                stopTone()
            }

            ParticipantConnectionState.Reconnecting -> handleReconnecting()

            ParticipantConnectionState.Disconnected -> {
                if (_callUiState.value.phase != CallPhase.Reconnecting) {
                    handleCallEnded(EndedReason.Failed("Connection lost"))
                }
            }

            ParticipantConnectionState.Idle -> Unit
        }
    }

    private fun handleCallEnded(reason: EndedReason) {
        if (reason is EndedReason.RemoteHangup && _callUiState.value.phase == CallPhase.Ended) {
            return
        }

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
            it.copy(
                phase = CallPhase.Ended,
                endedReason = reason,
                call = it.call ?: _currentCall,
            )
        }
    }

    private fun handleReconnecting() {
        _callUiState.update {
            it.copy(
                phase = CallPhase.Reconnecting,
            )
        }
        scope.launch { playTone(ToneConfig.reconnecting()) }
        startReconnectTimeout()
    }

    private fun cleanupCall() {
        _currentCall?.removeListener(CALL_LISTENER_KEY)
        _currentCall = null
        audioRouter.stop()
        _mediaState.update { MediaState() }
        _callDuration.value = 0L
        lastConnectedAt = 0
        updateActiveSpeakerUsers(emptySet())
        scope.launch { stopTone() }
    }

    private val audioRouterListener = object : AudioRouterListener {
        override fun onAudioDevicesChanged(
            devices: List<AudioDevice>,
            selectedDevice: AudioDevice?,
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
                AudioDevice.Earpiece::class,
            )
        } else {
            listOf(
                AudioDevice.BluetoothHeadset::class,
                AudioDevice.WiredHeadset::class,
                AudioDevice.Earpiece::class,
                AudioDevice.Speakerphone::class,
            )
        }

        audioRouter.setPreferredDeviceOrder(priority)
        scope.launch {
            delay(100)
            updateSpeakerState(selectedAudioDevice.value)
        }
    }

    private fun updateSpeakerState(device: AudioDevice?) {
        _mediaState.update { it.copy(isSpeakerOn = device is AudioDevice.Speakerphone) }
    }

    private fun syncParticipantsFromCall(call: Call) {
        upsertParticipantFromSdk(call.localParticipant, isSelf = true)
        call.getRemoteParticipants().forEach {
            upsertParticipantFromSdk(it, isSelf = false)
        }
    }

    private fun upsertParticipantFromSdk(participant: Participant, isSelf: Boolean) {
        updateParticipant(participant.id) { existing ->
            participant.toUiState(existing = existing, isSelf = isSelf)
        }
    }

    private fun updateParticipant(
        userId: String,
        transform: (CallParticipantUiState?) -> CallParticipantUiState,
    ) {
        _callUiState.update { state ->
            val participants = state.participants.toMutableList()
            val index = participants.indexOfFirst { it.userId == userId }
            val existing = participants.getOrNull(index)
            val updated = transform(existing)
            if (index >= 0) {
                participants[index] = updated
            } else {
                participants += updated
            }
            state.copy(participants = participants.stabilizeParticipantOrder())
        }
    }

    private fun removeParticipant(userId: String) {
        val currentUserId = SceytChatUIKit.currentUserId
        if (userId == currentUserId) return

        _callUiState.update { state ->
            state.copy(
                participants = state.participants
                    .filterNot { it.userId == userId }
                    .stabilizeParticipantOrder()
            )
        }
    }

    private fun updateActiveSpeakerUsers(userIds: Set<String>) {
        _callUiState.update { state ->
            state.copy(
                participants = state.participants.map { participant ->
                    participant.copy(isActiveSpeaker = participant.userId in userIds)
                }
            )
        }
    }

    private fun Participant.toUiState(
        existing: CallParticipantUiState?,
        isSelf: Boolean,
    ): CallParticipantUiState {
        val cachedInfo = participantInfoCache[id]
        val currentUser = if (isSelf) SceytChatUIKit.currentUser else null
        return CallParticipantUiState(
            userId = id,
            clientId = clientId.ifBlank { existing?.clientId.orEmpty() },
            name = cachedInfo?.name ?: existing?.name ?: currentUser?.getPresentableName(),
            avatarUrl = cachedInfo?.avatar ?: existing?.avatarUrl ?: currentUser?.avatarURL,
            isSelf = isSelf || existing?.isSelf == true,
            participantState = state,
            connectionState = connectionState,
            isMuted = muted,
            isVideoEnabled = videoEnabled,
            videoTrack = getVideoTracks().firstOrNull()?.videoTrack ?: existing?.videoTrack,
            isActiveSpeaker = existing?.isActiveSpeaker == true,
        )
    }

    private fun List<CallParticipantUiState>.stabilizeParticipantOrder(): List<CallParticipantUiState> {
        return distinctBy { it.userId }.sortedByDescending { it.isSelf }
    }

    private fun primeParticipantInfos(members: List<SceytMember>) {
        members.forEach { member ->
            participantInfoCache[member.id] = UserInfo(
                name = member.fullName.ifBlank { member.user.id },
                avatar = member.avatarUrl,
            )
        }
    }

    private fun loadParticipantInfoAsync(userId: String) {
        if (participantInfoCache.containsKey(userId)) {
            updateParticipantInfo(userId, participantInfoCache[userId])
            return
        }

        scope.launch {
            val info = fetchUserInfo(userId) ?: return@launch
            participantInfoCache[userId] = info
            updateParticipantInfo(userId, info)
        }
    }

    private fun updateParticipantInfo(userId: String, info: UserInfo?) {
        info ?: return
        _callUiState.update { state ->
            val participants = state.participants.toMutableList()
            val index = participants.indexOfFirst { it.userId == userId }

            when {
                index >= 0 -> {
                    val existing = participants[index]
                    participants[index] = existing.copy(
                        name = info.name ?: existing.name,
                        avatarUrl = info.avatar ?: existing.avatarUrl,
                    )
                    state.copy(participants = participants.stabilizeParticipantOrder())
                }

                userId == SceytChatUIKit.currentUserId -> {
                    participants += buildPlaceholderParticipant(
                        userId = userId,
                        isSelf = true,
                    ).copy(
                        name = info.name,
                        avatarUrl = info.avatar,
                    )
                    state.copy(participants = participants.stabilizeParticipantOrder())
                }

                else -> state
            }
        }
    }

    private fun primeGroupParticipantInfo(channelId: Long) {
        scope.launch {
            val channel = groupCallParticipantResolver.getChannel(channelId)
                ?: return@launch

            primeParticipantInfos(channel.members.orEmpty())
            channel.members.orEmpty().forEach { member ->
                updateParticipantInfo(
                    userId = member.id,
                    info = UserInfo(
                        name = member.fullName.ifBlank { member.user.id },
                        avatar = member.avatarUrl,
                    ),
                )
            }
        }
    }

    private suspend fun fetchUserInfo(userId: String): UserInfo? {
        return try {
            val userFromDb = SceytChatUIKit.chatUIFacade.userInteractor.getUserFromDbById(userId)
            if (userFromDb != null) {
                return UserInfo(
                    name = userFromDb.getPresentableName(),
                    avatar = userFromDb.avatarURL,
                )
            }

            ConnectionEventManager.awaitToConnectSceytWithTimeout(10.seconds.inWholeMilliseconds)
            SceytChatUIKit.chatUIFacade.userInteractor.getUserById(userId).sceytFold(
                onSuccess = { user ->
                    user ?: return@sceytFold null
                    UserInfo(name = user.getPresentableName(), avatar = user.avatarURL)
                },
                onError = {
                    Log.e(TAG, "Error fetching user info: ${it?.message}")
                    null
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user info", e)
            null
        }
    }

    private fun refreshDurationTimer() {
        if (_callUiState.value.shouldShowRunningTimer) {
            startDurationTimer()
        } else {
            cancelDurationTimer()
        }
    }

    private fun startDurationTimer() {
        if (durationJob?.isActive == true) return
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
                running != null && running.progress.getBoolean(
                    key = IncomingCallWorker.KEY_FOREGROUND_READY,
                    defaultValue = false,
                )
            }
    }

    private suspend fun playTone(config: ToneConfig) {
        toneManager.playTone(config)
    }

    private fun stopTone() {
        scope.launch {
            toneManager.stopCurrentTone()
        }
    }
}
