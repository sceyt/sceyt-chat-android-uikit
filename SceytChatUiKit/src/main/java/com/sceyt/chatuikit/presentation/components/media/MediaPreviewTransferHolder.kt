package com.sceyt.chatuikit.presentation.components.media

import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData

object MediaPreviewTransferHolder {

    data class PreloadedData(
        val items: List<AttachmentWithUserData>,
        val initialIndex: Int,
    )

    private var pending: PreloadedData? = null

    fun set(data: PreloadedData) {
        pending = data
    }

    fun consume(): PreloadedData? = pending?.also { pending = null }
}
