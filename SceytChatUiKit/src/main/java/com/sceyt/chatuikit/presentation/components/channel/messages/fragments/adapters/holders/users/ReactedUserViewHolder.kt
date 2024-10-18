package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.holders.users

import com.sceyt.chatuikit.databinding.SceytItemReactedUserBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactedUserItem
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters.ReactedUserViewHolderFactory
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserItemStyle

class ReactedUserViewHolder(
        private val binding: SceytItemReactedUserBinding,
        private val itemStyle: ReactedUserItemStyle,
        private val clickListener: ReactedUserViewHolderFactory.OnItemClickListener
) : BaseViewHolder<ReactedUserItem>(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: ReactedUserItem) {
        with(binding) {
            val user = (item as ReactedUserItem.Item).reaction.user ?: return

            itemStyle.avatarRenderer.render(context, user, itemStyle.avatarStyle, avatar)
            userName.text = itemStyle.titleFormatter.format(context, user)
            reaction.setSmileText(itemStyle.subtitleFormatter.format(context, item.reaction.key))

            root.setOnClickListener {
                clickListener.onItemClick(item)
            }
        }
    }

    private fun SceytItemReactedUserBinding.applyStyle() {
        itemStyle.titleTextStyle.apply(userName)
        avatar.applyStyle(itemStyle.avatarStyle)
    }
}