package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemPopupAddReactionBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemPopupReactionBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class PopupReactionsAdapter(private var data: List<ReactionItem>,
                            private var listener: OnItemClickListener) : RecyclerView.Adapter<BaseViewHolder<ReactionItem>>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactionItem> {
        return if (viewType == ItemType.ADD.ordinal)
            AddViewHolder(SceytItemPopupAddReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            ViewHolder(SceytItemPopupReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReactionItem>, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            data[position] is ReactionItem.Reaction -> ItemType.REACTION.ordinal
            else -> ItemType.ADD.ordinal
        }
    }

    inner class ViewHolder(val binding: SceytItemPopupReactionBinding) : BaseViewHolder<ReactionItem>(binding.root) {
        override fun bind(item: ReactionItem) {
            val reaction = (item as ReactionItem.Reaction).reaction
            binding.emojiView.setSmileText(reaction.key)
            if (item.reaction.containsSelf)
                binding.emojiView.setReactionBackgroundColor(context.getCompatColor(R.color.sceyt_color_button))
            else
                binding.emojiView.setReactionBackgroundColor(Color.TRANSPARENT)

            binding.root.setOnClickListener {
                listener.onReactionClick(item)
            }
        }
    }

    inner class AddViewHolder(val binding: SceytItemPopupAddReactionBinding) : BaseViewHolder<ReactionItem>(binding.root) {
        init {
            binding.setupStyle()
        }

        override fun bind(item: ReactionItem) {
            binding.root.setOnClickListener {
                listener.onAddClick()
            }
        }

        private fun SceytItemPopupAddReactionBinding.setupStyle() {
            addEmoji.setColorFilter(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }
    }

    enum class ItemType {
        REACTION, ADD
    }

    interface OnItemClickListener {
        fun onReactionClick(reaction: ReactionItem.Reaction)
        fun onAddClick()
    }
}
