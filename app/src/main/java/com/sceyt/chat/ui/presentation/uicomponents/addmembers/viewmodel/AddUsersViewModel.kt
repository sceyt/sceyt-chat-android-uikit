package com.sceyt.chat.ui.presentation.uicomponents.addmembers.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.UsersRepository
import com.sceyt.chat.ui.data.UsersRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.addmembers.adapters.UserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddUsersViewModel : BaseViewModel() {
    private val usersRepository: UsersRepository = UsersRepositoryImpl()

    private val _channelsLiveData = MutableLiveData<List<UserItem>>()
    val channelsLiveData: LiveData<List<UserItem>> = _channelsLiveData

    private val _loadMoreChannelsLiveData = MutableLiveData<List<UserItem>>()
    val loadMoreChannelsLiveData: LiveData<List<UserItem>> = _loadMoreChannelsLiveData

    fun loadMessages(offset: Int, query: String = "") {
        loadingItems = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = usersRepository.loadUsers(offset, query)
            var empty = false
            if (response is SceytResponse.Success) {
                empty = response.data.isNullOrEmpty()
                hasNext = response.data?.size == 20
                if (offset == 0)
                    _channelsLiveData.postValue(mapToUserItems(response.data, hasNext))
                else _loadMoreChannelsLiveData.postValue(mapToUserItems(response.data, hasNext))
            }
            notifyPageStateWithResponse(response, offset > 0, empty, searchQuery = query)
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
}