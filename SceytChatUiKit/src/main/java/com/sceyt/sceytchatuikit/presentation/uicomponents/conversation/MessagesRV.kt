package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.*
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
    private var alreadyScrolledToUnreadMessages = false

    // Loading prev properties
    private var needLoadPrevMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToStartInvoked = false
    private var richToPrefetchDistanceToLoadPrevInvoked = false
    private var richToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToPrefetchDistanceToLoadPrevListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    // Loading next properties
    private var needLoadNextMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToEndInvoked = false
    private var richToPrefetchDistanceToLoadNextInvoked = false
    private var richToEndListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null
    private var richToPrefetchDistanceToLoadNextListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    private var showHideDownScroller: ((Boolean) -> Unit)? = null

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

        addOnLayoutChangeListener { _, _, top, _, _, _, oldTop, _, _ ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (scrollState != SCROLL_STATE_IDLE || ::mAdapter.isInitialized.not()) return@postDelayed
                val lastPos = getLastVisibleItemPosition()
                showHideDownScroller?.invoke(mAdapter.itemCount - lastPos > 2)
                checkNeedLoadPrev(oldTop - top)
                checkNeedLoadNext(oldTop - top)
            }, 50)
        }
    }

    private fun checkNeedLoadPrev(dy: Int) {
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
                richToPrefetchDistanceToLoadPrevListener?.invoke(skip, firstItem)
                needLoadPrevMessagesListener?.invoke(skip, firstItem)
            }
        } else richToPrefetchDistanceToLoadPrevInvoked = false
    }

    private fun checkNeedLoadNext(dy: Int) {
        val lastVisiblePosition = getLastVisibleItemPosition()
        showHideDownScroller?.invoke(mAdapter.itemCount - lastVisiblePosition > 2)

        if (mAdapter.itemCount - lastVisiblePosition <= SceytKitConfig.MESSAGES_LOAD_SIZE / 2 && dy > 0) {
            val skip = mAdapter.getSkip()
            val lastItem = mAdapter.getLastMessageItem()
            if (lastVisiblePosition == mAdapter.itemCount) {
                if (!richToEndInvoked) {
                    richToEndInvoked = true
                    richToPrefetchDistanceToLoadNextInvoked = true
                    richToEndListener?.invoke(skip, lastItem)
                    needLoadNextMessagesListener?.invoke(skip, lastItem)
                }
            } else richToEndInvoked = false

            if (!richToPrefetchDistanceToLoadNextInvoked) {
                richToPrefetchDistanceToLoadNextInvoked = true
                richToPrefetchDistanceToLoadNextListener?.invoke(skip, lastItem)
                needLoadNextMessagesListener?.invoke(skip, lastItem)
            }
        } else richToPrefetchDistanceToLoadNextInvoked = false
    }

    private fun checkScrollToEnd(addedItemsCount: Int, isMySendMessage: Boolean, isLastItemVisible: Boolean): Boolean {
        var scrollToEnd: Boolean = isMySendMessage
        val lastIndex = mAdapter.itemCount - 1
        if (!isMySendMessage) {
            val last = lastVisibleItemPosition()
            scrollToEnd = if (last == NO_POSITION)
                true
            else last == lastIndex || (lastIndex > 0 && last == lastIndex - addedItemsCount)
        }
        if (scrollToEnd) {
            if (isLastItemVisible)
                (layoutManager as SpeedyLinearLayoutManager).smoothScrollToPositionWithDuration(
                    this, lastIndex, 200f)
            else
                scrollToPosition(lastIndex)
        }
        return scrollToEnd
    }

    fun setData(messages: List<MessageListItem>, force: Boolean = false) {
        if (::mAdapter.isInitialized.not() || force) {
            adapter = MessagesAdapter(SyncArrayList(messages), viewHolderFactory)
                .also { mAdapter = it }
            scheduleLayoutAnimation()
        } else
            mAdapter.notifyUpdate(messages, this)

        /* if (alreadyScrolledToUnreadMessages.not())
             awaitAnimationEnd {
                 messages.findIndexed { it is MessageListItem.UnreadMessagesSeparatorItem }?.let {
                     scrollToPosition(it.first)
                     alreadyScrolledToUnreadMessages = true
                 }
             }*/
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
            val isLastItemVisible = isLastItemDisplaying()
            mAdapter.addNewMessages(items.toList())
            var outGoing = true
            items.find { it is MessageListItem.MessageItem }?.let {
                outGoing = (it as MessageListItem.MessageItem).message.incoming.not()
            }
            checkScrollToEnd(items.size, outGoing, isLastItemVisible)
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

    fun setRichToPrefetchDistanceToLoadPrevListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToPrefetchDistanceToLoadPrevListener = listener
    }

    fun setRichToPrefetchDistanceToLoadNextListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        richToPrefetchDistanceToLoadNextListener = listener
    }

    fun setMessageListener(listener: MessageClickListeners) {
        viewHolderFactory.setMessageListener(listener)
    }

    fun setMessageDisplayedListener(listener: (message: SceytMessage) -> Unit) {
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
