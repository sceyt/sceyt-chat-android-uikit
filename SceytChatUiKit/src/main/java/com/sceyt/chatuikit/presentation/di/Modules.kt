package com.sceyt.chatuikit.presentation.di

import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.viewmodel.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.members.viewmodel.ChannelMembersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val viewModelModule = module {
    viewModel { params ->
        MessageListViewModel(params.get(), params.get(), params.get())
    }
    viewModel { ChannelMembersViewModel(get(), get()) }
    viewModel { ReactionsInfoViewModel() }
}

