package com.sceyt.chat.demo.presentation.addmembers.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.USERS_LOAD_SIZE
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
                SceytKitClient.getUserMiddleWare().loadMoreUsers()
            else SceytKitClient.getUserMiddleWare().loadUsers(query)

            var empty = false
            if (response is SceytResponse.Success) {
                empty = response.data.isNullOrEmpty()
                hasNext = response.data?.size == USERS_LOAD_SIZE
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
            val response = SceytKitClient.getChannelsMiddleWare().findOrCreateDirectChannel(user)
            notifyResponseAndPageState(_createChannelLiveData, response)
        }
    }
}