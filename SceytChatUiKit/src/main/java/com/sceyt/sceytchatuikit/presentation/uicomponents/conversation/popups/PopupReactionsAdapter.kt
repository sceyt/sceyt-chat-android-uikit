package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemPopupAddReactionBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemPopupReactionBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class PopupReactionsAdapter(private var data: List<String>,
                            private var listener: OnItemClickListener) : RecyclerView.Adapter<BaseViewHolder<String>>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<String> {
        return if (viewType == ItemType.ADD.ordinal)
            AddViewHolder(SceytItemPopupAddReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        else
            ViewHolder(SceytItemPopupReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    override fun onBindViewHolder(holder: BaseViewHolder<String>, position: Int) {
        holder.bind(data.getOrNull(position) ?: "")
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < data.size)
            return ItemType.REACTION.ordinal
        else ItemType.ADD.ordinal
    }

    inner class ViewHolder(val binding: SceytItemPopupReactionBinding) : BaseViewHolder<String>(binding.root) {
        override fun bind(item: String) {
            binding.emojiView.setSmileText(item)

            binding.root.setOnClickListener {
                listener.onReactionClick(item)
            }
        }
    }

    inner class AddViewHolder(val binding: SceytItemPopupAddReactionBinding) : BaseViewHolder<String>(binding.root) {
        init {
            binding.setupStyle()
        }

        override fun bind(item: String) {
            binding.root.setOnClickListener {
                listener.onAddClick()
            }
        }

        private fun SceytItemPopupAddReactionBinding.setupStyle() {
            addEmoji.imageTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }
    }

    enum class ItemType {
        REACTION, ADD
    }

    interface OnItemClickListener {
        fun onReactionClick(reaction: String)
        fun onAddClick()
    }
}
