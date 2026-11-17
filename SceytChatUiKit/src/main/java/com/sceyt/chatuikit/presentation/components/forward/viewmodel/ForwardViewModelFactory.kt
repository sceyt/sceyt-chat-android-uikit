package com.sceyt.chatuikit.presentation.components.forward.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chatuikit.data.models.messages.SceytMessage

/**
 * Creates a [ForwardViewModel] for the given [messages] to forward.
 */
class ForwardViewModelFactory(
    private val messages: List<SceytMessage>,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ForwardViewModel(messages) as T
    }
}
