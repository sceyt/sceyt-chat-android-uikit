package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactedUserItem
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserListStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReactionsInfoViewModel(
        private val messageReactionsMiddleWare: PersistenceReactionsLogic,
        private val messageId: Long,
        private val key: String,
) : BaseViewModel() {
    private val _loadReactIonsLiveData = MutableLiveData<PaginationResponse<SceytReaction>>()
    val loadReactionsLiveData: LiveData<PaginationResponse<SceytReaction>> = _loadReactIonsLiveData

    lateinit var style: ReactedUserListStyle

    fun getReactions(offset: Int, loadKey: LoadKeyData? = null, ignoreDb: Boolean = false) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext, ignoreDatabase = ignoreDb)

        viewModelScope.launch(Dispatchers.IO) {
            messageReactionsMiddleWare.loadReactions(messageId, offset, key, loadKey, ignoreDb)
                .onEach {
                    withContext(Dispatchers.Main) {
                        _loadReactIonsLiveData.value = it
                    }
                    pagingResponseReceived(it)
                }.launchIn(viewModelScope)
        }
    }

    suspend fun initDbResponse(
            response: PaginationResponse.DBResponse<SceytReaction>,
            cashedList: List<ReactedUserItem>?
    ): List<ReactedUserItem> {
        return withContext(Dispatchers.IO) {
            val currentList = arrayListOf<SceytReaction>()
            if (response.offset > 0)
                cashedList?.forEach { item ->
                    if (item is ReactedUserItem.Item)
                        currentList.add(item.reaction)
                }
            currentList.addAll(response.data)

            val data = initResponseData(currentList, response.hasNext)

            withContext(Dispatchers.Main) {
                data
            }
        }
    }

    suspend fun initServerResponse(
            response: PaginationResponse.ServerResponse<SceytReaction>
    ): List<ReactedUserItem> {
        return withContext(Dispatchers.IO) {
            val data = initResponseData(response.cacheData, response.hasNext)
            withContext(Dispatchers.Main) {
                data
            }
        }
    }

    private fun initResponseData(reactions: List<SceytReaction>, hasNext: Boolean): List<ReactedUserItem> {
        var list: List<ReactedUserItem> = reactions.map { ReactedUserItem.Item(it) }
        if (hasNext)
            list = list.toArrayList().apply { add(ReactedUserItem.LoadingMore) }

        return list
    }
}