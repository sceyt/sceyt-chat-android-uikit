package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor

class SelfDestructingMediaPreviewViewModelFactory(
    private val messageInteractor: MessageInteractor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SelfDestructingMediaPreviewViewModel(messageInteractor) as T
    }
}