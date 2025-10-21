package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemReactedUserBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.users.ReactedUserViewHolder
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserItemStyle

open class ReactedUserViewHolderFactory(
        private val style: ReactedUserItemStyle,
) {
    private var clickListener: OnItemClickListener = OnItemClickListener {}

    open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactedUserItem> {
        return when (viewType) {
            ViewType.Item.ordinal -> createReactedUserViewHolder(parent)
            ViewType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw Exception("Unsupported view type")
        }
    }

    private fun createReactedUserViewHolder(parent: ViewGroup): ReactedUserViewHolder {
        val binding = SceytItemReactedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReactedUserViewHolder(binding, style, clickListener)
    }

    private fun createLoadingMoreViewHolder(parent: ViewGroup): BaseViewHolder<ReactedUserItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return object : BaseViewHolder<ReactedUserItem>(binding.root) {
            override fun bind(item: ReactedUserItem) {
                binding.adapterListLoadingProgressBar.setProgressColorRes(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
            }
        }
    }

    open fun getItemViewType(item: ReactedUserItem): Int {
        return when (item) {
            is ReactedUserItem.Item -> ViewType.Item.ordinal
            is ReactedUserItem.LoadingMore -> ViewType.Loading.ordinal
        }
    }

    enum class ViewType {
        Item, Loading
    }

    fun setClickListener(listener: OnItemClickListener) {
        clickListener = listener
    }

    fun interface OnItemClickListener {
        fun onItemClick(item: ReactedUserItem.Item)
    }
}