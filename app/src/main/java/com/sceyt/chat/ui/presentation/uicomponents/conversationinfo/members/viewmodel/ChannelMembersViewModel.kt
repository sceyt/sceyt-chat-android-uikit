package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelMembersViewModel : BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()
    var hasNext: Boolean = false
    var loadingMembers: Boolean = false

    private val _membersLiveData = MutableLiveData<List<MemberItem>>()
    val membersLiveData: LiveData<List<MemberItem>> = _membersLiveData

    private val _loadMoreMembersLiveData = MutableLiveData<List<MemberItem>>()
    val loadMoreMembersLiveData: LiveData<List<MemberItem>> = _loadMoreMembersLiveData

    private val _changeOwnerLiveData = MutableLiveData<String>()
    val changeOwnerLiveData: LiveData<String> = _changeOwnerLiveData


    fun getChannelMembers(channelId: Long, loadingMore: Boolean) {
        loadingMembers = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.loadChannelMembers(channelId)
            if (response is SceytResponse.Success) {
                hasNext = response.data?.size == SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE

                if (loadingMore)
                    _loadMoreMembersLiveData.postValue(mapToMemberItem(response.data, hasNext))
                else
                    _membersLiveData.postValue(mapToMemberItem(response.data, hasNext))
            }
            loadingMembers = false
            notifyPageStateWithResponse(response, loadingMore)
        }
    }

    private fun mapToMemberItem(list: List<SceytMember>?, hasNest: Boolean): MutableList<MemberItem> {
        val memberItems: MutableList<MemberItem> = (list
                ?: return arrayListOf()).map { MemberItem.Member(it) }.toMutableList()
        if (hasNest)
            memberItems.add(MemberItem.LoadingMore)
        return memberItems
    }

    fun changeOwner(channel: SceytChannel, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.changeChannelOwner(channel.toGroupChannel(), id)
            notifyResponseAndPageState(_changeOwnerLiveData, response)
        }
    }
}