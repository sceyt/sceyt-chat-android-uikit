package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.mappers.toChannel

internal class MergePendingDirectChannelsUseCase(
    private val channelDao: ChannelDao,
    private val migratePendingChannelToRealChannelUseCase: MigratePendingChannelToRealChannelUseCase
) {
    private val tag = "PendingChannelMigration"

    suspend operator fun invoke(
        realChannel: SceytChannel,
        currentUserId: String?
    ): SceytChannel {
        return invoke(listOf(realChannel), currentUserId).firstOrNull() ?: realChannel
    }

    suspend operator fun invoke(
        realChannels: List<SceytChannel>,
        currentUserId: String?
    ): List<SceytChannel> {
        currentUserId ?: return realChannels
        val keyedRealChannels = realChannels.mapNotNull { channel ->
            if (channel.pending) return@mapNotNull null
            channel.directMemberKey(currentUserId)?.let { key -> key to channel }
        }
        if (keyedRealChannels.isEmpty()) return realChannels

        val pendingChannelsByKey = channelDao.getPendingChannelsByType(ChannelTypeEnum.Direct.value)
            .map { it.toChannel() }
            .mapNotNull { channel ->
                channel.directMemberKey(currentUserId)?.let { key -> key to channel }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
        if (pendingChannelsByKey.isEmpty()) return realChannels

        val mergedByRealId = hashMapOf<Long, SceytChannel>()
        val mergedPendingIds = hashSetOf<Long>()
        keyedRealChannels.forEach { (key, realChannel) ->
            pendingChannelsByKey[key].orEmpty()
                .filter { pendingChannel ->
                    pendingChannel.id != realChannel.id && mergedPendingIds.add(pendingChannel.id)
                }
                .forEach { pendingChannel ->
                    val currentRealChannel = mergedByRealId[realChannel.id]
                        ?: channelDao.getChannelById(realChannel.id)?.toChannel()
                        ?: realChannel
                    SceytLog.d(
                        tag = tag,
                        message = "Merging pending direct channel: pendingId=${pendingChannel.id}, realId=${realChannel.id}"
                    )
                    mergedByRealId[realChannel.id] = migratePendingChannelToRealChannelUseCase(
                        pendingChannel = pendingChannel,
                        realChannel = currentRealChannel
                    )
                }
        }

        if (mergedByRealId.isEmpty()) return realChannels
        return realChannels.map { channel -> mergedByRealId[channel.id] ?: channel }
    }

    private fun SceytChannel.directMemberKey(currentUserId: String): List<String>? {
        if (type != ChannelTypeEnum.Direct.value) return null
        if (isSelf) return listOf(currentUserId)
        val memberIds = (members?.map { it.id }.orEmpty() + currentUserId)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        return memberIds.takeIf { it.size == 2 }
    }
}
