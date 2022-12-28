package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.extensions.isNull

object TransferUpdateObserver {
    private val progressUpdateListener: HashMap<String, (TransferData) -> Unit> = hashMapOf()

    fun setListener(key: String, listener: (TransferData) -> Unit) {
        progressUpdateListener[key] = listener
    }

    internal fun clearListeners() {
        progressUpdateListener.clear()
    }

    fun update(data: TransferData) {
        val key: String = if (data.attachmentTid.isNull() || data.attachmentTid == 0L) {
            data.url.toString()
        } else {
            data.attachmentTid.toString()
        }
        progressUpdateListener[key]?.invoke(data)
    }
}