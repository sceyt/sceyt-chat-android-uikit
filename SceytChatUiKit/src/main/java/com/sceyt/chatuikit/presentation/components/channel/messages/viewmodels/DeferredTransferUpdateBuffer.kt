package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded

internal class DeferredTransferUpdateBuffer {
    private val updates = linkedMapOf<Long, Entry>()

    fun isNotEmpty(): Boolean = updates.isNotEmpty()

    fun add(transfer: TransferData) {
        val entry = updates.getOrPut(transfer.messageTid) { Entry() }
        if (transfer.isMessageListThumbLoaded()) {
            entry.thumb = transfer
        } else {
            entry.transfer = transfer
        }
    }

    fun drain(): List<TransferData> {
        val result = updates.values.flatMap { entry ->
            listOfNotNull(entry.transfer, entry.thumb)
        }
        updates.clear()
        return result
    }

    private fun TransferData.isMessageListThumbLoaded(): Boolean {
        return state == ThumbLoaded && thumbData?.key == ThumbFor.MessagesLisView.value
    }

    private data class Entry(
        var transfer: TransferData? = null,
        var thumb: TransferData? = null,
    )
}
