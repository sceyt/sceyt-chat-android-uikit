package com.sceyt.chatuikit.presentation.uicomponents.addmembers.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsersViewModel : BaseViewModel() {
    private val _usersLiveData = MutableLiveData<List<UserItem>>()
    val usersLiveData: LiveData<List<UserItem>> = _usersLiveData

    private val _loadMoreChannelsLiveData = MutableLiveData<List<UserItem>>()
    val loadMoreChannelsLiveData: LiveData<List<UserItem>> = _loadMoreChannelsLiveData

    private val _createChannelLiveData = MutableLiveData<SceytChannel>()
    val createChannelLiveData: LiveData<SceytChannel> = _createChannelLiveData

    fun loadUsers(query: String = "", isLoadMore: Boolean) {
        loadingNextItems.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (isLoadMore)
                SceytChatUIKit.chatUIFacade.userInteractor.loadMoreUsers()
            else SceytChatUIKit.chatUIFacade.userInteractor.loadUsers(query)

            var empty = false
            if (response is SceytResponse.Success) {
                empty = response.data.isNullOrEmpty()
                hasNext = response.data?.size == SceytChatUIKit.config.usersLoadSize
                if (isLoadMore)
                    _loadMoreChannelsLiveData.postValue(mapToUserItems(response.data, hasNext))
                else _usersLiveData.postValue(mapToUserItems(response.data, hasNext))
            }
            notifyPageStateWithResponse(response, isLoadMore, empty, searchQuery = query)
            loadingNextItems.set(false)
        }
    }

    private fun mapToUserItems(list: List<User>?, hasNext: Boolean): List<UserItem> {
        val memberItems: MutableList<UserItem> = (list
                ?: return arrayListOf()).map { UserItem.User(it) }.toMutableList()
        if (hasNext)
            memberItems.add(UserItem.LoadingMore)
        return memberItems
    }

    fun findOrCreateDirectChannel(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = SceytChatUIKit.chatUIFacade.channelInteractor.findOrCreateDirectChannel(user)
            notifyResponseAndPageState(_createChannelLiveData, response)
        }
    }
}