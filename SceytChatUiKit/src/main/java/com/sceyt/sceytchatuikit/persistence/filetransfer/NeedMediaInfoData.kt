package com.sceyt.sceytchatuikit.persistence.filetransfer

import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment

sealed class NeedMediaInfoData(val item: SceytAttachment) {
    class NeedDownload(attachment: SceytAttachment) : NeedMediaInfoData(attachment)
    class NeedThumb(attachment: SceytAttachment, val size: Size) : NeedMediaInfoData(attachment)
}