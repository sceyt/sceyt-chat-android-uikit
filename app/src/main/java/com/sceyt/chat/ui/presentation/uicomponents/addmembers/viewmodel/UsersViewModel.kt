package com.sceyt.chat.ui.presentation.uicomponents.addmembers.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.UsersRepository
import com.sceyt.chat.ui.data.UsersRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig.USERS_LOAD_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsersViewModel : BaseViewModel() {
    private val usersRepository: UsersRepository = UsersRepositoryImpl()

    private val _channelsLiveData = MutableLiveData<List<UserItem>>()
    val channelsLiveData: LiveData<List<UserItem>> = _channelsLiveData

    private val _loadMoreChannelsLiveData = MutableLiveData<List<UserItem>>()
    val loadMoreChannelsLiveData: LiveData<List<UserItem>> = _loadMoreChannelsLiveData

    private val _createChannelLiveData = MutableLiveData<SceytChannel>()
    val createChannelLiveData: LiveData<SceytChannel> = _createChannelLiveData

    fun loadUsers(query: String = "", isLoadMore: Boolean) {
        loadingItems = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = if (isLoadMore)
                usersRepository.loadMoreUsers()
            else usersRepository.loadUsers(query)

            var empty = false
            if (response is SceytResponse.Success) {
                empty = response.data.isNullOrEmpty()
                hasNext = response.data?.size == USERS_LOAD_SIZE
                if (isLoadMore)
                    _loadMoreChannelsLiveData.postValue(mapToUserItems(response.data, hasNext))
                else _channelsLiveData.postValue(mapToUserItems(response.data, hasNext))
            }
            notifyPageStateWithResponse(response, isLoadMore, empty, searchQuery = query)
            loadingItems = false
        }
    }

    private fun mapToUserItems(list: List<User>?, hasNext: Boolean): List<UserItem> {
        val memberItems: MutableList<UserItem> = (list
                ?: return arrayListOf()).map { UserItem.User(it) }.toMutableList()
        if (hasNext)
            memberItems.add(UserItem.LoadingMore)
        return memberItems
    }

    fun createDirectChannel(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = usersRepository.createDirectChannel(user)
            notifyResponseAndPageState(_createChannelLiveData, response)
        }
    }
}