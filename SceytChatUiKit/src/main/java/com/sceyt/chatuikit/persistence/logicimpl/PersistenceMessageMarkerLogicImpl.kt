package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.message.DeliveryStatus.Displayed
import com.sceyt.chat.models.message.DeliveryStatus.Received
import com.sceyt.chatuikit.data.channeleventobserver.MessageMarkerEventData
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.persistence.dao.MarkerDao
import com.sceyt.chatuikit.persistence.dao.MessageDao
import com.sceyt.chatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.interactor.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.mappers.toMarker
import com.sceyt.chatuikit.persistence.mappers.toMarkerEntity
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository

class PersistenceMessageMarkerLogicImpl(
        private val messageMarkersRepository: MessageMarkersRepository,
        private val markerDao: MarkerDao,
        private val messageDao: MessageDao,
) : PersistenceMessageMarkerLogic {

    override suspend fun getMessageMarkersDb(messageId: Long, names: List<String>, offset: Int, limit: Int): List<SceytMarker> {
        return markerDao.getMessageMarkers(messageId, names, offset, limit).map { it.toMarker() }
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        val marker = when (data.status) {
            Received -> MarkerTypeEnum.Received.value()
            Displayed -> MarkerTypeEnum.Displayed.value()
            else -> return
        }

        val existMessageIds = messageDao.getExistMessageByIds(data.marker.messageIds)
        val markers = existMessageIds.map { messageId ->
            MarkerEntity(messageId, data.from.id, marker, data.marker.createdAt)
        }
        markerDao.insertMany(markers)
    }

    override suspend fun onMessageMarkerEvent(data: MessageMarkerEventData) {
        val existMessageIds = messageDao.getExistMessageByIds(data.marker.messageIds)
        val markers = existMessageIds.map {
            MarkerEntity(it, data.user.id, data.marker.name, data.marker.createdAt)
        }
        markerDao.insertMany(markers)
    }

    override suspend fun getMessageMarkers(messageId: Long, name: String, offset: Int, limit: Int): SceytResponse<List<SceytMarker>> {
        val response = messageMarkersRepository.getMessageMarkers(messageId, name, offset, limit)
        if (response is SceytResponse.Success) {
            response.data?.let { markers ->
                markerDao.insertMany(markers.map { it.toMarkerEntity() })
            }
        }
        return response
    }
}