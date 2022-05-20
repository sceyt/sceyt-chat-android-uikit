package com.sceyt.chat.ui.data.models.messages

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class UploadData : BaseObservable() {
    @Bindable
    var progress: Int = 1
        set(value) {
            field = value
            notifyPropertyChanged(BR.progress)
        }

    @Bindable
    var uploadingg: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.uploadingg)
        }
    var error: Exception? = null

    fun update(progress: Int?, isLoading: Boolean, error: Exception?) {
        this.progress = progress ?: this.progress
        this.uploadingg = isLoading
        this.error = error
    }
}