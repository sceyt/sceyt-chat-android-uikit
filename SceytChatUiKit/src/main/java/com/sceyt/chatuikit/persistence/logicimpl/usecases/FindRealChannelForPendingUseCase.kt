package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.toChannel

internal class FindRealChannelForPendingUseCase(
    private val channelDao: ChannelDao,
    private val channelsCache: ChannelsCache
) {

    suspend operator fun invoke(
        pendingChannel: SceytChannel,
        currentUserId: String?
    ): SceytChannel? {
        channelsCache.getRealChannelIdWithPendingChannelId(pendingChannel.id)?.let { realChannelId ->
            return channelDao.getChannelById(realChannelId)?.toChannel()
                ?: channelsCache.getOneOf(realChannelId)
        }

        pendingChannel.uri?.takeIf { it.isNotBlank() }?.let { uri ->
            channelDao.getChannelByUri(uri)?.toChannel()
                ?.takeIf { it.isRealChannelFor(pendingChannel) }
                ?.let { return it }
        }

        if (pendingChannel.type == ChannelTypeEnum.Direct.value) {
            if (pendingChannel.isSelf) {
                val id = currentUserId ?: return null
                return channelDao.getChannelByPeerId(id)
                    .map { it.toChannel() }
                    .firstOrNull { it.isSelf && it.isRealChannelFor(pendingChannel) }
            }

            pendingChannel.getPeer()?.id?.let { peerId ->
                return channelDao.getChannelByPeerId(peerId)
                    .map { it.toChannel() }
                    .firstOrNull { it.type == pendingChannel.type && it.isRealChannelFor(pendingChannel) }
            }
        }

        return null
    }

    private fun SceytChannel.isRealChannelFor(pendingChannel: SceytChannel): Boolean {
        return !pending && id != pendingChannel.id
    }
}
