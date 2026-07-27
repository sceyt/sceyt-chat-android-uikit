package com.sceyt.chatuikit.presentation.helpers

import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded

internal class DeferredTransferUpdateBuffer(
    private val thumbFor: ThumbFor,
) {
    private val updates = linkedMapOf<Long, Entry>()

    fun isNotEmpty(): Boolean = updates.isNotEmpty()

    fun add(transfer: TransferData) {
        if (transfer.state == ThumbLoaded && !transfer.isThumbLoadedFor(thumbFor)) return

        val entry = updates.getOrPut(transfer.messageTid) { Entry() }
        if (transfer.isThumbLoadedFor(thumbFor)) {
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

    private data class Entry(
        var transfer: TransferData? = null,
        var thumb: TransferData? = null,
    )
}

internal fun TransferData.isThumbLoadedFor(thumbFor: ThumbFor): Boolean {
    return state == ThumbLoaded && thumbData?.key == thumbFor.value
}
