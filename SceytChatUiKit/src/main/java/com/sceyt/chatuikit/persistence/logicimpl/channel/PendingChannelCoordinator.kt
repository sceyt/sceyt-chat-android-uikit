package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CreatePendingChannelUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindExistingChannelByMembersUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.FindRealChannelForPendingUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.InsertChannelWithMembersUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MergePendingDirectChannelsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.MigratePendingChannelToRealChannelUseCase
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owns the mutex and orchestration for pending-channel creation and migration. */
internal class PendingChannelCoordinator(
    private val channelsRepository: ChannelsRepository,
    private val channelDao: ChannelDao,
    private val channelsCache: ChannelsCache,
    private val findExistingChannelByMembersUseCase: FindExistingChannelByMembersUseCase,
    private val createPendingChannelUseCase: CreatePendingChannelUseCase,
    private val findRealChannelForPendingUseCase: FindRealChannelForPendingUseCase,
    private val migratePendingChannelToRealChannelUseCase: MigratePendingChannelToRealChannelUseCase,
    private val mergePendingDirectChannelsUseCase: MergePendingDirectChannelsUseCase,
    private val insertChannelWithMembersUseCase: InsertChannelWithMembersUseCase,
) {
    private val mutex = Mutex()

    suspend fun findOrCreateByMembers(
        data: CreateChannelData,
        currentUserId: String?,
    ): SceytResponse<SceytChannel> = mutex.withLock {
        findExistingChannelByMembersUseCase(data, currentUserId)?.let { channel ->
            if (channel.pending) channelsCache.upsertPendingChannel(channel)
            SceytResponse.Success(channel)
        } ?: createPendingChannelUseCase(data, currentUserId)
    }

    suspend fun findOrCreateByUri(
        data: CreateChannelData,
        currentUserId: String?,
    ): SceytResponse<SceytChannel> {
        if (data.uri.isBlank()) return SceytResponse.Error(SceytException(0, "Uri is empty"))

        mutex.withLock {
            channelDao.getChannelByUri(data.uri)?.toChannel()?.let { channel ->
                if (channel.pending) channelsCache.upsertPendingChannel(channel)
                SceytResponse.Success(channel)
            }
        }?.let { return it }

        val response = channelsRepository.getChannelByUri(data.uri)
        return mutex.withLock {
            val existingChannel = channelDao.getChannelByUri(data.uri)?.toChannel()
            if (response is SceytResponse.Success && response.data != null) {
                SceytResponse.Success(
                    saveFetchedChannelByUriUnlocked(
                        uri = data.uri,
                        channel = response.data,
                        currentUserId = currentUserId
                    )
                )
            } else {
                existingChannel?.let { channel ->
                    if (channel.pending) channelsCache.upsertPendingChannel(channel)
                    SceytResponse.Success(channel)
                } ?: createPendingChannelUseCase(data, currentUserId)
            }
        }
    }

    /**
     * Persists a fetched channel page and merges it with pending direct channels atomically,
     * so a concurrent [createRealFromPending] cannot slot between the write and the merge.
     */
    suspend fun persistAndMergeFetchedChannels(
        realChannels: List<SceytChannel>,
        links: List<UserChatLinkEntity>,
        currentUserId: String?,
    ): List<SceytChannel> = mutex.withLock {
        val pendingChannelsByRealId = preparePendingChannelsForUriReconciliation(realChannels)
        channelDao.insertChannelsAndLinks(realChannels.map { it.toChannelEntity() }, links)
        val uriMergedChannels = migratePendingChannelsMatchedByUri(
            realChannels = realChannels,
            pendingChannelsByRealId = pendingChannelsByRealId
        )
        if (realChannels.none { !it.pending }) return@withLock uriMergedChannels
        mergePendingDirectChannelsUseCase(uriMergedChannels, currentUserId)
    }

    private suspend fun preparePendingChannelsForUriReconciliation(
        realChannels: List<SceytChannel>,
    ): Map<Long, SceytChannel> {
        val pendingChannelsByRealId = mutableMapOf<Long, SceytChannel>()
        val claimedPendingIds = mutableSetOf<Long>()

        realChannels.forEach { realChannel ->
            if (realChannel.pending) return@forEach
            val uri = realChannel.uri?.takeIf { it.isNotBlank() } ?: return@forEach
            val pendingChannel = channelDao.getChannelByUri(uri)?.toChannel()
                ?.takeIf { it.pending && it.id != realChannel.id }
                ?: return@forEach
            if (claimedPendingIds.add(pendingChannel.id)) {
                pendingChannelsByRealId[realChannel.id] = pendingChannel
            }
        }

        pendingChannelsByRealId.values.forEach { pendingChannel ->
            channelDao.updateUri(pendingChannel.id, null)
        }
        return pendingChannelsByRealId
    }

    private suspend fun migratePendingChannelsMatchedByUri(
        realChannels: List<SceytChannel>,
        pendingChannelsByRealId: Map<Long, SceytChannel>,
    ): List<SceytChannel> = realChannels.map { realChannel ->
        pendingChannelsByRealId[realChannel.id]?.let { pendingChannel ->
            migratePendingChannelToRealChannelUseCase(pendingChannel, realChannel)
        } ?: realChannel
    }

    suspend fun createRealFromPending(
        channel: SceytChannel,
        currentUserId: String?,
    ): SceytResponse<SceytChannel> {
        mutex.withLock {
            findRealAndMigratePendingChannelUnlocked(channel, currentUserId)
        }?.let { return SceytResponse.Success(it) }

        val response = channelsRepository.createChannel(channel.toCreateChannelData())

        return mutex.withLock {
            findRealAndMigratePendingChannelUnlocked(channel, currentUserId)?.let { realChannel ->
                SceytResponse.Success(realChannel)
            } ?: response.fold(
                onSuccess = { newChannel ->
                    if (newChannel == null)
                        return@fold createErrorResponse("create channel response is success, but channel is null")

                    val pendingChannel = channelDao.getChannelById(channel.id)?.toChannel()
                    insertChannelWithMembersUseCase(newChannel)
                    val mergedChannel = if (pendingChannel?.pending == true)
                        migratePendingChannelToRealChannelUseCase(pendingChannel, newChannel)
                    else newChannel

                    SceytResponse.Success(mergedChannel)
                },
                onError = { exception -> SceytResponse.Error(exception) }
            )
        }
    }

    suspend fun getRealByUriAndReconcile(
        uri: String,
        currentUserId: String?,
    ): SceytResponse<SceytChannel?> {
        val response = channelsRepository.getChannelByUri(uri)
        if (response !is SceytResponse.Success || response.data == null) return response

        return mutex.withLock {
            SceytResponse.Success(
                saveFetchedChannelByUriUnlocked(
                    uri = uri,
                    channel = response.data,
                    currentUserId = currentUserId
                )
            )
        }
    }

    private suspend fun saveFetchedChannelByUriUnlocked(
        uri: String,
        channel: SceytChannel,
        currentUserId: String?,
    ): SceytChannel {
        val pendingChannel = channelDao.getChannelByUri(uri)?.toChannel()
            ?.takeIf { it.pending && it.id != channel.id }
        if (pendingChannel != null) channelDao.updateUri(pendingChannel.id, null)

        insertChannelWithMembersUseCase(channel)
        val mergedChannel = if (pendingChannel != null) {
            migratePendingChannelToRealChannelUseCase(pendingChannel, channel)
        } else {
            mergePendingDirectChannelsUseCase(channel, currentUserId)
        }
        notifyPendingUriChannelCreated(uri, mergedChannel)
        return mergedChannel
    }

    private suspend fun findRealAndMigratePendingChannelUnlocked(
        channel: SceytChannel,
        currentUserId: String?,
    ): SceytChannel? {
        val realChannel = findRealChannelForPendingUseCase(channel, currentUserId) ?: return null
        val pendingChannel = channelDao.getChannelById(channel.id)?.toChannel()
        return if (pendingChannel?.pending == true)
            migratePendingChannelToRealChannelUseCase(pendingChannel, realChannel)
        else realChannel
    }

    private suspend fun notifyPendingUriChannelCreated(uri: String, channel: SceytChannel) {
        channelsCache.getCachedData().entries.forEach { (_, map) ->
            map.entries.find { it.value.uri == uri }?.let { (id, cachedChannel) ->
                if (cachedChannel.pending) channelsCache.pendingChannelCreated(id, channel)
            }
        }
    }

    private fun SceytChannel.toCreateChannelData() = CreateChannelData(
        type = type,
        uri = uri.orEmpty(),
        subject = subject.orEmpty(),
        avatarUrl = avatarUrl.orEmpty(),
        metadata = metadata.orEmpty(),
        members = members.orEmpty()
    )
}
