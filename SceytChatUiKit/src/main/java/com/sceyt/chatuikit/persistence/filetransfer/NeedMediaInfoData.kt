package com.sceyt.chatuikit.persistence.filetransfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

sealed class NeedMediaInfoData(val item: SceytAttachment) {
    class NeedDownload(attachment: SceytAttachment) : NeedMediaInfoData(attachment)
    class NeedThumb(attachment: SceytAttachment, val thumbData: ThumbData) : NeedMediaInfoData(attachment)
}