package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.addRVScrollListener
import com.sceyt.sceytchatuikit.extensions.getFirstVisibleItemPosition
import com.sceyt.sceytchatuikit.extensions.getLastVisibleItemPosition
import com.sceyt.sceytchatuikit.extensions.lastVisibleItemPosition
import com.sceyt.sceytchatuikit.presentation.common.SpeedyLinearLayoutManager
import com.sceyt.sceytchatuikit.presentation.common.SyncArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.ItemOffsetDecoration
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig


class MessagesRV @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: MessagesAdapter
    private var viewHolderFactory = MessageViewHolderFactory(context)

    // Loading prev properties
    private var needLoadPrevMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToStartInvoked = false
    private var richToPrefetchDistanceToLoadPrevInvoked = false
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    // Loading next properties
    private var needLoadNextMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToEndInvoked = false
    private var richToPrefetchDistanceToLoadNextInvoked = false
    private var richToEndListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    private var showHideDownScroller: ((show: Boolean) -> Unit)? = null

    init {
        init()
    }

    private fun init() {
        setHasFixedSize(true)
        itemAnimator = DefaultItemAnimator().apply {
            addDuration = 0
            removeDuration = 100
            moveDuration = 150
        }

        layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.sceyt_layout_anim_messages)

        addItemDecoration(ItemOffsetDecoration())
        layoutManager = SpeedyLinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        addOnScrollListener()
    }

    private fun addOnScrollListener() {
        addRVScrollListener { _: RecyclerView, _: Int, dy: Int ->
            checkNeedLoadPrev(dy)
            checkNeedLoadNext(dy)
        }

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (scrollState != SCROLL_STATE_IDLE || ::mAdapter.isInitialized.not()) return@postDelayed
                val lastPos = getLastVisibleItemPosition()
                checkScrollDown(lastPos)
                checkNeedLoadPrev(-1)
                checkNeedLoadNext(1)
            }, 80)
        }
    }

    private fun checkNeedLoadPrev(dy: Int) {
        if (mAdapter.itemCount == 0) return
        val firstVisiblePosition = getFirstVisibleItemPosition()
        if (firstVisiblePosition <= SceytKitConfig.MESSAGES_LOAD_SIZE / 2 && dy < 0) {
            val skip = mAdapter.getSkip()
            val firstItem = mAdapter.getFirstMessageItem()
            if (firstVisiblePosition == 0) {
                if (!richToStartInvoked) {
                    richToStartInvoked = true
                    richToPrefetchDistanceToLoadPrevInvoked = true
                    richToStartListener?.invoke(skip, firstItem)
                    needLoadPrevMessagesListener?.invoke(skip, firstItem)
                }
            } else richToStartInvoked = false

            if (!richToPrefetchDistanceToLoadPrevInvoked) {
                richToPrefetchDistanceToLoadPrevInvoked = true
                needLoadPrevMessagesListener?.invoke(skip, firstItem)
            }
        } else richToPrefetchDistanceToLoadPrevInvoked = false
    }

    private fun checkNeedLoadNext(dy: Int) {
        if (mAdapter.itemCount == 0) return
        val lastVisiblePosition = getLastVisibleItemPosition()
        checkScrollDown(lastVisiblePosition)

        if (mAdapter.itemCount - lastVisiblePosition <= SceytKitConfig.MESSAGES_LOAD_SIZE / 2 && dy > 0) {
            val skip = mAdapter.getSkip()
            val lastSentItem = mAdapter.getLastMessageBy {
                it is MessageListItem.MessageItem && it.message.deliveryStatus != DeliveryStatus.Pending
            }
            if (lastVisiblePosition == mAdapter.itemCount) {
                if (!richToEndInvoked) {
                    richToEndInvoked = true
                    richToPrefetchDistanceToLoadNextInvoked = true
                    richToEndListener?.invoke(skip, lastSentItem)
                    needLoadNextMessagesListener?.invoke(skip, lastSentItem)
                }
            } else richToEndInvoked = false

            if (!richToPrefetchDistanceToLoadNextInvoked) {
                richToPrefetchDistanceToLoadNextInvoked = true
                needLoadNextMessagesListener?.invoke(skip, lastSentItem)
            }
        } else richToPrefetchDistanceToLoadNextInvoked = false
    }

    private fun checkScrollDown(lastVisiblePosition: Int) {
        showHideDownScroller?.invoke(mAdapter.itemCount - lastVisiblePosition >= 2 && canScrollVertically(0))
    }

    private fun checkScrollToEnd(addedItemsCount: Int, isMySendMessage: Boolean): Boolean {
        var scrollToEnd: Boolean = isMySendMessage
        val lastIndex = mAdapter.itemCount - 1
        if (!isMySendMessage) {
            val last = lastVisibleItemPosition()
            scrollToEnd = if (last == NO_POSITION)
                true
            else last == lastIndex || (lastIndex > 0 && last == lastIndex - addedItemsCount)
        }
        if (scrollToEnd)
            scrollToPosition(lastIndex)
        return scrollToEnd
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(messages: List<MessageListItem>, force: Boolean = false) {
        if (::mAdapter.isInitialized.not()) {
            adapter = MessagesAdapter(SyncArrayList(messages), viewHolderFactory)
                .also { mAdapter = it }
            scheduleLayoutAnimation()
        } else if (force)
            mAdapter.notifyDataSetChanged()
        else
            mAdapter.notifyUpdate(messages, this)
    }

    fun isEmpty() = ::mAdapter.isInitialized.not() || mAdapter.getSkip() == 0

    fun getFirstMsg(): MessageListItem.MessageItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getFirstMessageItem()
        } else null
    }

    fun getLastMsg(): MessageListItem.MessageItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getLastMessageItem()
        } else null
    }

    fun getFirstMessageBy(predicate: (MessageListItem) -> Boolean): MessageListItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getFirstMessageBy(predicate)
        } else null
    }

    fun getLastMessageBy(predicate: (MessageListItem) -> Boolean): MessageListItem? {
        return if (::mAdapter.isInitialized) {
            mAdapter.getLastMessageBy(predicate)
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
            mAdapter.addNextPageMessagesList(messages)
    }

    fun addPrevPageMessages(messages: List<MessageListItem>) {
        if (::mAdapter.isInitialized.not())
            setData(messages)
        else
            mAdapter.addPrevPageMessagesList(messages)
    }

    fun addNewMessages(vararg items: MessageListItem) {
        if (::mAdapter.isInitialized.not())
            setData(items.toList())
        else {
            mAdapter.addNewMessages(items.toList())
            var outGoing = true
            items.find { it is MessageListItem.MessageItem }?.let {
                outGoing = (it as MessageListItem.MessageItem).message.incoming.not()
            }
            checkScrollToEnd(items.size, outGoing)
        }
    }

    fun setNeedLoadPrevMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        needLoadPrevMessagesListener = listener
    }

    fun setNeedLoadNextMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        needLoadNextMessagesListener = listener
    }

    fun setRichToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToStartListener = listener
    }

    fun setRichToEndListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToEndListener = listener
    }

    fun setMessageListener(listener: MessageClickListeners) {
        viewHolderFactory.setMessageListener(listener)
    }

    fun setMessageDisplayedListener(listener: (message: MessageListItem) -> Unit) {
        viewHolderFactory.setMessageDisplayedListener(listener)
    }

    fun setScrollDownControllerListener(listener: (Boolean) -> Unit) {
        showHideDownScroller = listener
    }

    /** Call this function to customise MessageViewHolderFactory and set your own.
     * Note: Call this function before initialising messages adapter.*/
    fun setViewHolderFactory(factory: MessageViewHolderFactory) {
        check(::mAdapter.isInitialized.not()) { "Adapter was already initialized, please set ChannelViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun getViewHolderFactory() = viewHolderFactory

    fun sortMessages() {
        if (::mAdapter.isInitialized.not()) return
        mAdapter.sort(this)
    }

    fun deleteMessageByTid(tid: Long) {
        if (::mAdapter.isInitialized)
            mAdapter.deleteMessageByTid(tid)
    }

    fun hideLoadingPrevItem() {
        if (::mAdapter.isInitialized)
            mAdapter.removeLoadingPrev()
    }

    fun hideLoadingNextItem() {
        if (::mAdapter.isInitialized)
            mAdapter.removeLoadingNext()
    }

    fun clearData() {
        mAdapter.clearData()
    }
}
