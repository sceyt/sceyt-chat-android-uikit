package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemInfoAllReactionsHeaderBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemInfoReactionHeaderBinding
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ReactionsHeaderAdapter(private val data: ArrayList<ReactionHeaderItem>,
                             private val clickListener: OnItemClickListener) : RecyclerView.Adapter<BaseViewHolder<ReactionHeaderItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactionHeaderItem> {
        return when (viewType) {
            ViewType.Reaction.ordinal -> {
                val binding = SceytItemInfoReactionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReactionsHeaderViewHolder(binding)
            }
            else -> {
                val binding = SceytItemInfoAllReactionsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AllReactionsViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReactionHeaderItem>, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ReactionsHeaderViewHolder(val binding: SceytItemInfoReactionHeaderBinding) : BaseViewHolder<ReactionHeaderItem>(binding.root) {

        override fun bind(item: ReactionHeaderItem) {
            val score = (item as ReactionHeaderItem.Reaction).reactionScore

            with(binding.reaction) {
                setCountAndSmile(score.score, score.key)

                if (item.selected) {
                    setReactionBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                    setCountTextColor(Color.WHITE)
                } else {
                    setReactionBackgroundColor(Color.TRANSPARENT)
                    setCountTextColor(context.getCompatColor(R.color.sceyt_color_text_themed))
                }
            }

            binding.root.setOnClickListener {
                clickListener.onItemClick(item, bindingAdapterPosition)
            }
        }
    }

    inner class AllReactionsViewHolder(val binding: SceytItemInfoAllReactionsHeaderBinding) : BaseViewHolder<ReactionHeaderItem>(binding.root) {

        @SuppressLint("SetTextI18n")
        override fun bind(item: ReactionHeaderItem) {
            with(binding.tvAll) {
                text = "${itemView.getString(R.string.all)} ${(item as ReactionHeaderItem.All).count}"

                if (item.selected) {
                    background = GradientDrawable().apply {
                        color = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
                        cornerRadius = dpToPx(30f).toFloat()
                        setStroke(3, getCompatColor(R.color.sceyt_color_divider))
                    }
                    setTextColor(Color.WHITE)
                } else {
                    background = GradientDrawable().apply {
                        color = ColorStateList.valueOf(Color.WHITE)
                        cornerRadius = dpToPx(30f).toFloat()
                        setStroke(3, getCompatColor(R.color.sceyt_color_divider))
                    }
                    setTextColor(getCompatColor(R.color.sceyt_color_text_themed))
                }

                binding.root.setOnClickListener {
                    clickListener.onItemClick(item, bindingAdapterPosition)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is ReactionHeaderItem.Reaction -> ViewType.Reaction.ordinal
            is ReactionHeaderItem.All -> ViewType.All.ordinal
        }
    }

    fun setSelected(position: Int) {
        data.findIndexed { it.selected }?.let {
            it.second.selected = false
            notifyItemChanged(it.first, Any())
        }
        data[position].selected = true
        notifyItemChanged(position, Any())
    }

    fun addOrUpdateItem(reaction: ReactionScore) {
        data.findIndexed { it is ReactionHeaderItem.Reaction && it.reactionScore.key == reaction.key }?.let {
            (it.second as ReactionHeaderItem.Reaction).reactionScore = ReactionScore(reaction.key, reaction.score)
            notifyItemChanged(it.first, Any())
        } ?: let {
            data.add(ReactionHeaderItem.Reaction(ReactionScore(reaction.key, reaction.score)))
            notifyItemInserted(data.lastIndex)
        }
    }

    fun removeItem(reaction: Reaction) {
        data.findIndexed { it is ReactionHeaderItem.Reaction && it.reactionScore.key == reaction.key }?.let {
            data.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
    }

    fun updateAppItem(sumOf: Long) {
        if (data.isEmpty()) return
        data[0] = ReactionHeaderItem.All(sumOf).also { it.selected = data[0].selected }
        notifyItemChanged(0, Any())
    }

    enum class ViewType {
        Reaction,
        All
    }

    fun interface OnItemClickListener {
        fun onItemClick(item: ReactionHeaderItem, position: Int)
    }
}