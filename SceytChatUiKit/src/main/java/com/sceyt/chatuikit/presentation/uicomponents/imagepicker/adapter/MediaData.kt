package com.sceyt.chatuikit.presentation.uicomponents.imagepicker.adapter

import android.net.Uri
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.sceyt.chatuikit.BR

data class MediaData(val contentUri: Uri,
                     val realPath: String,
                     val isWrong: Boolean) : BaseObservable() {

    var selected: Boolean = false
        @Bindable get
        set(value) {
            field = value
            notifyPropertyChanged(BR.selected)
        }
}