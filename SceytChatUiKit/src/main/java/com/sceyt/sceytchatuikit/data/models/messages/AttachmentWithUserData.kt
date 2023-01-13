package com.sceyt.sceytchatuikit.data.models.messages

import com.sceyt.chat.models.user.User

data class AttachmentWithUserData(
        val attachment: SceytAttachment,
        val user: User?
)