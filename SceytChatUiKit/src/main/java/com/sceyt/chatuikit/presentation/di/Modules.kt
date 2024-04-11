package com.sceyt.chatuikit.presentation.di

import com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.viewmodels.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val viewModelModule = module {
    viewModel { params ->
        MessageListViewModel(params.get(), params.get(), params.get())
    }
    viewModel { ChannelMembersViewModel(get(), get()) }
    viewModel { ReactionsInfoViewModel() }
}

