package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.user.User

data class DraftMessage(
        val chatId: Long,
        val message: String?,
        val createdAt: Long,
        val metadata: String?,
        val mentionUsers: List<User>?)
