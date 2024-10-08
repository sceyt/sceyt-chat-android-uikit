package com.sceyt.chatuikit.persistence.file_transfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment

sealed class NeedMediaInfoData(val item: SceytAttachment) {
    class NeedDownload(attachment: SceytAttachment) : NeedMediaInfoData(attachment)
    class NeedThumb(attachment: SceytAttachment, val thumbData: ThumbData) : NeedMediaInfoData(attachment)
    class NeedLinkPreview(attachment: SceytAttachment, val onlyCheckMissingData: Boolean) : NeedMediaInfoData(attachment)
}