package com.sceyt.chatuikit.persistence.database.entity.messages

data class ForwardingDetailsDb(
        val messageId: Long,
        val userId: String?,
        val hops: Int
)