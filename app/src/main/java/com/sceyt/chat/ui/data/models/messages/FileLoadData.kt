package com.sceyt.chat.ui.data.models.messages

class FileLoadData {
    var progressPercent: Int = 1
    var loading: Boolean = false
    var success: Boolean = false

    fun update(progress: Int? = null, loading: Boolean, success: Boolean = false) {
        this.progressPercent = progress ?: this.progressPercent
        this.loading = loading
        this.success = success
    }

    fun loadedState(): FileLoadData {
        progressPercent = 1
        loading = false
        success = true
        return this
    }
}