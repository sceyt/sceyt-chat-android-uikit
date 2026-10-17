package com.sceyt.chat.demo.call

import com.callclient.call.Call
import com.callclient.call.data.CreateCallOptions
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.chatuikit.data.models.channels.SceytChannel

private const val CHANNEL_ID = "channel_id"
private const val CHANNEL_NAME = "channel_name"

internal val Call.channelIdOrNull: Long?
    get() = metadata?.get(CHANNEL_ID)?.toLongOrNull()

internal fun SceytChannel.toCreateCallOptions(
    isVideo: Boolean,
    currentUserId: String?,
): CreateCallOptions? {
    val remoteIds = members.orEmpty()
        .map { it.id }
        .filter { it.isNotBlank() && it != currentUserId }
        .distinct()
    if (remoteIds.isEmpty()) return null

    return CreateCallOptions(
        participantsIds = if (isGroup) remoteIds else remoteIds.take(1),
        videoCall = isVideo,
        mediaFlow = if (isGroup) MediaFlow.SFU else MediaFlow.P2P,
        metadata = if (isGroup) {
            mapOf(
                CHANNEL_ID to id.toString(),
                CHANNEL_NAME to subject.orEmpty().ifBlank { "Group call" },
            )
        } else {
            emptyMap()
        },
    )
}
