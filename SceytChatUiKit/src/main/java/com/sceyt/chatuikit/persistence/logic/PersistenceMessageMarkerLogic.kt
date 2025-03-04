package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMarker

interface PersistenceMessageMarkerLogic {
    suspend fun getMessageMarkers(messageId: Long, name: String,
                                  offset: Int, limit: Int): SceytResponse<List<SceytMarker>>

    suspend fun getMessageMarkersDb(messageId: Long, names: List<String>,
                                    offset: Int, limit: Int): List<SceytMarker>

    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessageMarkerEvent(data: MessageMarkerEventData)
}