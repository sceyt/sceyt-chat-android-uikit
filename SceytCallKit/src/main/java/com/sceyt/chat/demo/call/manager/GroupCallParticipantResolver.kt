package com.sceyt.chat.demo.call.manager

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.fold
import kotlinx.coroutines.flow.firstOrNull

internal class GroupCallParticipantResolver(
    private val currentUserIdProvider: () -> String? = { SceytChatUIKit.currentUserId },
) {

    suspend fun getChannel(channelId: Long): SceytChannel? {
        val channelInteractor = SceytChatUIKit.chatUIFacade.channelInteractor
        return channelInteractor.getChannelFromDb(channelId)
            ?: channelInteractor.getChannelFromServer(channelId).fold(
                onSuccess = { it },
                onError = { null }
            )
    }

    internal fun resolveParticipantIds(channel: SceytChannel): List<String> {
        val currentUserId = currentUserIdProvider()
        val allMembers = channel.members ?: return emptyList()
        return allMembers
            .map(SceytMember::id)
            .filter { it.isNotBlank() && it != currentUserId }
            .distinct()
    }
}
