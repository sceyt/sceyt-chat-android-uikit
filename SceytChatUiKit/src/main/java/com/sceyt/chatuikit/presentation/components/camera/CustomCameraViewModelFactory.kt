package com.sceyt.chatuikit.presentation.components.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CustomCameraViewModelFactory(
    private val allowedModeProvider: () -> CameraState.AllowedMode
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CustomCameraViewModel(allowedModeProvider()) as T
    }
}
