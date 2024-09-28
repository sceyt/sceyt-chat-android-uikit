package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttachmentWithUserData(
        val attachment: SceytAttachment,
        val user: SceytUser?
) : Parcelable