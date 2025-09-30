package com.sceyt.chatuikit.data.models.channels

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftAttachment(
        val channelId: Long,
        val filePath: String,
        val type: AttachmentTypeEnum,
) : Parcelable