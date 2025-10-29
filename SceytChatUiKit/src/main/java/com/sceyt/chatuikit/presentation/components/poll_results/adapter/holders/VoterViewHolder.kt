package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemVoterBinding
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.poll_results.VoterItemStyle

class VoterViewHolder(
        private val binding: SceytItemVoterBinding,
        private val style: VoterItemStyle,
        private val clickListeners: VoterClickListeners.ClickListeners
) : BaseViewHolder<VoterItem>(binding.root) {

    private lateinit var voterItem: VoterItem.Voter

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            clickListeners.onVoterClick(it, voterItem)
        }
    }

    override fun bind(item: VoterItem) {
        voterItem = (item as? VoterItem.Voter) ?: return
        val vote = voterItem.vote
        val user = vote.user ?: return

        with(binding) {
            style.avatarRenderer.render(root.context, user, style.avatarStyle, avatar)
            tvUserName.text = style.userNameFormatter.format(root.context, user)
            tvVoteTime.text = SceytChatUIKit.formatters.pollVoteTimeDateFormatter.format(
                context = root.context,
                from = vote.createdAt
            )
        }
    }

    private fun SceytItemVoterBinding.applyStyle() {
        style.userNameTextStyle.apply(tvUserName)
        style.voteTimeTextStyle.apply(tvVoteTime)
        style.avatarStyle.apply(avatar)
    }
}