package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.isFirstCompletelyItemDisplaying
import com.sceyt.chat.ui.extensions.isFirstItemDisplaying
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.ChatItemOffsetDecoration
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import java.util.concurrent.atomic.AtomicBoolean

class MessagesRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: MessagesAdapter
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private val viewHolderFactory = MessageViewHolderFactory(context)
    private var richToStartInvoked = AtomicBoolean(false)

    init { init() }

    private fun init() {
        setHasFixedSize(true)
        itemAnimator = DefaultItemAnimator().apply {
            addDuration = 0
            removeDuration = 100
        }
        addItemDecoration(ChatItemOffsetDecoration(context, R.dimen.margin_top))
        scheduleLayoutAnimation()
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
            stackFromEnd = true
        }
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, _: Int ->
                if (isFirstItemDisplaying()) {
                    if (!richToStartInvoked.get()) {
                        richToStartInvoked.set(true)
                        richToStartListener?.invoke(mAdapter.getSkip(), mAdapter.getFirstItem())
                    }
                } else richToStartInvoked.set(false)

                if (isFirstCompletelyItemDisplaying())
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

    fun setRichToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToStartListener = listener
    }
}