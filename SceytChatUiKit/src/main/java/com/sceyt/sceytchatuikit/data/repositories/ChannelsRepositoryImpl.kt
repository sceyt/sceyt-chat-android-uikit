package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.*
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.member.MemberListQuery
import com.sceyt.chat.models.user.BlockUserRequest
import com.sceyt.chat.models.user.UnBlockUserRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.operators.*
import com.sceyt.chat.sceyt_callbacks.*
import com.sceyt.sceytchatuikit.data.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelsRepositoryImpl : ChannelsRepository {

    private lateinit var channelsQuery: ChannelListQuery

    private fun getOrder(): ChannelListQuery.ChannelListOrder {
        return if (SceytUIKitConfig.sortChannelsBy == SceytUIKitConfig.ChannelSortType.ByLastMsg)
            ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage
        else ChannelListQuery.ChannelListOrder.ListQueryChannelOrderCreatedAt
    }

    private fun createMemberListQuery(channelId: Long, offset: Int): MemberListQuery {
        return MemberListQuery.Builder(channelId)
            .limit(SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE)
            .orderType(MemberListQuery.QueryOrderType.ListQueryOrderAscending)
            .order(MemberListQuery.MemberListOrder.MemberListQueryOrderKeyUserName)
            .offset(offset)
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

    override suspend fun getChannels(query: String): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val channelListQuery = ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(getOrder())
                .query(query.ifBlank { null })
                .limit(20)
                .build().also { channelsQuery = it }

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

    override suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            channelsQuery.loadNext(object : ChannelsCallback {
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

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            DirectChannelOperator.createChannelRequest(user).execute(object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel> {
        if (channelData.avatarUrl.isNullOrBlank().not() && channelData.avatarUploaded.not()) {
            val uploadResult = uploadAvatar(channelData.avatarUrl!!)
            if (uploadResult is SceytResponse.Success) {
                channelData.avatarUrl = uploadResult.data
                channelData.avatarUploaded = true
            } else return SceytResponse.Error(uploadResult.message)
        }

        return suspendCancellableCoroutine { continuation ->
            val createChannelRequest = initCreateChannelRequest(channelData)

            createChannelRequest?.execute(object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException) {
                    continuation.resume(SceytResponse.Error(e.message))
                }
            }) ?: run {
                continuation.resume(SceytResponse.Error("Invalid channel type"))
            }
        }
    }

    private fun initCreateChannelRequest(channelData: CreateChannelData): CreateChannelRequest? {
        val createChannelRequest: CreateChannelRequest? = when (channelData.channelType) {
            Channel.Type.Private -> PrivateChannelOperator.createChannelRequest()
                .withMembers(channelData.members)
                .withAvatarUrl(channelData.avatarUrl ?: "")
                .withLabel(channelData.label ?: "")
                .withSubject(channelData.subject ?: "")
                .withMetadata(channelData.metadata ?: "")
            Channel.Type.Public -> PublicChannelOperator.createChannelRequest()
                .withMembers(channelData.members)
                .withUri(channelData.uri ?: "")
                .withAvatarUrl(channelData.avatarUrl ?: "")
                .withLabel(channelData.label ?: "")
                .withSubject(channelData.subject ?: "")
                .withMetadata(channelData.metadata ?: "")
            else -> null
        }
        return createChannelRequest
    }

    override suspend fun markAsRead(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markAllMessagesAsRead(object : ChannelCallback {
                override fun onResult(data: Channel) {
                    continuation.resume(SceytResponse.Success(data.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).leave(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun clearHistory(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).clearHistory(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun blockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            BlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.resume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }

            })
        }
    }

    override suspend fun unblockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            UnBlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.resume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun blockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).block(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.resume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun unBlockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).unBlock(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.resume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).delete(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channelId))
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

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            val channelCallback = object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            }

            when (data.channelType) {
                ChannelTypeEnum.Private -> {
                    PrivateChannelOperator.build(channelId).updateChannel(data.newSubject, data.metadata, data.label,
                        data.avatarUrl ?: "", channelCallback)
                }
                ChannelTypeEnum.Public -> {
                    PublicChannelOperator.build(channelId).update(data.channelUrl, data.newSubject, data.metadata, data.label,
                        data.avatarUrl ?: "", channelCallback)
                }
                else -> continuation.resume(SceytResponse.Error("This is Direct channel"))
            }
        }
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int): SceytResponse<List<SceytMember>> {
        return suspendCancellableCoroutine { continuation ->
            createMemberListQuery(channelId, offset)
                .loadNext(object : MembersCallback {
                    override fun onResult(members: MutableList<Member>?) {
                        if (members.isNullOrEmpty())
                            continuation.resume(SceytResponse.Success(emptyList()))
                        else
                            continuation.resume(SceytResponse.Success(members.map { it.toSceytMember() }))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.resume(SceytResponse.Error(e?.message))
                    }
                })
        }
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).addMembers(members, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun changeChannelMemberRole(channelId: Long, member: Member): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).changeMemberRole(member, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).changeOwner(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).kickMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.resume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).blockMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.resume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).unMute(object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.resume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).mute(muteUntil, object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.resume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}