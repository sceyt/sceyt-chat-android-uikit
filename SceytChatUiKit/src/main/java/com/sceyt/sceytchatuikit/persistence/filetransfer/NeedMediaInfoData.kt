package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment

sealed class NeedMediaInfoData(val item: SceytAttachment) {
    class NeedDownload(attachment: SceytAttachment) : NeedMediaInfoData(attachment)
    class NeedThumb(attachment: SceytAttachment, val thumbData: ThumbData) : NeedMediaInfoData(attachment)
}