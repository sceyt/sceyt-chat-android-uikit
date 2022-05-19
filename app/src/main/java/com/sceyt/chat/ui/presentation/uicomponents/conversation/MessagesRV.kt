package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.os.Handler
import android.service.autofill.Validators.not
import android.util.AttributeSet
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.awaitAnimationEnd
import com.sceyt.chat.ui.extensions.isFirstItemDisplaying
import com.sceyt.chat.ui.extensions.isLastItemDisplaying
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.ChatItemOffsetDecoration
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.BaseMsgViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import java.util.concurrent.atomic.AtomicBoolean

class MessagesRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: MessagesAdapter
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private val viewHolderFactory = MessageViewHolderFactory(context)
    private var richToStartInvoked = AtomicBoolean(false)

    init {
        init()
    }

    private fun init() {
        setHasFixedSize(true)
        itemAnimator = DefaultItemAnimator().apply {
            addDuration = 0
            removeDuration = 100
            moveDuration = 100
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
            }
        }
    }

    private fun checkScrollToEnd(isOutMessages: Boolean): Boolean {
        var scrollToEnd = isOutMessages
        if (!isOutMessages) {
            scrollToEnd = isLastItemDisplaying()
        }
        return scrollToEnd
    }

    fun setData(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = MessagesAdapter(messages as ArrayList<MessageListItem>, viewHolderFactory)
                .also { mAdapter = it }
        } else
            mAdapter.notifyUpdate(messages)
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    fun getLastMsg(): MessageListItem.MessageItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getLastItem()
        } else null
    }

    fun getData() = mAdapter.getData()

    fun addNextPageMessages(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(messages)
        else
            mAdapter.addNextPageMessagesList(messages as MutableList<MessageListItem>)
    }

    fun addNewMessages(vararg items: MessageListItem.MessageItem) {
        if (::mAdapter.isInitialized.not())
            setData(items.toList())
        else {
            val scrollToBottom = checkScrollToEnd(items.getOrNull(0)?.message?.incoming != true)
            mAdapter.addNewMessages(items.toList())


            if (scrollToBottom) {
                smoothScrollToPosition(mAdapter.itemCount)
            }
        }
    }

    fun setRichToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToStartListener = listener
    }

    fun setMessageListener(listener: MessageClickListeners) {
        viewHolderFactory.setMessageListener(listener)
    }

    fun updateReaction(messageId: Long, scores: Array<ReactionScore>) {
        for ((index: Int, item: MessageListItem) in mAdapter.getData().withIndex()) {
            if (item is MessageListItem.MessageItem && item.message.id == messageId) {
                (findViewHolderForAdapterPosition(index) as? BaseMsgViewHolder)?.updateReaction(scores, item.message)
                break
            }
        }
    }

    fun clearData() {
        mAdapter.clearData()
    }
}
