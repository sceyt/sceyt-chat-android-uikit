package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.persistence.dao.MarkerDao
import com.sceyt.chatuikit.persistence.interactor.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.mappers.toMarker
import com.sceyt.chatuikit.persistence.mappers.toMarkerEntity
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository

class PersistenceMessageMarkerLogicImpl(
        private val messageMarkersRepository: MessageMarkersRepository,
        private val markerDao: MarkerDao
) : PersistenceMessageMarkerLogic {

    override suspend fun getMessageMarkersDb(messageId: Long, names: List<String>, offset: Int, limit: Int): List<SceytMarker> {
        return markerDao.getMessageMarkers(messageId, names, offset, limit).map { it.toMarker() }
    }

    override suspend fun getMessageMarkers(messageId: Long, name: String, offset: Int, limit: Int): SceytResponse<List<SceytMarker>> {
        val response = messageMarkersRepository.getMessageMarkers(messageId, name, offset, limit)
        if (response is SceytResponse.Success) {
            response.data?.let { markers ->
                markerDao.insertUserMarkers(markers.map { it.toMarkerEntity() })
            }
        }
        return response
    }
}