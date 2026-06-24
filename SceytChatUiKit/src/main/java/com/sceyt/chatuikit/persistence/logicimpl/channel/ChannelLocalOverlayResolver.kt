package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.data.models.channels.SceytChannel

internal class ChannelLocalOverlayResolver(
    private val localUnreadCountsManager: LocalUnreadCountsManager,
) {

    suspend fun fromServer(channel: SceytChannel): SceytChannel {
        return localUnreadCountsManager.seedChannel(channel)
    }

    suspend fun fromServer(channels: List<SceytChannel>): List<SceytChannel> {
        return localUnreadCountsManager.seedChannels(channels)
    }

    suspend fun fromStore(channel: SceytChannel): SceytChannel {
        return localUnreadCountsManager.applyLocalState(channel)
    }

    suspend fun fromStore(channels: List<SceytChannel>): List<SceytChannel> {
        return localUnreadCountsManager.applyLocalState(channels)
    }
}
