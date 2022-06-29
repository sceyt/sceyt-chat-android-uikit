package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.extensions.dpToPx
import com.sceyt.chat.ui.extensions.getFirstVisibleItemPosition
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.ChatItemOffsetDecoration
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.presentation.common.SpeedyLinearLayoutManager
import java.util.concurrent.atomic.AtomicBoolean


class MessagesRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: MessagesAdapter
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToPrefetchDistanceListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var needLoadMoreMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var viewHolderFactory = MessageViewHolderFactory(context)
    private var richToStartInvoked = AtomicBoolean(false)
    private var richToPrefetchDistanceInvoked = AtomicBoolean(false)

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

        layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.sceyt_layout_animation_fall_down).apply {
            animation.duration = 300
        }

        addItemDecoration(ChatItemOffsetDecoration(dpToPx(8f)))
        layoutManager = SpeedyLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false).apply {
            stackFromEnd = true
        }
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        post {
            addRVScrollListener { _: RecyclerView, _: Int, dy: Int ->
                val firstVisiblePosition = getFirstVisibleItemPosition()
                if (firstVisiblePosition <= SceytUIKitConfig.MESSAGES_LOAD_SIZE / 2 && dy < 0) {
                    val skip = mAdapter.getSkip()
                    val firstItem = mAdapter.getFirstMessageItem()
                    if (firstVisiblePosition == 0) {
                        if (!richToStartInvoked.get()) {
                            richToStartInvoked.set(true)
                            richToPrefetchDistanceInvoked.set(true)
                            richToStartListener?.invoke(skip, firstItem)
                            needLoadMoreMessagesListener?.invoke(skip, firstItem)
                        }
                    } else richToStartInvoked.set(false)

                    if (!richToPrefetchDistanceInvoked.get()) {
                        richToPrefetchDistanceInvoked.set(true)
                        richToPrefetchDistanceListener?.invoke(skip, firstItem)
                        needLoadMoreMessagesListener?.invoke(skip, firstItem)
                    }
                } else richToPrefetchDistanceInvoked.set(false)
            }
        }
    }

    fun setData(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not()) {
            adapter = MessagesAdapter(ArrayList(messages), viewHolderFactory)
                .also { mAdapter = it }
        } else
            mAdapter.notifyUpdate(messages)
        scheduleLayoutAnimation()
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    fun getLastMsg(): MessageListItem.MessageItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getLastMessageItem()
        } else null
    }

    fun getData(): ArrayList<MessageListItem>? {
        return if (::mAdapter.isInitialized)
            mAdapter.getData()
        else null
    }

    fun addNextPageMessages(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(messages)
        else
            mAdapter.addNextPageMessagesList(messages as MutableList<MessageListItem>)
    }

    fun addNewMessages(vararg items: MessageListItem) {
        if (::mAdapter.isInitialized.not())
            setData(items.toList())
        else {
            mAdapter.addNewMessages(items.toList())
            scrollToPosition(mAdapter.itemCount - 1)
        }
    }

    fun setNeedLoadMoreMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        needLoadMoreMessagesListener = listener
    }

    fun setRichToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToStartListener = listener
    }

    fun setRichToPrefetchDistanceListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToPrefetchDistanceListener = listener
    }

    fun setMessageListener(listener: MessageClickListeners) {
        viewHolderFactory.setMessageListener(listener)
    }


    /** Call this function to customise MessageViewHolderFactory and set your own.
     * Note: Call this function before initialising messages adapter.*/
    fun setViewHolderFactory(factory: MessageViewHolderFactory) {
        check(::mAdapter.isInitialized.not()) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun clearData() {
        mAdapter.clearData()
    }
}
