package com.sceyt.chatuikit.presentation.components.shareable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.config.ChannelListConfig

class ShareableViewModelFactory (
        private val config: ChannelListConfig = ChannelListConfig.default,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ShareableViewModel(config) as T
    }
}