package com.sceyt.chatuikit.data.repositories

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chat.sceyt_callbacks.ChannelsCallback
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SearchChannelParams
import com.sceyt.chatuikit.data.models.SceytResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ChannelsRepositoryImplSearchTest {
    private val config = ChannelListConfig(
        types = listOf("broadcast"),
        order = ChannelListQuery.ChannelListOrder.ListQueryChannelOrderLastMessage,
        queryLimit = 15,
        queryParam = ChannelQueryParam(1, 10, 1, true),
    )

    @Test
    fun `search does not replace the channel list pagination query`() = runTest {
        val listQuery = emptyQuery()
        val searchQuery = emptyQuery()

        mockQueries(listQuery, searchQuery).use {
            val repository = ChannelsRepositoryImpl()
            repository.getChannels("", config, SearchChannelParams.default)

            val response = repository.searchChannels("hello", config, SearchChannelParams.default)
            repository.loadMoreChannels("", config, SearchChannelParams.default)

            assertThat(response).isInstanceOf(SceytResponse.Success::class.java)
            verify(listQuery, times(2)).loadNext(any())
            verify(searchQuery).loadNext(any())
        }
    }

    @Test
    fun `search before opening the channel list does not initialize its pagination query`() = runTest {
        val searchQuery = emptyQuery()
        val listQuery = emptyQuery()

        mockQueries(searchQuery, listQuery).use {
            val repository = ChannelsRepositoryImpl()
            repository.searchChannels("hello", config, SearchChannelParams.default)
            repository.loadMoreChannels("", config, SearchChannelParams.default)

            verify(searchQuery).loadNext(any())
            verify(listQuery).loadNext(any())
        }
    }

    private fun emptyQuery(): ChannelListQuery = mock<ChannelListQuery>().also { query ->
        doAnswer {
            it.getArgument<ChannelsCallback>(0).onResult(mutableListOf())
            null
        }.whenever(query).loadNext(any())
    }

    private fun mockQueries(vararg queries: ChannelListQuery): MockedConstruction<ChannelListQuery.Builder> {
        val queue = ArrayDeque(queries.toList())
        return Mockito.mockConstruction(
            ChannelListQuery.Builder::class.java,
            Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF),
        ) { builder, _ ->
            whenever(builder.build()).thenReturn(queue.removeFirst())
        }
    }
}
