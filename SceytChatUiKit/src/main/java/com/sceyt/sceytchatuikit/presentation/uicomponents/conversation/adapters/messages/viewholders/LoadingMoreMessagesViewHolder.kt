package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder

class LoadingMoreMessagesViewHolder(binding: SceytItemLoadingMoreBinding, displayedListener: ((MessageListItem) -> Unit)?) :
        BaseMsgViewHolder(binding.root, displayedListener = displayedListener) {

    override val enableReply = false
}