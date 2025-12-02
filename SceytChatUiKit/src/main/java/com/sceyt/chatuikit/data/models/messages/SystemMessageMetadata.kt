package com.sceyt.chatuikit.data.models.messages

/**
 * Metadata for member added/removed system messages
 */
data class MembersMetaData(
    val members: List<String>?
)

/**
 * Metadata for disappearing messages system message
 */
data class DisappearingMessageMetadata(
    val duration: String?
)
