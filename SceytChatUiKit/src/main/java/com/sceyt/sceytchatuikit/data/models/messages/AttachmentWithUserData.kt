package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chat.models.user.User
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttachmentWithUserData(
        val attachment: SceytAttachment,
        val user: User?
) : Parcelable