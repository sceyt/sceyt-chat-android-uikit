package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Owns channel-member tracking that used to live inside [MessageListViewModel]: applying member
 * add/kick events to the channel and loading members on demand.
 */
internal class ChannelMemberController(
    private val scope: CoroutineScope,
    private val memberInteractor: ChannelMemberInteractor,
    private val currentChannel: () -> SceytChannel,
    private val updateChannel: (SceytChannel.() -> SceytChannel) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun onMemberEvent(eventData: ChannelMembersEventData) {
        val sceytMembers = eventData.members
        val channelMembers = currentChannel().members?.toMutableList() ?: arrayListOf()

        when (eventData.eventType) {
            ChannelMembersEventEnum.Added -> {
                channelMembers.addAll(sceytMembers)
                updateChannel {
                    copy(
                        members = channelMembers,
                        memberCount = memberCount + sceytMembers.size
                    )
                }
            }

            ChannelMembersEventEnum.Kicked -> {
                channelMembers.removeAll(sceytMembers)
                updateChannel {
                    copy(
                        members = channelMembers,
                        memberCount = memberCount - sceytMembers.size
                    )
                }
            }

            else -> Unit
        }
    }

    fun loadIfNeeded() {
        scope.launch(ioDispatcher) {
            val count = memberInteractor.getMembersCountFromDb(currentChannel().id)
            if (currentChannel().memberCount > count)
                loadMembers(offset = 0, nextToken = "", role = null).collect()
        }
    }

    fun loadMembers(
        offset: Int,
        nextToken: String,
        role: String?,
    ): Flow<PaginationResponse<SceytMember>> {
        return memberInteractor.loadChannelMembers(
            channelId = currentChannel().id,
            offset = offset,
            nextToken = nextToken,
            role = role
        )
    }

    fun loadAll() {
        scope.launch(ioDispatcher) {
            suspend fun load(
                offset: Int,
                nextToken: String,
            ): PaginationResponse.ServerResponse<SceytMember>? {
                return memberInteractor.loadChannelMembers(currentChannel().id, offset, nextToken, null)
                    .firstOrNull {
                        it is PaginationResponse.ServerResponse
                    } as? PaginationResponse.ServerResponse<SceytMember>
            }

            val count = memberInteractor.getMembersCountFromDb(currentChannel().id)
            if (currentChannel().memberCount > count) {
                var offset = 0
                var rest = load(0, "")
                while (rest?.hasNext == true) {
                    offset += rest.data.data?.size ?: return@launch
                    rest = load(offset, rest.nextToken)
                }
            }
        }
    }
}