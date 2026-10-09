package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chat.models.message.PinMessagesRequest
import com.sceyt.chat.models.message.PinnedMessage
import com.sceyt.chat.models.message.PinnedMessagesListQuery
import com.sceyt.chat.models.message.UnpinMessagesRequest
import com.sceyt.chat.sceyt_callbacks.PinnedMessagesCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytPinnedMessage
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.flow

internal class PinRepositoryImpl : PinRepository {

    override suspend fun pinMessages(
        channelId: Long,
        messageIds: List<Long>,
        pinType: PinType,
        pinTill: Long?,
    ): SceytResponse<List<SceytPinnedMessage>> = suspendCancellableCoroutine { continuation ->
        PinMessagesRequest(channelId, messageIds.toLongArray())
            .setPinType(pinType)
            .setPinTill(pinTill ?: 0L)
            .execute(object : PinnedMessagesCallback {
                override fun onResult(pinnedMessages: List<PinnedMessage>?) {
                    val pins = pinnedMessages.orEmpty().mapNotNull { it.toSceytPinnedMessage(channelId) }
                    continuation.safeResume(SceytResponse.Success(pins))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(
                        TAG,
                        "pinMessages error: ${e?.message}, channelId: $channelId," +
                                " messageIds: $messageIds, pinType: $pinType"
                    )
                }
            })
    }

    override suspend fun unpinMessages(
        channelId: Long,
        messageIds: List<Long>,
    ): SceytResponse<List<SceytPinnedMessage>> = suspendCancellableCoroutine { continuation ->
        UnpinMessagesRequest(channelId, messageIds.toLongArray())
            .execute(object : PinnedMessagesCallback {
                override fun onResult(pinnedMessages: List<PinnedMessage>?) {
                    val pins = pinnedMessages.orEmpty().mapNotNull { it.toSceytPinnedMessage(channelId) }
                    continuation.safeResume(SceytResponse.Success(pins))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(
                        TAG,
                        "unpinMessages error: ${e?.message}, channelId: $channelId," +
                                " messageIds: $messageIds"
                    )
                }
            })
    }

    override fun getPinnedMessages(channelId: Long) = flow {
        val query = PinnedMessagesListQuery.Builder(channelId)
            .order(PinnedMessagesListQuery.Order.ASC)
            .pinType(PinnedMessagesListQuery.PinTypeFilter.ALL)
            .limit(SceytChatUIKit.config.queryLimits.pinnedMessagesListQueryLimit)
            .build()
        do {
            val response = query.loadNextAsResponse()
            emit(response)
        } while (response is SceytPagingResponse.Success && response.hasNext)
    }

    private suspend fun PinnedMessagesListQuery.loadNextAsResponse() =
        suspendCancellableCoroutine { continuation ->
            loadNext(object : PinnedMessagesCallback {
                override fun onResult(pinnedMessages: List<PinnedMessage>?) {
                    continuation.safeResume(
                        SceytPagingResponse.Success(
                            data = pinnedMessages.orEmpty().mapNotNull { it.toSceytPinnedMessage(channelId) },
                            hasNext = hasNext(),
                            nextToken = pinnedMessagesPage?.nextToken
                        )
                    )
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytPagingResponse.Error(e))
                    SceytLog.e(TAG, "getPinnedMessages error: ${e?.message}, channelId: $channelId")
                }
            })
        }
}