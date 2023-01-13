package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.mappers.getInfoFromMetadata

open class AttachmentDataItem {
    lateinit var file: SceytAttachment
    var size: Size? = null
    var blurredThumb: Bitmap? = null
    var thumbPath: String? = null
    var duration: Long? = null

    protected constructor()

    constructor(attachment: SceytAttachment) {
        file = attachment
        attachment.getInfoFromMetadata { size, blurredThumb, duration ->
            this.size = size
            this.blurredThumb = blurredThumb
            this.duration = duration
        }
    }
}