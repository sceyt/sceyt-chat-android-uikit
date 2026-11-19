package com.sceyt.chatuikit.presentation.components.picker

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.PickerFilterType

class MediaPickerViewModelFactory(
    private val contentResolver: ContentResolver,
    private val filterType: PickerFilterType,
    private val maxSelectCount: Int,
    private val preselectedPaths: List<String>,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return MediaPickerViewModel(
            contentResolver = contentResolver,
            savedStateHandle = extras.createSavedStateHandle(),
            filterType = filterType,
            maxSelectCount = maxSelectCount,
            preselectedPaths = preselectedPaths,
        ) as T
    }
}
