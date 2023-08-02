package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.SceytException

sealed class GetAllChannelsResponse {
    object SuccessfullyFinished : GetAllChannelsResponse()
    data class Proportion(val channels: List<SceytChannel>) : GetAllChannelsResponse()
    data class Error(val error: SceytException?) : GetAllChannelsResponse()
}