package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.toSceytUiChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ChannelsRepositoryImpl {

    private val query =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage)
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

    fun getChannels(offset: Int): Flow<SceytResponse<List<SceytUiChannel>>> {
        return flow {
            emit(SceytResponse.Loading(true))

            val response = getChannelsCoroutine(offset)
            emit(response)
        }
    }

    private suspend fun getChannelsCoroutine(offset: Int): SceytResponse<List<SceytUiChannel>> {
        return suspendCancellableCoroutine { continuation ->
            query.offset = offset
            query.loadNext(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    if (channels.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.resume(SceytResponse.Success(channels.map { it.toSceytUiChannel() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}