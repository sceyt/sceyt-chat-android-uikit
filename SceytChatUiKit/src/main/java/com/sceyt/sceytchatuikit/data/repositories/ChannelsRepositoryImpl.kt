package com.sceyt.sceytchatuikit.data.repositories

import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListFilterQueryType
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chat.models.channel.CreateChannelRequest
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.member.MemberListQuery
import com.sceyt.chat.models.user.BlockUserRequest
import com.sceyt.chat.models.user.UnBlockUserRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.operators.*
import com.sceyt.chat.sceyt_callbacks.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
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
            ChatClient.getClient().getChannel(id, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "getChannel error: ${e?.message}")
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
                    Log.e(TAG, "getChannelFromServerByUrl error: ${e?.message}")
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
                    Log.e(TAG, "getChannels error: ${e?.message}")
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
                    Log.e(TAG, "loadMoreChannels error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getAllChannels(limit: Int): Flow<SceytResponse<List<SceytChannel>>> = callbackFlow {
        val channelListQuery = ChannelListQuery.Builder()
            .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
            .withQueryParam(ChannelQueryParam(1,10,1,true))
            .order(getOrder())
            .limit(limit)
            .build()

        channelListQuery.loadNext(object : ChannelsCallback {
            override fun onResult(channels: MutableList<Channel>?) {
                if (channels.isNullOrEmpty())
                    trySend(SceytResponse.Success(emptyList()))
                else {
                    trySend(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    if (channels.size == limit)
                        channelListQuery.loadNext(this)
                    else channel.close()
                }
            }

            override fun onError(e: SceytException?) {
                trySend(SceytResponse.Error(e))
                channel.close()
                Log.e(TAG, "getAllChannels error: ${e?.message}")
            }
        })

        awaitClose()
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            DirectChannelOperator.createChannelRequest(user).execute(object : ChannelCallback {
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
        if (channelData.avatarUrl.isNullOrBlank().not() && channelData.avatarUploaded.not()) {
            when (val uploadResult = uploadAvatar(channelData.avatarUrl!!)) {
                is SceytResponse.Success -> {
                    channelData.avatarUrl = uploadResult.data
                    channelData.avatarUploaded = true
                }
                is SceytResponse.Error -> {
                    Log.e(TAG, "uploadAvatar error: ${uploadResult.message}")
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

                override fun onError(e: SceytException) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "createChannel error: ${e.message}")
                }
            }) ?: run {
                continuation.safeResume(SceytResponse.Error(SceytException(0, "Invalid channel type")))
                Log.e(TAG, "createChannel error: Invalid channel type")
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

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).markUsRead(object : ChannelCallback {
                override fun onResult(data: Channel) {
                    continuation.safeResume(SceytResponse.Success(data.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "markChannelAsRead error: ${e?.message}")
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
                    Log.e(TAG, "markChannelAsUnRead error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).leave(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "leaveChannel error: ${e?.message}")
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
                    Log.e(TAG, "clearHistory error: ${e?.message}")
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
                    Log.e(TAG, "hideChannel error: ${e?.message}")
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
                    Log.e(TAG, "blockUser error: ${e?.message}")
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
                    Log.e(TAG, "unblockUser error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun blockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).block(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "blockChannel error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun unBlockChannel(channelId: Long): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).unBlock(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.safeResume(SceytResponse.Success(channelId))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "unBlockChannel error: ${e?.message}")
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
                    Log.e(TAG, "deleteChannel error: ${e?.message}")
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
                    Log.e(TAG, "uploadAvatar error: ${e?.message}")
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
                    Log.e(TAG, "editChannel error: ${e?.message}")
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
                else -> continuation.safeResume(SceytResponse.Error(SceytException(0, "This is Direct channel")))
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
                        Log.e(TAG, "loadChannelMembers error: ${e?.message}")
                    }
                })
        }
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).addMembers(members, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "addMembersToChannel error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun changeChannelMemberRole(channelId: Long, vararg member: Member): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).changeMembersRole(member.toList(), object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "changeChannelMemberRole error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).changeOwner(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "changeChannelOwner error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).kickMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "deleteMember error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            GroupChannelOperator.build(channelId).blockMember(userId, object : ChannelCallback {
                override fun onResult(channel: Channel?) {
                    continuation.safeResume(SceytResponse.Success(channel?.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    Log.e(TAG, "blockAndDeleteMember error: ${e?.message}")
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
                    Log.e(TAG, "unMuteChannel error: ${e?.message}")
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
                    Log.e(TAG, "muteChannel error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        return suspendCancellableCoroutine { continuation ->
            PublicChannelOperator.build(channelId).join(object : ChannelCallback {
                override fun onResult(result: Channel) {
                    continuation.safeResume(SceytResponse.Success(result.toSceytUiChannel()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    Log.e(TAG, "join error: ${error?.message}")
                }
            })
        }
    }

    private fun createChannelListQuery(query: String? = null): ChannelListQuery {
        return ChannelListQuery.Builder()
            .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
            .order(getOrder())
            .query(query?.ifBlank { null })
            .withQueryParam(ChannelQueryParam(1,10,1,true))
            .limit(CHANNELS_LOAD_SIZE)
            .build()
    }
}