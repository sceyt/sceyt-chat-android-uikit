package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.isFirstCompletelyItemDisplaying
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.ChatItemOffsetDecoration
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessagesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageViewHolderFactory

class MessagesRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: MessagesAdapter
    private var richToEndListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private val viewHolderFactory = MessageViewHolderFactory(context)

    init {
        init()
        ChannelViewHolderFactory.cashViews(context)
    }

    private fun init() {
        clipToPadding = false
        addItemDecoration(ChatItemOffsetDecoration(context, R.dimen.margin_top))
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
            stackFromEnd = true
        }
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, _: Int ->
                if (isLastItemDisplaying() && mAdapter.itemCount != 0)
                    richToEndListener?.invoke(mAdapter.getSkip(), mAdapter.getLastItem())

                if (isFirstCompletelyItemDisplaying() && mAdapter.itemCount != 0)
                    richToStartListener?.invoke(mAdapter.getSkip(), mAdapter.getFirstItem())
            }
        }
    }

    fun setData(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = MessagesAdapter(messages as ArrayList<MessageListItem>, viewHolderFactory)
                .also { mAdapter = it }
        } else
            mAdapter.notifyUpdate(messages)
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    fun addNewChannels(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(messages)
        else
            mAdapter.addList(messages as MutableList<MessageListItem>)
    }

    fun setRichToEndListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToEndListener = listener
    }

    fun setRichToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToStartListener = listener
    }

    fun setChannelListener(listener: ChannelListeners) {
        viewHolderFactory.setChannelListener(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ChannelViewHolderFactory.clearCash()
    }
}