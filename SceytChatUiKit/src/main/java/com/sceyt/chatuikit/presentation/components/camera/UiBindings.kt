package com.sceyt.chatuikit.presentation.components.camera

import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytActivityCustomCameraBinding
import com.sceyt.chatuikit.styles.camera.CustomCameraStyle
import java.util.Locale

object UiBindings {

    fun render(
        binding: SceytActivityCustomCameraBinding,
        state: CameraState,
        style: CustomCameraStyle
    ) {
        val captureStateKey = when {
            state.isRecording -> 1
            state.currentMode == CameraState.CameraMode.PHOTO -> 2
            else -> 3
        }
        val captureDrawable = when {
            state.isRecording -> style.captureStyle.stopRecordingIcon
            state.currentMode == CameraState.CameraMode.PHOTO -> style.captureStyle.photoIcon
            else -> style.captureStyle.videoIcon
        }
        captureDrawable?.let {
            binding.btnCapture.setImageDrawable(it)
            val previousState = binding.btnCapture.getTag(R.id.sceyt_capture_state) as? Int
            if (previousState != null && previousState != captureStateKey) {
                binding.btnCapture.animate().cancel()
                binding.btnCapture.scaleX = 0.9f
                binding.btnCapture.scaleY = 0.9f
                binding.btnCapture.alpha = 0.7f
                binding.btnCapture.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(140L)
                    .start()
            }
            binding.btnCapture.setTag(R.id.sceyt_capture_state, captureStateKey)
        }

        when (state.flashMode) {
            androidx.camera.core.ImageCapture.FLASH_MODE_OFF -> style.flashStyle.offIcon
            androidx.camera.core.ImageCapture.FLASH_MODE_ON -> style.flashStyle.onIcon
            else -> style.flashStyle.autoIcon
        }?.let {
            binding.btnFlash.background = it
            binding.btnFlash.setImageDrawable(null)
        }

        binding.btnFlash.isVisible = state.hasFlashUnit
        binding.btnFlash.isEnabled = state.hasFlashUnit

        binding.tvRecordingTime.isVisible = state.isRecording
        if (state.isRecording) binding.tvRecordingTime.text = state.getFormattedRecordingTime()

        binding.btnPauseResume.isVisible = state.isRecording
        if (state.isRecording) {
            val pauseResumeDrawable = if (state.isPaused) style.playIcon else style.pauseIcon
            pauseResumeDrawable?.let {
                binding.btnPauseResume.background = it
                binding.btnPauseResume.setImageDrawable(null)
            }
            binding.btnPauseResume.contentDescription = binding.root.context.getString(
                if (state.isPaused) R.string.sceyt_play else R.string.sceyt_pause
            )
        }
        binding.btnGallery.isVisible = !state.isRecording
        binding.btnSwitchCamera.isVisible = !state.isRecording
        binding.modeSelector.isVisible = true
        binding.modeSelector.setModesVisible(!state.isRecording)
        binding.modeSelector.isEnabled =
            !state.isRecording && state.allowedMode == CameraState.AllowedMode.BOTH
        binding.modeSelector.setAllowedMode(state.allowedMode)

        binding.tvZoomLevel.text = String.format(Locale.getDefault(), "%.1fx", state.zoomRatio)
        binding.tvZoomLevel.isVisible = state.isZoomControlsVisible
    }
}