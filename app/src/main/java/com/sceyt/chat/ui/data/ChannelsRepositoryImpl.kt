package com.sceyt.chat.ui.data

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.*
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.member.MemberListQuery
import com.sceyt.chat.sceyt_callbacks.*
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventsObserverService
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelsRepositoryImpl : ChannelsRepository {
    //todo need to add DI
    //private val channelEventsService = ChannelEventsObserverService()

    override val onMessageFlow = ChannelEventsObserverService.onMessageFlow
    override val onMessageStatusFlow = ChannelEventsObserverService.onMessageStatusFlow

    override val onMessageEditedOrDeleteFlow = ChannelEventsObserverService.onMessageEditedOrDeletedFlow
        .filterNotNull()

    override val onChannelEvenFlow = ChannelEventsObserverService.onChannelEventFlow

    private val query =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(getOrder())
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

    private val querySearch =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(getOrder())
                .filterKey(ChannelListQuery.ChannelListFilterKey.ListQueryChannelFilterKeySubject)
                .queryType(ChannelListQuery.ChannelListFilterQueryType.ListQueryFilterContains)

    private lateinit var memberListQuery: MemberListQuery

    private fun getOrder(): ChannelListQuery.ChannelListOrder {
        return if (SceytUIKitConfig.sortChannelsBy == SceytUIKitConfig.ChannelSortType.ByLastMsg)
            ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage
        else ChannelListQuery.ChannelListOrder.ListQueryChannelOrderCreatedAt
    }

    private fun createMemberListQuery(channelId: Long): MemberListQuery {
        return MemberListQuery.Builder(channelId)
            .limit(SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE)
            .orderType(MemberListQuery.QueryOrderType.ListQueryOrderAscending)
            .build()
    }

    override suspend fun getChannel(id: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().getChannel(id, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun getChannels(offset: Int): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            query.offset = offset
            query.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.resume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun searchChannels(offset: Int, query: String): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val channelListQuery = querySearch
                .query(query)
                .offset(offset)
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

            channelListQuery.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.resume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun leaveChannel(channel: GroupChannel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.leave(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun clearHistory(channel: Channel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.clearHistory(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun blockChannel(channel: GroupChannel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.block(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun deleteChannel(channel: Channel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.delete(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun uploadAvatar(avatarUri: String): SceytResponse<String> {
        return suspendCoroutine { continuation ->
            ChatClient.getClient().upload(avatarUri, object : ProgressCallback {
                override fun onResult(pct: Float) {
                }

                override fun onError(e: SceytException?) {}
            }, object : UrlCallback {

                override fun onResult(url: String) {
                    continuation.resume(SceytResponse.Success(url))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun editChannel(channel: Channel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            val channelCallback = object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            }

            when (channel) {
                is PrivateChannel -> {
                    channel.updateChannel(newSubject, channel.metadata, channel.label,
                        avatarUrl ?: "", channelCallback)
                }
                is PublicChannel -> {
                    channel.update(channel.uri, newSubject, channel.metadata, channel.label,
                        avatarUrl ?: "", channelCallback)
                }
                else -> continuation.resume(SceytResponse.Error("This is Direct channel"))
            }
        }
    }

    override suspend fun loadChannelMembers(channelId: Long): SceytResponse<List<SceytMember>> {
        return suspendCancellableCoroutine { continuation ->
            if (::memberListQuery.isInitialized.not())
                memberListQuery = createMemberListQuery(channelId)

            memberListQuery.loadNext(object : MembersCallback {
                override fun onResult(members: MutableList<Member>) {
                    continuation.resume(SceytResponse.Success(members.map { it.toSceytMember() }))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun addMembersToChannel(channel: GroupChannel, members: List<Member>): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            channel.addMembers(members, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun changeChannelMemberRole(channel: GroupChannel, member: Member): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            channel.changeMemberRole(member, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun changeChannelOwner(channel: GroupChannel, userId: String): SceytResponse<String> {
        return suspendCancellableCoroutine { continuation ->
            channel.changeOwner(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(userId))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}