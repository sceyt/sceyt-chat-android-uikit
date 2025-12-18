package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository

class GetCommonGroupsUseCase(
    private val channelsRepository: ChannelsRepository
) {
    suspend fun getCommonGroups(
        userId: String,
    ): SceytPagingResponse<List<SceytChannel>> {
        return channelsRepository.getCommonGroups(userId)
    }

    suspend fun loadMore(): SceytPagingResponse<List<SceytChannel>> {
        return channelsRepository.loadMoreCommonGroups()
    }
}