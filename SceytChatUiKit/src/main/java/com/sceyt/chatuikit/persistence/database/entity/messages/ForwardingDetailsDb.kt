package com.sceyt.chatuikit.persistence.database.entity.messages

internal data class ForwardingDetailsDb(
        val messageId: Long,
        val userId: String?,
        val hops: Int
)