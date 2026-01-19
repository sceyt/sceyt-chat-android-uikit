package com.sceyt.chatuikit.presentation.components.camera

import androidx.camera.core.ImageCapture
import java.util.Locale

data class CameraState(
    val currentMode: CameraMode = CameraMode.PHOTO,
    val lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordingDuration: Long = 0L,
    val zoomRatio: Float = 1.0f,
    val hasFlashUnit: Boolean = false,
    val allowedMode: AllowedMode = AllowedMode.BOTH,
    val isZoomControlsVisible: Boolean = false
) {
    enum class CameraMode {
        PHOTO, VIDEO
    }

    enum class AllowedMode {
        BOTH, PHOTO_ONLY, VIDEO_ONLY
    }

    fun getFormattedRecordingTime(): String {
        val seconds = (recordingDuration / 1000) % 60
        val minutes = (recordingDuration / 1000) / 60
        return String.format(Locale.US,"%02d:%02d", minutes, seconds)
    }
}

data class PendingPreview(
    val filePath: String,
    val isVideo: Boolean
)