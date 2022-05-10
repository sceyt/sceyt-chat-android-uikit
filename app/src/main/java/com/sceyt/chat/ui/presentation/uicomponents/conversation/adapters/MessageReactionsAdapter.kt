package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.databinding.ItemAddReactionBinding
import com.sceyt.chat.ui.databinding.ItemReactionBinding

class MessageReactionsAdapter(
        private val reactions: List<ReactionScore>,/*
        private val onAddNewReactionCb: () -> Unit,
        private var onAddReactionCb: (ReactionScore) -> Unit,
        private var onReduceReactionCb: (ReactionScore) -> Unit,
        private var onDeleteReactionCb: (ReactionScore) -> Unit,*/
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewTypeReaction = 1
    private val viewTypeAdd = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeAdd -> {
                val binding = ItemAddReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AddReactionViewHolder(binding)
            }
            else -> {
                val binding = ItemReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReactionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            viewTypeReaction -> {
                (holder as ReactionViewHolder).bind(reactions[position])
            }
            viewTypeAdd -> {
                (holder as AddReactionViewHolder).bind()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (reactions[position].score == -1L)
            viewTypeAdd
        else viewTypeReaction
    }

    fun submitData(list: List<ReactionScore>) {
        //  submitList(list)
    }

    inner class ReactionViewHolder(private val binding: ItemReactionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: ReactionScore) {
            binding.tvEmoji.text = data.key
            binding.tvCount.text = data.score.toString()

            binding.root.setOnLongClickListener {
                showReactionPopup(it, reactions[bindingAdapterPosition])
                return@setOnLongClickListener false
            }
        }

        private fun showReactionPopup(view: View, reaction: ReactionScore) {
            /*val popup = PopupMenu(itemView.context, view)
            popup.menu.apply {
                add(0, R.id.add, 0, itemView.context.getString(R.string.add))
                add(0, R.id.remove, 0, itemView.context.getString(R.string.remove))
                add(0, R.id.delete, 0, itemView.context.getString(R.string.delete))
            }
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.add -> onAddReactionCb(reaction)
                    R.id.remove -> onReduceReactionCb(reaction)
                    R.id.delete -> onDeleteReactionCb(reaction)
                }
                false
            }
            popup.show()*/
        }
    }

    inner class AddReactionViewHolder(private val binding: ItemAddReactionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.root.setOnClickListener {
              //  onAddNewReactionCb.invoke()
            }
        }
    }

    override fun getItemCount(): Int {
        return reactions.size
    }

    /*  companion object {
          private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReactionScore>() {
              override fun areItemsTheSame(oldItem: ReactionScore, newItem: ReactionScore) =
                      oldItem.key == newItem.key

              override fun areContentsTheSame(oldItem: ReactionScore, newItem: ReactionScore) =
                      oldItem.score == newItem.score
          }
      }*/
}