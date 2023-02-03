package com.sceyt.sceytchatuikit.persistence.filetransfer

object TransferUpdateObserver {
    private val progressUpdateListener: HashMap<String, (TransferData) -> Unit> = hashMapOf()

    fun setListener(key: String, listener: (TransferData) -> Unit) {
        progressUpdateListener[key] = listener
    }

    internal fun clearListeners() {
        progressUpdateListener.clear()
    }

    fun update(data: TransferData) {
        progressUpdateListener[data.messageTid.toString()]?.invoke(data)
    }
}