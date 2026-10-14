package com.sceyt.chatuikit.data.models.messages

import com.google.gson.annotations.SerializedName

/**
 * Metadata for member added/removed system messages
 */
data class MembersMetaData(
    @SerializedName("m")
    val members: List<String>?
)

/**
 * Metadata for disappearing messages system message
 */
data class DisappearingMessageMetadata(
    @SerializedName("autoDeletePeriod")
    val duration: String?
)

/**
 * Metadata for the pin system message.
 */
data class PinnedMessageMetadata(
    @SerializedName("id")
    val id: String?
)
