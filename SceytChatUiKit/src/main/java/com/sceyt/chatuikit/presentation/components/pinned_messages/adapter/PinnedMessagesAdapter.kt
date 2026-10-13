package com.sceyt.chatuikit.presentation.components.pinned_messages.adapter

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesDiffUtil
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PinnedMessagesAdapter(
    viewHolderFactory: MessageViewHolderFactory,
    messagesListStyle: MessagesListViewStyle,
    private val scope: LifecycleCoroutineScope,
    recyclerView: RecyclerView,
    private val style: PinnedMessagesStyle,
    private val onNavigateClick: (SceytMessage) -> Unit,
    private val items: SyncArrayList<MessageListItem> = SyncArrayList(),
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MessagesAdapter(items, viewHolderFactory, messagesListStyle, scope, recyclerView) {

    private var submitJob: Job? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMessageViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        val stub = holder.itemView.findViewById<View>(R.id.sideActionView) as? ViewStub ?: return holder
        (stub.inflate() as ImageView).apply {
            contentDescription = context.getString(R.string.sceyt_pinned_message)
            setImageDrawable(style.navigateIcon)
            style.navigateButtonBackgroundStyle.apply(this)
            setOnClickListener {
                val item = getFirstMessageBy { it.getItemId() == holder.itemId } as? MessageListItem.MessageItem
                item?.let { onNavigateClick(it.message) }
            }
        }
        return holder
    }

    fun submitList(newItems: List<MessageListItem>) {
        submitJob?.cancel()
        submitJob = scope.launch {
            val oldItems = ArrayList(items)
            val diffResult = withContext(backgroundDispatcher) {
                DiffUtil.calculateDiff(MessagesDiffUtil(oldItems, newItems), true)
            }
            items.clear()
            items.addAll(newItems)
            diffResult.dispatchUpdatesTo(this@PinnedMessagesAdapter)
        }
    }
}