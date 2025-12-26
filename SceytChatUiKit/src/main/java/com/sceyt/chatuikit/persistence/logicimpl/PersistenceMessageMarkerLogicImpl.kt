package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Displayed
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Received
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.persistence.database.dao.MarkerDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.logic.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.mappers.toMarker
import com.sceyt.chatuikit.persistence.mappers.toMarkerEntity
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository

internal class PersistenceMessageMarkerLogicImpl(
        private val messageMarkersRepository: MessageMarkersRepository,
        private val markerDao: MarkerDao,
        private val messageDao: MessageDao,
) : PersistenceMessageMarkerLogic {

    override suspend fun getMessageMarkersDb(messageId: Long, names: List<String>, offset: Int, limit: Int): List<SceytMarker> {
        return markerDao.getMessageMarkers(messageId, names, offset, limit).map { it.toMarker() }
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        val marker = when (data.status) {
            Received -> MarkerType.Received.value
            Displayed -> MarkerType.Displayed.value
            else -> return
        }

        val markers = data.marker.messageIds.map { messageId ->
            MarkerEntity(messageId, data.from.id, marker, data.marker.createdAt)
        }
        messageDao.insertUserMarkersIfExistMessage(markers)
    }

    override suspend fun onMessageMarkerEvent(data: MessageMarkerEventData) {
        val markers = data.marker.messageIds.map {
            MarkerEntity(it, data.user.id, data.marker.name, data.marker.createdAt)
        }
        messageDao.insertUserMarkersIfExistMessage(markers)

    }

    override suspend fun getMessageMarkers(
            messageId: Long,
            name: String,
            offset: Int,
            limit: Int
    ): SceytResponse<List<SceytMarker>> {
        val response = messageMarkersRepository.getMessageMarkers(messageId, name, offset, limit)
        if (response is SceytResponse.Success) {
            response.data?.let { markers ->
                messageDao.insertUserMarkersIfExistMessage(markers.map { it.toMarkerEntity() })
            }
        }
        return response
    }
}