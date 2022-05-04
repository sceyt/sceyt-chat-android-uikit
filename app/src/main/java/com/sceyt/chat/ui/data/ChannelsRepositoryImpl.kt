package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ChannelsRepositoryImpl {

    private val query =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage)
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

    private val querySearch =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage)
                .filterKey(ChannelListQuery.ChannelListFilterKey.ListQueryChannelFilterKeySubject)
                .queryType(ChannelListQuery.ChannelListFilterQueryType.ListQueryFilterContains)

    suspend fun getChannels(offset: Int): SceytResponse<List<SceytUiChannel>> {
        return getChannelsCoroutine(offset)
    }

    suspend fun searchChannels(offset: Int, query: String): SceytResponse<List<SceytUiChannel>> {
        return getSearchChannelsCoroutine(offset, query)
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

    private suspend fun getSearchChannelsCoroutine(offset: Int, searchQuery: String): SceytResponse<List<SceytUiChannel>> {
        return suspendCancellableCoroutine { continuation ->
            val channelListQuery = querySearch
                .query(searchQuery)
                .offset(offset)
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

            channelListQuery.loadNext(object : ChannelsCallback {
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