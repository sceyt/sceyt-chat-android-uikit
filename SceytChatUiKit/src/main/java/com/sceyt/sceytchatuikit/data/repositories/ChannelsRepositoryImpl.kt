package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Builder
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListFilterQueryType
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chat.models.channel.CreateChannelRequest
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.member.MemberListQuery
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.BlockUserRequest
import com.sceyt.chat.models.user.UnBlockUserRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.operators.ChannelOperator
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ChannelCallback
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import com.sceyt.chat.sceyt_callbacks.MembersCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chat.sceyt_callbacks.UsersCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class ChannelsRepositoryImpl : ChannelsRepository {

    private lateinit var channelsQuery: ChannelListQuery

    private fun getOrder(): ChannelListQuery.ChannelListOrder {
        return if (SceytKitConfig.sortChannelsBy == SceytKitConfig.ChannelSortType.ByLastMsg)
            ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage
        else ChannelListQuery.ChannelListOrder.ListQueryChannelOrderCreatedAt
    }

    private fun createMemberListQuery(channelId: Long, offset: Int, role: String?): MemberListQuery {
        return MemberListQuery.Builder(channelId)
            .limit(SceytKitConfig.CHANNELS_MEMBERS_LOAD_SIZE)
            .orderType(MemberListQuery.QueryOrderType.ListQueryOrderAscending)
            .order(MemberListQuery.MemberListOrder.MemberListQueryOrderKeyUserName)
            .apply {
                if (!role.isNullOrBlank())
                    withRole(role)
            }
            .offset(offset)
            .build()
    }

    override suspend fun getChannel(id: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.getChannelRequest(id, channelQueryParam).execute(object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val query = ChannelListQuery.Builder()
                .limit(1)
                .filterKey(ChannelListQuery.ChannelListFilterKey.ListQueryChannelFilterKeyURI)
                .queryType(ChannelListFilterQueryType.ListQueryFilterEqual)
                .query(url)
                .build()

            query.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.safeResume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannelFromServerByUrl error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun getChannels(query: String): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val channelListQuery = createChannelListQuery(query).also { channelsQuery = it }

            channelListQuery.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.safeResume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannels error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val query = if (::channelsQuery.isInitialized)
                channelsQuery
            else createChannelListQuery().also { channelsQuery = it }

            query.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.safeResume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadMoreChannels error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun getAllChannels(limit: Int): Flow<GetAllChannelsResponse> = callbackFlow {
        val channelListQuery = ChannelListQuery.Builder()
            .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
            .withQueryParam(channelQueryParam)
            .order(getOrder())
            .limit(limit)
            .build()

        channelListQuery.loadNext(object : ChannelsCallback {
            override fun onResult(channels: MutableList<Channel>?) {
                if (channels.isNullOrEmpty()) {
                    trySend(GetAllChannelsResponse.SuccessfullyFinished)
                    channel.close()
                } else {
                    trySend(GetAllChannelsResponse.Proportion(channels.map { it.toSceytUiChannel() }))
                    if (channels.size == limit)
                        channelListQuery.loadNext(this)
                    else {
                        trySend(GetAllChannelsResponse.SuccessfullyFinished)
                        channel.close()
                    }
                }
            }

            override fun onError(e: SceytException?) {
                trySend(GetAllChannelsResponse.Error(e))
                channel.close()
                SceytLog.e(TAG, "getAllChannels error: ${e?.message}, code: ${e?.code}")
            }
        })

        awaitClose()
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            Builder(ChannelTypeEnum.Direct.getString())
                .withMembers(arrayListOf(Member(Role("Admin"), user)))
                .execute(object : ChannelCallback {
                    override fun onResult(channel: Channel) {
                        continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.safeResume(SceytResponse.Error(e))
                    }
                })
        }
    }

    override suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel> {
        if (channelData.avatarUrl.isBlank().not() && channelData.avatarUploaded.not()) {
            when (val uploadResult = uploadAvatar(channelData.avatarUrl)) {
                is SceytResponse.Success -> {
                    channelData.avatarUrl = uploadResult.data ?: ""
                    channelData.avatarUploaded = true
                }

                is SceytResponse.Error -> {
                    SceytLog.e(TAG, "uploadAvatar error: ${uploadResult.message}, code: ${uploadResult.code}")
                    return SceytResponse.Error(uploadResult.exception)
                }
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val createChannelRequest = initCreateChannelRequest(channelData)

            createChannelRequest?.execute(object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "createChannel error: ${e?.message}, code: ${e?.code}")
                }
            }) ?: run {
                continuation.safeResume(SceytResponse.Error(SceytException(0, "Invalid channel type")))
                SceytLog.e(TAG, "createChannel error: Invalid channel type")
            }
        }
    }

    private fun initCreateChannelRequest(channelData: CreateChannelData): CreateChannelRequest? {
        return Builder(channelData.channelType.getString())
            .withMembers(channelData.members)
            .withUri(channelData.uri)
            .withAvatarUrl(channelData.avatarUrl)
            .withSubject(channelData.subject)
            .withMetadata(channelData.metadata)
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markUsRead(object : ChannelCallback {
                override fun onResult(data: Channel) {
                    continuation.safeResume(SceytResponse.Success(data.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "markChannelAsRead error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markUsUnread(object : ChannelCallback {
                override fun onResult(data: Channel) {
                    continuation.safeResume(SceytResponse.Success(data.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "markChannelAsUnRead error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).leave(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "leaveChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).deleteAllChannelMessages(forEveryone, object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "clearHistory error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).hide(object : ChannelCallback {
                override fun onResult(data: Channel) {
                    continuation.safeResume(SceytResponse.Success(data.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "hideChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun blockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            BlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "blockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun unblockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            UnBlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unblockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun blockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).block(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "blockChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun unBlockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).unBlock(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unBlockChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).delete(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "deleteChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun uploadAvatar(avatarUri: String): SceytResponse<String> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().upload(avatarUri, object : ProgressCallback {
                override fun onResult(pct: Float) {
                }

                override fun onError(e: SceytException?) {}
            }, object : UrlCallback {

                override fun onResult(url: String) {
                    continuation.safeResume(SceytResponse.Success(url))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "uploadAvatar error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            val channelCallback = object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "editChannel error: ${e?.message}, code: ${e?.code}")
                }
            }

            when (data.channelType) {
                ChannelTypeEnum.Private, ChannelTypeEnum.Direct -> {
                    ChannelOperator.build(channelId).updateChannel("", data.newSubject, data.metadata,
                        data.avatarUrl ?: "", channelCallback)
                }

                ChannelTypeEnum.Public -> {
                    ChannelOperator.build(channelId).updateChannel(data.channelUri, data.newSubject, data.metadata,
                        data.avatarUrl ?: "", channelCallback)
                }
            }
        }
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int, role: String?): SceytResponse<List<SceytMember>> {
        return suspendCancellableCoroutine { continuation ->
            createMemberListQuery(channelId, offset, role)
                .loadNext(object : MembersCallback {
                    override fun onResult(members: MutableList<Member>?) {
                        if (members.isNullOrEmpty())
                            continuation.safeResume(SceytResponse.Success(emptyList()))
                        else
                            continuation.safeResume(SceytResponse.Success(members.map { it.toSceytMember() }))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "loadChannelMembers error: ${e?.message}, code: ${e?.code}")
                    }
                })
        }
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).addMembers(members, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "addMembersToChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun changeChannelMemberRole(channelId: Long, vararg member: Member): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).changeMembersRole(member.toList(), object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "changeChannelMemberRole error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).changeOwner(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "changeChannelOwner error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).kickMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "deleteMember error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).blockMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.safeResume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "blockAndDeleteMember error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).unMute(object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.safeResume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unMuteChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).mute(muteUntil, object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.safeResume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "muteChannel error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).join(object : ChannelCallback {
                override fun onResult(result: Channel) {
                    continuation.safeResume(SceytResponse.Success(result.toSceytUiChannel()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "join error: ${error?.message}, code: ${error?.code}")
                }
            })
        }
    }

    private fun createChannelListQuery(query: String? = null): ChannelListQuery {
        return ChannelListQuery.Builder()
            .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
            .order(getOrder())
            .query(query?.ifBlank { null })
            .withQueryParam(channelQueryParam)
            .limit(CHANNELS_LOAD_SIZE)
            .build()
    }

    private val channelQueryParam = ChannelQueryParam(1, 10, 1, true)
}