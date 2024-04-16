package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMarker

interface MessageMarkersRepository {
    suspend fun getMessageMarkers(messageId: Long, name: String,
                                  offset: Int, limit: Int): SceytResponse<List<SceytMarker>>
}
