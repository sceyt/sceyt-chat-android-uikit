package com.sceyt.chatuikit.presentation.components.pinned_messages.adapter

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemPinnedMessageBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle

internal class PinnedMessagesAdapter(
    private val style: PinnedMessagesStyle,
    private val viewHolderFactory: MessageViewHolderFactory,
    private val onNavigateClick: (SceytMessage) -> Unit,
) : ListAdapter<MessageListItem, PinnedMessagesAdapter.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        viewHolderFactory.getItemViewType(getItem(position))

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.messageHolder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.messageHolder.onViewDetachedFromWindow()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SceytItemPinnedMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val messageHolder = viewHolderFactory.createViewHolder(binding.bubbleContainer, viewType)
        binding.bubbleContainer.addView(messageHolder.itemView)
        style.navigateButtonBackgroundStyle.apply(binding.navigateButton)
        return ViewHolder(binding, messageHolder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val item = getItem(position) as? MessageListItem.MessageItem ?: return
        val diff = payloads.filterIsInstance<MessageDiff>().firstOrNull() ?: MessageDiff.DEFAULT
        holder.bind(item, diff)
    }

    inner class ViewHolder(
        private val binding: SceytItemPinnedMessageBinding,
        val messageHolder: BaseMessageViewHolder,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val bubbleView = messageHolder.layoutBubbleConfig?.first ?: messageHolder.itemView
        private val bubbleBounds = Rect()
        private var incoming = false

        init {
            messageHolder.itemView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                positionNavigateButton()
            }
        }

        fun bind(item: MessageListItem.MessageItem, diff: MessageDiff) = with(binding) {
            val message = item.message
            messageHolder.bind(item, diff)

            incoming = message.incoming
            bubbleContainer.updateLayoutParams<MarginLayoutParams> {
                marginEnd = if (incoming) NAV_LANE else 0
            }
            positionNavigateButton()

            navigateButton.setImageDrawable(style.navigateIcon)
            navigateButton.setOnClickListener { onNavigateClick(message) }
        }

        private fun positionNavigateButton(): Unit = with(binding) {
            val bubble = bubbleView
            val rowWidth = root.width
            if (rowWidth == 0) return
            bubbleBounds.set(0, 0, bubble.width, bubble.height)
            root.offsetDescendantRectToMyCoords(bubble, bubbleBounds)
            val navWidth = navigateButton.width.takeIf { it > 0 }
                ?: navigateButton.layoutParams.width
            val navHeight = navigateButton.height.takeIf { it > 0 }
                ?: navigateButton.layoutParams.height

            navigateButton.translationY =
                (bubbleBounds.bottom - navHeight - navigateButton.top - NAVIGATION_BOTTOM_INSET)
                    .coerceAtLeast(0).toFloat()

            val isRtl = root.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val restingLeft = if (isRtl) rowWidth - navWidth else 0
            val left = if (incoming != isRtl) {
                bubbleBounds.right + GAP
            } else bubbleBounds.left - GAP - navWidth
            val clamped = left.coerceAtMost(rowWidth - navWidth - GAP).coerceAtLeast(GAP)
            navigateButton.translationX = (clamped - restingLeft).toFloat()
        }
    }

    private companion object {

        val DIFF = object : DiffUtil.ItemCallback<MessageListItem>() {
            override fun areItemsTheSame(
                oldItem: MessageListItem,
                newItem: MessageListItem,
            ) = oldItem.getItemId() == newItem.getItemId()

            override fun areContentsTheSame(
                oldItem: MessageListItem,
                newItem: MessageListItem,
            ) = when {
                oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem ->
                    !oldItem.message.expansionChanged(newItem.message) &&
                            !oldItem.message.diff(newItem.message).hasDifference()

                else -> oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: MessageListItem,
                newItem: MessageListItem,
            ): Any? {
                if (oldItem !is MessageListItem.MessageItem) return null
                if (newItem !is MessageListItem.MessageItem) return null
                val diff = oldItem.message.diff(newItem.message)
                return if (oldItem.message.expansionChanged(newItem.message))
                    diff.copy(bodyChanged = true)
                else diff
            }
        }

        private fun SceytMessage.expansionChanged(other: SceytMessage) =
            isBodyExpanded != other.isBodyExpanded

        val GAP = 8.dpToPx()

        val NAV_LANE = 52.dpToPx()

        val NAVIGATION_BOTTOM_INSET = 4.dpToPx()
    }
}