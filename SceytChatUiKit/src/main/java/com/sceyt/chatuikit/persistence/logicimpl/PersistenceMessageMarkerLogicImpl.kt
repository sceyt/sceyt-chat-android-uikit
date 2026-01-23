package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Displayed
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Received
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.persistence.database.dao.MarkerDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.logic.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toMarker
import com.sceyt.chatuikit.persistence.mappers.toMarkerEntity
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PersistenceMessageMarkerLogicImpl(
    private val messageMarkersRepository: MessageMarkersRepository,
    private val markerDao: MarkerDao,
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) : PersistenceMessageMarkerLogic {

    override suspend fun getMessageMarkersDb(
        messageId: Long,
        names: List<String>,
        offset: Int,
        limit: Int
    ): List<SceytMarker> {
        return markerDao.getMessageMarkers(messageId, names, offset, limit).map { it.toMarker() }
    }

    override suspend fun onMessageStatusChangeEvent(
        data: MessageStatusChangeData
    ) = withContext(Dispatchers.IO) {
        val markerName = when (data.status) {
            Received -> MarkerType.Received.value
            Displayed -> MarkerType.Displayed.value
            else -> return@withContext
        }

        val messageIds = data.marker.messageIds
        if (messageIds.isEmpty()) return@withContext

        val userId = data.from.id
        val createdAt = data.marker.createdAt
        val channelId = data.channel.id

        // 1️⃣ Batch update message status and get updated messages
        val updatedMessages = messageDao.updateMessageStatusWithBefore(
            channelId = channelId,
            status = data.status,
            id = messageIds.maxOf { it }
        )

        // 2️⃣ Update cache
        messagesCache.updateMessagesStatus(
            channelId = channelId,
            status = data.status,
            tIds = updatedMessages.map { it.tid }.toLongArray()
        )

        // 3️⃣ Insert per-user markers
        val userMarkers = messageIds.map { messageId ->
            MarkerEntity(messageId, userId, markerName, createdAt)
        }
        messageDao.insertUserMarkersIfExistMessage(userMarkers)

        // 4️⃣ Update marker totals in DB (batch)
        val messages = messageDao.getMessageEntitiesByIds(messageIds)
        val updatedMessagesWithTotals = messages.map { messageEntity ->
            val currentTotals = messageEntity.markerCount.orEmpty().toMutableList()
            val existingIndex = currentTotals.indexOfFirst { it.name == markerName }

            if (existingIndex >= 0) {
                val existing = currentTotals[existingIndex]
                currentTotals[existingIndex] = MarkerTotal(existing.name, existing.count + 1)
            } else {
                currentTotals.add(MarkerTotal(markerName, 1))
            }

            messageEntity.copy(markerCount = currentTotals)
        }

        messageDao.updateMessagesIgnored(updatedMessagesWithTotals)
    }


    override suspend fun onMessageMarkerEvent(
        data: MessageMarkerEventData
    ) = withContext(Dispatchers.IO) {
        val messageIds = data.marker.messageIds
        val userId = data.user.id
        val markerName = data.marker.name
        val createdAt = data.marker.createdAt
        val channelId = data.channel.id

        if (messageIds.isEmpty()) return@withContext

        // 1️⃣ Insert per-user markers
        val userMarkers = messageIds.map { messageId ->
            MarkerEntity(messageId, userId, markerName, createdAt)
        }
        messageDao.insertUserMarkersIfExistMessage(userMarkers)

        // 2️⃣ Get tIds for cache update
        val tIds = messageDao.getMessageTIdsByIds(ids = messageIds.toLongArray())
        if (tIds.isNotEmpty()) {
            val sceytMarkers = messageIds.map { messageId ->
                SceytMarker(
                    messageId = messageId,
                    userId = userId,
                    user = data.user,
                    name = markerName,
                    createdAt = createdAt
                )
            }

            messagesCache.addMessageMarker(
                channelId = channelId,
                markers = sceytMarkers,
                tIds = tIds.toLongArray()
            )
        }

        // 3️⃣ Update marker totals in DB (batch)
        val messages = messageDao.getMessageEntitiesByIds(ids = messageIds)
        val updatedMessages = messages.map { messageEntity ->
            val currentTotals = messageEntity.markerCount.orEmpty().toMutableList()
            val existingIndex = currentTotals.indexOfFirst { it.name == markerName }

            if (existingIndex >= 0) {
                val existing = currentTotals[existingIndex]
                currentTotals[existingIndex] = MarkerTotal(existing.name, existing.count + 1)
            } else {
                currentTotals.add(MarkerTotal(markerName, 1))
            }

            messageEntity.copy(markerCount = currentTotals)
        }

        messageDao.updateMessagesIgnored(updatedMessages)
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