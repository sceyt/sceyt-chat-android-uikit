package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventsObserverService
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ChannelsRepositoryImpl : ChannelsRepository {
    //todo need to add DI
    //private val channelEventsService = ChannelEventsObserverService()

    override val onMessageFlow = ChannelEventsObserverService.onMessageFlow
    override val onMessageStatusFlow = ChannelEventsObserverService.onMessageStatusFlow

    override val onMessageEditedOrDeleteFlow = ChannelEventsObserverService.onMessageEditedOrDeletedFlow
        .filterNotNull()

    override val onChannelEvenFlow = ChannelEventsObserverService.onChannelEventFlow

    private val query =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(getOrder())
                .limit(SceytUIKitConfig.CHANNELS_LOAD_SIZE)
                .build()

    private val querySearch =
            ChannelListQuery.Builder()
                .type(ChannelListQuery.ChannelQueryType.ListQueryChannelAll)
                .order(getOrder())
                .filterKey(ChannelListQuery.ChannelListFilterKey.ListQueryChannelFilterKeySubject)
                .queryType(ChannelListQuery.ChannelListFilterQueryType.ListQueryFilterContains)

    private fun getOrder(): ChannelListQuery.ChannelListOrder {
        return if (SceytUIKitConfig.sortChannelsBy == SceytUIKitConfig.ChannelSortType.ByLastMsg)
            ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage
        else ChannelListQuery.ChannelListOrder.ListQueryChannelOrderCreatedAt
    }

    override suspend fun getChannels(offset: Int): SceytResponse<List<SceytChannel>> {
        return getChannelsCoroutine(offset)
    }

    override suspend fun searchChannels(offset: Int, query: String): SceytResponse<List<SceytChannel>> {
        return getSearchChannelsCoroutine(offset, query)
    }

    private suspend fun getChannelsCoroutine(offset: Int): SceytResponse<List<SceytChannel>> {
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

    private suspend fun getSearchChannelsCoroutine(offset: Int, searchQuery: String): SceytResponse<List<SceytChannel>> {
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

    override suspend fun leaveChannel(channel: GroupChannel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.leave(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun clearHistory(channel: Channel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.clearHistory(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun blockChannel(channel: GroupChannel): SceytResponse<Long> {
        return suspendCancellableCoroutine { continuation ->
            channel.block(object : ChannelsCallback {
                override fun onResult(channels: MutableList<Channel>?) {
                    continuation.resume(SceytResponse.Success(channel.id))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}