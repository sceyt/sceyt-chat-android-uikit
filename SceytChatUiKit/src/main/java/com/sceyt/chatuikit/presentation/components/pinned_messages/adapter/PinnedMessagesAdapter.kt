package com.sceyt.chatuikit.presentation.components.pinned_messages.adapter

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemPinnedMessageBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesDiffUtil
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.MessageSelectableAnimHelper
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PinnedMessagesAdapter(
    private val style: PinnedMessagesStyle,
    private val messagesListStyle: MessagesListViewStyle,
    private val viewHolderFactory: MessageViewHolderFactory,
    private val onNavigateClick: (SceytMessage) -> Unit,
    private val onSelectClick: (SceytMessage) -> Unit,
) : RecyclerView.Adapter<PinnedMessagesAdapter.ViewHolder>() {

    private var items: List<MessageListItem> = emptyList()
    private var isMultiSelectableMode = false

    init {
        setHasStableIds(true)
    }

    fun setMultiSelectableMode(enable: Boolean) {
        isMultiSelectableMode = enable
    }

    fun isMultiSelectableMode() = isMultiSelectableMode

    fun getData() = items

    override fun getItemCount(): Int = items.size

    private fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int): Long = items[position].getItemId()

    override fun getItemViewType(position: Int): Int =
        viewHolderFactory.getItemViewType(getItem(position))

    suspend fun submit(newItems: List<MessageListItem>) {
        val oldItems = items
        val result = withContext(Dispatchers.Default) {
            DiffUtil.calculateDiff(MessagesDiffUtil(oldItems, newItems), true)
        }
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    fun replaceMessageItem(
        updatedItem: MessageListItem.MessageItem,
        positionHint: Int = RecyclerView.NO_POSITION,
    ): Int {
        val currentIndex = resolveMessageIndex(positionHint, updatedItem.message.tid)
        if (currentIndex != RecyclerView.NO_POSITION)
            items = items.toMutableList().also { it[currentIndex] = updatedItem }
        return currentIndex
    }

    private fun resolveMessageIndex(positionHint: Int, tid: Long): Int {
        val hintedItem = items.getOrNull(positionHint) as? MessageListItem.MessageItem
        if (hintedItem?.message?.tid == tid)
            return positionHint

        return items.indexOfFirst {
            it is MessageListItem.MessageItem && it.message.tid == tid
        }.takeIf { it >= 0 } ?: RecyclerView.NO_POSITION
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
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
        messagesListStyle.messageItemStyle.selectionCheckboxStyle.apply(binding.selectView)
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
        private val selectableAnimHelper by lazy { MessageSelectableAnimHelper(this) }
        private val checkBoxSize = dpToPx(22f)
        private var incoming = false

        init {
            binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                positionNavigateButton()
            }
        }

        fun bind(item: MessageListItem.MessageItem, diff: MessageDiff) = with(binding) {
            val message = item.message
            messageHolder.bind(item, diff)
            if (diff.selectionChanged || diff.statusChanged)
                bindSelectView(message)

            root.setOnClickListener { if (isMultiSelectableMode) onSelectClick(message) }

            incoming = message.incoming
            bubbleContainer.updateLayoutParams<MarginLayoutParams> {
                marginEnd = if (incoming) NAV_LANE else 0
            }

            navigateButton.setImageDrawable(style.navigateIcon)
            navigateButton.setOnClickListener { onNavigateClick(message) }
        }

        fun onViewAttachedToWindow() {
            messageHolder.onViewAttachedToWindow()
            currentMessage?.let { bindSelectView(it) }
        }

        fun setSelectableState() {
            val item = items.getOrNull(bindingAdapterPosition) ?: return
            selectableAnimHelper.setSelectableState(binding.selectView, item)
        }

        fun cancelSelectableState(onAnimEnd: (() -> Unit)? = null) {
            selectableAnimHelper.cancelSelectableState(binding.selectView, onAnimEnd)
        }

        private val currentMessage
            get() = (items.getOrNull(bindingAdapterPosition) as? MessageListItem.MessageItem)?.message

        private fun bindSelectView(message: SceytMessage) = with(binding.selectView) {
            if (isMultiSelectableMode) {
                isVisible = true
                isChecked = message.isSelected
                updateLayoutParams<MarginLayoutParams> {
                    marginStart = checkBoxSize / 2
                }
            } else isVisible = false
        }

        private fun positionNavigateButton(): Unit = with(binding) {
            val rowWidth = root.width
            if (rowWidth == 0) return
            val navWidth = navigateButton.width
            val navHeight = navigateButton.height

            bubbleBounds.set(0, 0, bubbleView.width, bubbleView.height)
            bubbleContainer.offsetDescendantRectToMyCoords(bubbleView, bubbleBounds)
            navigateButton.translationY =
                (bubbleBounds.bottom - navHeight).coerceAtLeast(0).toFloat()

            bubbleBounds.set(0, 0, bubbleView.width, bubbleView.height)
            root.offsetDescendantRectToMyCoords(bubbleView, bubbleBounds)
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
        val GAP = 8.dpToPx()
        val NAV_LANE = 52.dpToPx()
    }
}