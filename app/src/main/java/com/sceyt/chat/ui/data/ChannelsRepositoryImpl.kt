package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ChannelsRepositoryImpl {

    private val query =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage)
                .limit(10)
                .build()

    fun getChannels(offset: Int = 0): Flow<SceytResponse<List<Channel>>> {
        return flow {
            if (offset == 0)
                emit(SceytResponse.Loading(true))

            try {
                val response = getChannelsCoroutine(offset)
                emit(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emit(SceytResponse.Error("Couldn't load data"))
            } finally {
                emit(SceytResponse.Loading(false))
            }
        }
    }

    private suspend fun getChannelsCoroutine(offset: Int): SceytResponse<List<Channel>> {
        return suspendCancellableCoroutine { continuation ->
             query.offset = offset
             query.loadNext(object : ChannelsCallback {
                 override fun onResult(channels: MutableList<Channel>?) {
                     if (channels.isNullOrEmpty())
                         continuation.resume(SceytResponse.Success(emptyList()))
                     else {
                         continuation.resume(SceytResponse.Success(channels))
                     }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}