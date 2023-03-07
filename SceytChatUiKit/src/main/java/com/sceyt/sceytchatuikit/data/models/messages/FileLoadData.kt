package com.sceyt.sceytchatuikit.data.models.messages

class FileLoadData(val loadId: String) {
    var progressPercent: Float = 2f
    var loading: Boolean = false
    var success: Boolean = true
    var position = 0

    fun update(progress: Float? = null, loading: Boolean, success: Boolean = false) {
        this.progressPercent = progress ?: this.progressPercent
        this.loading = loading
        this.success = success
    }

    fun loadedState(): FileLoadData {
        progressPercent = 100f
        loading = false
        success = true
        return this
    }
}