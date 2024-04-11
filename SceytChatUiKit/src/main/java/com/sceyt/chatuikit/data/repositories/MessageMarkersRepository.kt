package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.message.Marker
import com.sceyt.chatuikit.data.models.SceytResponse

interface MessageMarkersRepository {
    suspend fun getMessageMarkers(messageId: Long, name: String,
                                  offset: Int, limit: Int): SceytResponse<List<Marker>>
}
