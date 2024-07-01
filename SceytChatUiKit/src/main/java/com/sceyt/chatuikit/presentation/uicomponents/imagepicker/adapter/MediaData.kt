package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter

import android.net.Uri
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.sceyt.chatuikit.BR
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.GalleryMediaPicker

data class MediaData(val contentUri: Uri,
                     val realPath: String,
                     val isWrong: Boolean,
                     val mediaType: GalleryMediaPicker.MediaType) : BaseObservable() {

    var selected: Boolean = false
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.selected)
        }
}