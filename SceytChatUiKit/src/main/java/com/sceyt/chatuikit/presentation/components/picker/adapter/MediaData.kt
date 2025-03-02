package com.sceyt.chatuikit.presentation.components.picker.adapter

import android.net.Uri
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker

data class MediaData(
        val contentUri: Uri,
        val realPath: String,
        val isWrong: Boolean,
        val mediaType: BottomSheetMediaPicker.MediaType
) {
    var selected: Boolean = false
}