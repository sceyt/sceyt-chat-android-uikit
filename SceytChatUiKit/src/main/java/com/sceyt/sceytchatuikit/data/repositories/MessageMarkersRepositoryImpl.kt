package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.MessageMarkerListQuery
import com.sceyt.chat.sceyt_callbacks.MessageMarkersCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import kotlinx.coroutines.suspendCancellableCoroutine

class MessageMarkersRepositoryImpl : MessageMarkersRepository {

    override suspend fun getMessageMarkers(messageId: Long, name: String,
                                           offset: Int, limit: Int): SceytResponse<List<Marker>> = suspendCancellableCoroutine { continuation ->
        getQuery(messageId, name, limit, offset).loadNext(object : MessageMarkersCallback {
            override fun onResult(markers: MutableList<Marker>?) {
                val result: MutableList<Marker> = markers?.toMutableList() ?: mutableListOf()
                continuation.safeResume(SceytResponse.Success(result))
            }

            override fun onError(e: SceytException?) {
                continuation.safeResume(SceytResponse.Error(e))
                SceytLog.e(TAG, "getMessageMarkers error: ${e?.message}")
            }
        })
    }

    private fun getQuery(messageId: Long,
                         name: String,
                         limit: Int,
                         offset: Int) = MessageMarkerListQuery.Builder().apply {
        setMessageId(messageId)
        setName(name)
        limit(limit)
        offset(offset)
    }.build()
}