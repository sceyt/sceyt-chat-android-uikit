package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onSuccess
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.presentation.components.channel.input.data.SearchResult
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns in-conversation message search that used to live inside [MessageListViewModel]:
 * running a query, paging the next server batch, and stepping prev/next through results.
 *
 * [isPreparingToScrollToMessage] is shared with pagination, so it is injected rather than owned.
 */
internal class MessageSearchController(
    private val scope: CoroutineScope,
    private val messageInteractor: MessageInteractor,
    private val conversationId: () -> Long,
    private val replyInThread: Boolean,
    private val isPreparingToScrollToMessage: AtomicBoolean,
    private val messageListQueryLimit: () -> Int,
    private val onScrollToSearchMessage: (SceytMessage) -> Unit,
) {
    private val _searchResult = MutableLiveData<SearchResult>()
    val searchResult: LiveData<SearchResult> = _searchResult.asLiveData()

    private var searchJob: Job? = null
    private val isLoadingNextFromServer = AtomicBoolean(false)

    fun search(query: String) {
        if (_searchResult.value?.searchQuery == query)
            return

        _searchResult.postValue(SearchResult(searchQuery = query, isLoading = true))

        searchJob?.cancel()
        searchJob = scope.launch {
            messageInteractor.searchMessages(
                conversationId = conversationId(),
                replyInThread = replyInThread,
                query = query
            ).onSuccess { response ->
                val messages = response.data.sortedBy { it.id }
                _searchResult.postValue(
                    SearchResult(
                        searchQuery = query,
                        currentIndex = 0,
                        messages = messages,
                        hasNext = response.hasNext,
                        isLoading = false
                    )
                )
                onScrollToSearchMessage(messages.firstOrNull() ?: return@launch)
            }
        }
    }

    fun scrollToSearchMessage(isPrev: Boolean) {
        if (isPreparingToScrollToMessage.get()) return
        val current = _searchResult.value ?: return
        val messages = current.messages
        val nextIndex = if (isPrev) {
            current.currentIndex + 1
        } else current.currentIndex - 1
        if (nextIndex < 0 || nextIndex >= messages.size)
            return

        isPreparingToScrollToMessage.set(true)
        _searchResult.postValue(current.copy(currentIndex = nextIndex))
        onScrollToSearchMessage(messages[nextIndex])

        val queryLimit = messageListQueryLimit()
        if (current.hasNext && messages.size - nextIndex < queryLimit / 2) {
            loadNext()
        }
    }

    private fun loadNext() {
        if (isLoadingNextFromServer.getAndSet(true)) return
        scope.launch {
            messageInteractor.loadNextSearchMessages().onSuccess {
                val messages = it.data
                val oldValue = _searchResult.value ?: return@launch
                val loadedMessages = ArrayList(oldValue.messages)
                val newMessages: List<SceytMessage> = loadedMessages.plus(messages.reversed())
                _searchResult.postValue(oldValue.copy(messages = newMessages, hasNext = it.hasNext))
            }
            isLoadingNextFromServer.set(false)
        }
    }
}