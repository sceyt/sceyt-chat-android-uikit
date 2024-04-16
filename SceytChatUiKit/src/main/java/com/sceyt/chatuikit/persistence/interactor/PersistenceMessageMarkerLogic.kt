package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMarker

interface PersistenceMessageMarkerLogic {
    suspend fun getMessageMarkers(messageId: Long, name: String,
                                  offset: Int, limit: Int): SceytResponse<List<SceytMarker>>

    suspend fun getMessageMarkersDb(messageId: Long, names: List<String>,
                                    offset: Int, limit: Int): List<SceytMarker>
}