package com.sceyt.chatuikit.presentation.components.select_users.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
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
            else SceytChatUIKit.chatUIFacade.userInteractor.loadUsers(createUserListQueryBuilder(query))

            var empty = false
            if (response is SceytResponse.Success) {
                empty = response.data.isNullOrEmpty()
                hasNext = response.data?.size == SceytChatUIKit.config.queryLimits.userListQueryLimit
                if (isLoadMore)
                    _loadMoreChannelsLiveData.postValue(mapToUserItems(response.data, hasNext))
                else _usersLiveData.postValue(mapToUserItems(response.data, hasNext))
            }
            notifyPageStateWithResponse(response, isLoadMore, empty, searchQuery = query)
            loadingNextItems.set(false)
        }
    }

    private fun mapToUserItems(list: List<SceytUser>?, hasNext: Boolean): List<UserItem> {
        val memberItems: MutableList<UserItem> = (list
                ?: return arrayListOf()).map { UserItem.User(it) }.toMutableList()
        if (hasNext)
            memberItems.add(UserItem.LoadingMore)
        return memberItems
    }

    fun findOrCreatePendingDirectChannel(user: SceytUser) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = SceytChatUIKit.chatUIFacade.channelInteractor.findOrCreatePendingChannelByMembers(
                CreateChannelData(
                    type = ChannelTypeEnum.Direct.value,
                    members = listOf(SceytMember(user, RoleTypeEnum.Owner.value))
                )
            )
            notifyResponseAndPageState(_createChannelLiveData, response)
        }
    }

    private fun createUserListQueryBuilder(searchQuery: String) = UserListQuery.Builder()
        .order(UserListQuery.UserListQueryOrderKeyType.UserListQueryOrderKeyFirstName)
        .filter(UserListQuery.UserListFilterType.UserListFilterTypeAll)
        .limit(SceytChatUIKit.config.queryLimits.userListQueryLimit)
        .query(searchQuery)
        .build()
}