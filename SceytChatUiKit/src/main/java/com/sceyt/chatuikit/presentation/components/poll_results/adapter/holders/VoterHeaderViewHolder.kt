package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import com.sceyt.chatuikit.databinding.SceytItemVoterHeaderBinding
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.poll_results.VoterItemStyle

class VoterHeaderViewHolder(
        private val binding: SceytItemVoterHeaderBinding,
        private val style: VoterItemStyle
) : BaseViewHolder<VoterItem>(binding.root) {

    override fun bind(item: VoterItem) {
        val headerItem = (item as? VoterItem.HeaderItem) ?: return
        style.votersHeaderTextStyle.apply(binding.tvVoteCount)
        binding.tvVoteCount.text = style.voteCountFormatter.format(context, headerItem.voteCount)
    }
}