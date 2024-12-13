package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.config.ChannelListConfig

class ChannelsViewModelFactory(
        private val config: ChannelListConfig = ChannelListConfig.default,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChannelsViewModel(config) as T
    }
}