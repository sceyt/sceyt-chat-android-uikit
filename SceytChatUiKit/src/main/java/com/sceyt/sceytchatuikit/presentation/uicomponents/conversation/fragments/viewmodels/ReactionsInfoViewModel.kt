package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogic
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters.ReactedUserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ReactionsInfoViewModel : BaseViewModel(), SceytKoinComponent {
    private val messageReactionsMiddleWare by inject<PersistenceReactionsLogic>()

    private val _loadReactIonsLiveData = MutableLiveData<PaginationResponse<Reaction>>()
    val loadReactIonsLiveData: LiveData<PaginationResponse<Reaction>> = _loadReactIonsLiveData


    fun getReactions(messageId: Long, offset: Int, key: String, loadKey: LoadKeyData? = null, ignoreDb: Boolean = false) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

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

    suspend fun initDbResponse(response: PaginationResponse.DBResponse<Reaction>, cashedList: List<ReactedUserItem>?): List<ReactedUserItem> {
        return withContext(Dispatchers.IO) {
            val currentList = arrayListOf<Reaction>()
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

    suspend fun initServerResponse(response: PaginationResponse.ServerResponse<Reaction>): List<ReactedUserItem> {
        return withContext(Dispatchers.IO) {
            val data = initResponseData(response.cacheData, response.hasNext)
            withContext(Dispatchers.Main) {
                data
            }
        }
    }

    private fun initResponseData(reactions: List<Reaction>, hasNext: Boolean): List<ReactedUserItem> {
        var list: List<ReactedUserItem> = reactions.map { ReactedUserItem.Item(it) }
        if (hasNext)
            list = list.toArrayList().apply { add(ReactedUserItem.LoadingMore) }

        return list
    }
}