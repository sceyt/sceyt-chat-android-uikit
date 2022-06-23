package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel

import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChannelMembersViewModel :  BaseViewModel() {
    // Todo di
    private val repo: ChannelsRepository = ChannelsRepositoryImpl()

    private val _membersFlow = MutableStateFlow<SceytResponse<List<SceytMember>>>(SceytResponse.Success(null))
    val membersFlow: StateFlow<SceytResponse<List<SceytMember>>> = _membersFlow


    fun getChannelMembers() {

    }
}