package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle

open class PollOptionViewHolderFactory(
        context: Context,
        private val pollStyle: PollStyle,
        private val isClosedProvider: () -> Boolean,
        private val isAnonymousProvider: () -> Boolean,
        private val totalVotesProvider: () -> Int,
        private val bubbleBackgroundStyleProvider: (() -> BackgroundStyle),
        private val onOptionClick: ((PollOptionUiModel) -> Unit)? = null,
) {

    private val layoutInflater = LayoutInflater.from(context)

    open fun createViewHolder(parent: ViewGroup): PollOptionViewHolder {
        val viewHolder = PollOptionViewHolder(
            binding = SceytItemPollOptionBinding.inflate(layoutInflater, parent, false),
            pollStyle = pollStyle,
            isClosedProvider = isClosedProvider,
            isAnonymousProvider = isAnonymousProvider,
            totalVotesProvider = totalVotesProvider,
            bubbleBackgroundStyleProvider = bubbleBackgroundStyleProvider,
            onOptionClick = onOptionClick
        )
        return viewHolder
    }
}

