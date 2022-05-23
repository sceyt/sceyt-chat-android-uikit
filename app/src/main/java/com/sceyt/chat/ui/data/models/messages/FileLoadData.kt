package com.sceyt.chat.ui.data.models.messages

import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class FileLoadData : BaseObservable() {
    @Bindable
    var progressPercent: Int = 1
        set(value) {
            field = value
            notifyPropertyChanged(BR.progressPercent)
        }

    @Bindable
    var loading: Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.loading)
        }

    fun update(progress: Int?, isLoading: Boolean) {
        Log.i("sdfsdf",progress.toString())
        this.progressPercent = progress ?: this.progressPercent
        this.loading = isLoading
    }
}