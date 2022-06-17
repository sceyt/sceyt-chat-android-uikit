package com.sceyt.chat.ui.data.models.messages

class FileLoadData(val loadId: String) {
    var progressPercent: Float = 1f
    var loading: Boolean = false
    var success: Boolean = true

    fun update(progress: Float? = null, loading: Boolean, success: Boolean = false) {
        this.progressPercent = progress ?: this.progressPercent
        this.loading = loading
        this.success = success
    }
}