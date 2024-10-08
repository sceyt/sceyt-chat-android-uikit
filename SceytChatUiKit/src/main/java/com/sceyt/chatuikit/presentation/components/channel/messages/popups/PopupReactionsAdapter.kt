package com.sceyt.chatuikit.presentation.components.channel.messages.popups

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemPopupAddReactionBinding
import com.sceyt.chatuikit.databinding.SceytItemPopupReactionBinding
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.messages_list.ReactionPickerStyle

class PopupReactionsAdapter(
        private val data: List<ReactionItem>,
        private val style: ReactionPickerStyle,
        private val listener: OnItemClickListener
) : RecyclerView.Adapter<BaseViewHolder<ReactionItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactionItem> {
        return if (viewType == ItemType.ADD.ordinal) {
            val binding = SceytItemPopupAddReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AddViewHolder(binding)
        } else {
            val binding = SceytItemPopupReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ViewHolder(binding)
        }
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

    inner class ViewHolder(
            val binding: SceytItemPopupReactionBinding
    ) : BaseViewHolder<ReactionItem>(binding.root) {
        override fun bind(item: ReactionItem) {
            val reaction = (item as ReactionItem.Reaction).reaction
            binding.emojiView.setSmileText(reaction.key)
            if (item.reaction.containsSelf)
                binding.emojiView.setReactionBackgroundColor(style.selectedBackgroundColor)
            else
                binding.emojiView.setReactionBackgroundColor(Color.TRANSPARENT)

            binding.root.setOnClickListener {
                listener.onReactionClick(item)
            }
        }
    }

    inner class AddViewHolder(
            val binding: SceytItemPopupAddReactionBinding
    ) : BaseViewHolder<ReactionItem>(binding.root) {
        init {
            binding.applyStyle()
        }

        override fun bind(item: ReactionItem) {
            binding.root.setOnClickListener {
                listener.onAddClick()
            }
        }

        private fun SceytItemPopupAddReactionBinding.applyStyle() {
            addEmoji.setImageDrawable(style.moreIcon)
            addEmoji.backgroundTintList = ColorStateList.valueOf(style.moreBackgroundColor)
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
