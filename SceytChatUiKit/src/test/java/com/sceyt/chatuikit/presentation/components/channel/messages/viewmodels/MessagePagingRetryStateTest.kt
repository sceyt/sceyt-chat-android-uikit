package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import org.junit.Test

class MessagePagingRetryStateTest {

    @Test
    fun `load prev server error enables prev retry until successful prev response`() {
        val state = MessagePagingRetryState()

        state.onServerResponse(serverResponse(LoadPrev, SceytResponse.Error()))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isTrue()
        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isFalse()

        state.onServerResponse(serverResponse(LoadPrev, SceytResponse.Success(emptyList())))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isFalse()
    }

    @Test
    fun `load next server error enables next retry until successful next response`() {
        val state = MessagePagingRetryState()

        state.onServerResponse(serverResponse(LoadNext, SceytResponse.Error()))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isFalse()
        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isTrue()

        state.onServerResponse(serverResponse(LoadNext, SceytResponse.Success(emptyList())))

        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isFalse()
    }

    @Test
    fun `load near success clears stale edge retries`() {
        val state = MessagePagingRetryState()

        state.onServerResponse(serverResponse(LoadPrev, SceytResponse.Error()))
        state.onServerResponse(serverResponse(LoadNext, SceytResponse.Error()))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isTrue()
        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isTrue()

        state.onServerResponse(serverResponse(LoadNear, SceytResponse.Success(emptyList())))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isFalse()
        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isFalse()
    }

    @Test
    fun `load near server error enables edge retries`() {
        val state = MessagePagingRetryState()

        state.onServerResponse(serverResponse(LoadNear, SceytResponse.Error()))

        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = false)).isTrue()
        assertThat(state.canRetryNext(loadingFromDb = false, loadingFromServer = false)).isTrue()
    }

    @Test
    fun `active loading blocks retry`() {
        val state = MessagePagingRetryState()
        state.onServerResponse(serverResponse(LoadPrev, SceytResponse.Error()))

        assertThat(state.canRetryPrev(loadingFromDb = true, loadingFromServer = false)).isFalse()
        assertThat(state.canRetryPrev(loadingFromDb = false, loadingFromServer = true)).isFalse()
    }

    private fun serverResponse(
        loadType: PaginationResponse.LoadType,
        response: SceytResponse<List<Nothing>>,
    ) = PaginationResponse.ServerResponse(
        data = response,
        cacheData = emptyList(),
        loadKey = null,
        offset = 0,
        hasDiff = false,
        hasNext = false,
        hasPrev = false,
        loadType = loadType,
        ignoredDb = false,
        dbResultWasEmpty = false
    )
}
