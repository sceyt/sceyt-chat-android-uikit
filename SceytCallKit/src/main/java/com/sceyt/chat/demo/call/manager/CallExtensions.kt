package com.sceyt.chat.demo.call.manager

import com.callclient.call.Call
import com.sceyt.chat.models.signal.MediaFlow

internal val Call.isGroupCall: Boolean
    get() = mediaFlow == MediaFlow.SFU

internal val Call.isDirectCall: Boolean
    get() = !isGroupCall

internal val Call.channelIdOrNull: Long?
    get() = metadata.orEmpty()[GroupCallMetadata.CHANNEL_ID]?.toLongOrNull()

internal val Call.channelSubjectOrNull: String?
    get() = metadata.orEmpty()[GroupCallMetadata.CHANNEL_NAME]?.takeIf { it.isNotBlank() }

internal val Call.primaryRemoteUserIdOrNull: String?
    get() = getRemoteParticipants().firstOrNull()?.id

internal val Call.isVideoCall: Boolean
    get() = videoCall

internal fun Call.displayTitle(
    participants: List<CallParticipantUiState>,
    fallbackGroupName: String = DEFAULT_GROUP_NAME,
): String {
    val remoteParticipant = participants.firstOrNull { !it.isSelf }
    return if (isGroupCall) {
        channelSubjectOrNull ?: remoteParticipant?.displayName ?: fallbackGroupName
    } else {
        remoteParticipant?.displayName ?: primaryRemoteUserIdOrNull.orEmpty()
    }
}
