package com.sceyt.chatuikit.presentation.components.camera

import android.os.SystemClock
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomCameraViewModel(
    allowedMode: CameraState.AllowedMode
) : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var lastPauseTime = 0L

    private var zoomUiJob: Job? = null
    private val zoomAutoHideMs = 2000L

    init {
        _state.update { s ->
            s.copy(
                allowedMode = allowedMode,
                currentMode = when (allowedMode) {
                    CameraState.AllowedMode.VIDEO_ONLY -> CameraState.CameraMode.VIDEO
                    else -> CameraState.CameraMode.PHOTO
                }
            )
        }
    }

    fun switchCamera() {
        if (_state.value.isRecording) return
        _state.update { s ->
            s.copy(
                lensFacing = if (s.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                    androidx.camera.core.CameraSelector.LENS_FACING_FRONT
                else
                    androidx.camera.core.CameraSelector.LENS_FACING_BACK
            )
        }
    }

    fun toggleFlashMode() {
        _state.update { s ->
            val newFlashMode = when (s.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }
            s.copy(flashMode = newFlashMode)
        }
    }

    fun setMode(mode: CameraState.CameraMode) {
        if (_state.value.isRecording) return
        _state.update { it.copy(currentMode = mode) }
        hideZoomControls()
    }

    fun setZoomRatio(ratio: Float) {
        _state.update { it.copy(zoomRatio = ratio) }
    }

    fun setHasFlashUnit(hasFlash: Boolean) {
        _state.update { it.copy(hasFlashUnit = hasFlash) }
    }

    fun onZoomInteracted() {
        _state.update { it.copy(isZoomControlsVisible = true) }
        zoomUiJob?.cancel()
        zoomUiJob = viewModelScope.launch {
            delay(zoomAutoHideMs)
            _state.update { it.copy(isZoomControlsVisible = false) }
        }
    }

    fun hideZoomControls() {
        zoomUiJob?.cancel()
        zoomUiJob = null
        _state.update { it.copy(isZoomControlsVisible = false) }
    }

    fun startRecording() {
        recordingStartTime = SystemClock.elapsedRealtime()
        pausedDuration = 0L
        _state.update { it.copy(isRecording = true, isPaused = false, recordingDuration = 0L) }
        startRecordingTimer()
    }

    fun pauseRecording() {
        if (!_state.value.isRecording || _state.value.isPaused) return
        lastPauseTime = SystemClock.elapsedRealtime()
        stopRecordingTimer()
        _state.update { it.copy(isPaused = true, recordingDuration = calculateElapsed()) }
    }

    fun resumeRecording() {
        if (!_state.value.isRecording || !_state.value.isPaused) return
        pausedDuration += (SystemClock.elapsedRealtime() - lastPauseTime)
        _state.update { it.copy(isPaused = false, recordingDuration = calculateElapsed()) }
        startRecordingTimer()
    }

    fun stopRecording() {
        stopRecordingTimer()
        pausedDuration = 0L
        lastPauseTime = 0L
        _state.update { it.copy(isRecording = false, isPaused = false, recordingDuration = 0L) }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            updateRecordingDuration()
            while (true) {
                delay(1000)
                updateRecordingDuration()
            }
        }
    }

    private fun calculateElapsed(): Long {
        return SystemClock.elapsedRealtime() - recordingStartTime - pausedDuration
    }

    private fun updateRecordingDuration() {
        val elapsed = calculateElapsed()
        _state.update { it.copy(recordingDuration = elapsed) }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        zoomUiJob?.cancel()
    }
}
