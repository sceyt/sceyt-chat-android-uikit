package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse

internal class MessagePagingRetryState {
    private var retryPrev = false
    private var retryNext = false

    fun onServerResponse(response: PaginationResponse.ServerResponse<*>) {
        when (response.data) {
            is SceytResponse.Success -> clear(response.loadType)
            is SceytResponse.Error -> markFailed(response.loadType)
        }
    }

    fun canRetryPrev(
        loadingFromDb: Boolean,
        loadingFromServer: Boolean,
    ): Boolean {
        return retryPrev && !loadingFromDb && !loadingFromServer
    }

    fun canRetryNext(
        loadingFromDb: Boolean,
        loadingFromServer: Boolean,
    ): Boolean {
        return retryNext && !loadingFromDb && !loadingFromServer
    }

    fun reset() {
        retryPrev = false
        retryNext = false
    }

    private fun markFailed(loadType: PaginationResponse.LoadType) {
        when (loadType) {
            LoadPrev -> retryPrev = true
            LoadNext -> retryNext = true
            LoadNear -> {
                retryPrev = true
                retryNext = true
            }

            LoadNewest -> Unit
        }
    }

    private fun clear(loadType: PaginationResponse.LoadType) {
        when (loadType) {
            LoadPrev -> retryPrev = false
            LoadNext -> retryNext = false
            LoadNear, LoadNewest -> reset()
        }
    }
}
