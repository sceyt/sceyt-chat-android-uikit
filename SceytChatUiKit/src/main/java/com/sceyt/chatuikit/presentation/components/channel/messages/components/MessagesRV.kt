package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.util.Predicate
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.addRVScrollListener
import com.sceyt.chatuikit.extensions.getFirstVisibleItemPosition
import com.sceyt.chatuikit.extensions.getLastVisibleItemPosition
import com.sceyt.chatuikit.extensions.lastVisibleItemPosition
import com.sceyt.chatuikit.presentation.common.recyclerview.SpeedyLinearLayoutManager
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.ItemOffsetDecoration
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.sticky_date.StickyDateHeaderUpdater
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.shared.helpers.MessageSwipeController
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlin.math.absoluteValue


class MessagesRV @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {
    private lateinit var mAdapter: MessagesAdapter
    private var viewHolderFactory = MessageViewHolderFactory(context)
    private var messageSwipeController: MessageSwipeController? = null

    private var scrollStateChangeListener: ((Int) -> Unit)? = null

    // Loading prev properties
    private var needLoadPrevMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? =
        null
    private var reachToStartInvoked = false
    private var reachToPrefetchDistanceToLoadPrevInvoked = false
    private var reachToStartListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    // Loading next properties
    private var needLoadNextMessagesListener: ((offset: Int, message: MessageListItem?) -> Unit)? =
        null
    private var reachToEndInvoked = false
    private var reachToPrefetchDistanceToLoadNextInvoked = false
    private var reachToEndListener: ((offset: Int, message: MessageListItem?) -> Unit)? = null

    private var showHideDownScroller: ((show: Boolean) -> Unit)? = null
    private var swipeToReplyListener: ((MessageListItem) -> Unit)? = null
    private var enableSwipe: Boolean = true
    private lateinit var style: MessagesListViewStyle
    private var scrollY = 0

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

        layoutAnimation = AnimationUtils.loadLayoutAnimation(
            context, R.anim.sceyt_layout_anim_messages
        )

        layoutManager = SpeedyLinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        addOnScrollListener()
    }

    private val messageListQueryLimit get() = SceytChatUIKit.config.queryLimits.messageListQueryLimit

    private fun addOnScrollListener() {
        addRVScrollListener(onScrolled = { _: RecyclerView, _: Int, dy: Int ->
            scrollY += dy
            checkNeedLoadPrev(dy)
            checkNeedLoadNext(dy)
        }, onScrollStateChanged = { _, newState ->
            scrollStateChangeListener?.invoke(newState)
        })

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (scrollState != SCROLL_STATE_IDLE || ::mAdapter.isInitialized.not()) return@addOnLayoutChangeListener
            scrollY = computeVerticalScrollOffset()
            checkNeedLoadPrev(-1)
            checkNeedLoadNext(1)
        }
    }

    private fun checkNeedLoadPrev(dy: Int) {
        if (mAdapter.itemCount == 0) return
        val firstVisiblePosition = getFirstVisibleItemPosition()
        if (firstVisiblePosition <= messageListQueryLimit / 2 && dy < 0) {
            if (firstVisiblePosition == 0) {
                if (!reachToStartInvoked) {
                    val skip = mAdapter.getSkip()
                    val firstItem = mAdapter.getFirstMessageItem()
                    reachToStartInvoked = true
                    reachToPrefetchDistanceToLoadPrevInvoked = true
                    reachToStartListener?.invoke(skip, firstItem)
                    needLoadPrevMessagesListener?.invoke(skip, firstItem)
                }
            } else reachToStartInvoked = false

            if (!reachToPrefetchDistanceToLoadPrevInvoked) {
                reachToPrefetchDistanceToLoadPrevInvoked = true
                needLoadPrevMessagesListener?.invoke(
                    mAdapter.getSkip(),
                    mAdapter.getFirstMessageItem()
                )
            }
        } else reachToPrefetchDistanceToLoadPrevInvoked = false
    }

    private fun checkNeedLoadNext(dy: Int) {
        if (mAdapter.itemCount == 0) return
        val lastVisiblePosition = getLastVisibleItemPosition()
        checkScrollDown()

        if (mAdapter.itemCount - lastVisiblePosition <= messageListQueryLimit / 2 && dy > 0) {
            if (lastVisiblePosition == mAdapter.itemCount - 1) {
                if (!reachToEndInvoked) {
                    val skip = mAdapter.getSkip()
                    val lastSentItem = getLastSentItem()
                    reachToEndInvoked = true
                    reachToPrefetchDistanceToLoadNextInvoked = true
                    reachToEndListener?.invoke(skip, lastSentItem)
                    needLoadNextMessagesListener?.invoke(skip, lastSentItem)
                }
            } else reachToEndInvoked = false

            if (!reachToPrefetchDistanceToLoadNextInvoked) {
                reachToPrefetchDistanceToLoadNextInvoked = true
                needLoadNextMessagesListener?.invoke(mAdapter.getSkip(), getLastSentItem())
            }
        } else reachToPrefetchDistanceToLoadNextInvoked = false
    }

    private fun getLastSentItem() = mAdapter.getLastMessageBy {
        it is MessageListItem.MessageItem && it.message.isNotPending()
    }

    private fun checkScrollDown() {
        val canScrollVertically = canScrollVertically(0)
        if (!canScrollVertically)
            scrollY = 0

        val show = canScrollVertically && scrollY.absoluteValue >= 300
        showHideDownScroller?.invoke(show)
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

    internal fun setStyle(style: MessagesListViewStyle) {
        this.style = style
        addItemDecoration(ItemOffsetDecoration(style))
        viewHolderFactory.setStyle(style)
    }

    fun setData(
        messages: List<MessageListItem>,
        lifecycleScope: LifecycleCoroutineScope,
        force: Boolean = false
    ) {
        if (::mAdapter.isInitialized.not()) {
            adapter = MessagesAdapter(
                messages = SyncArrayList(collection = messages),
                viewHolderFactory = viewHolderFactory,
                style = style,
                scope = lifecycleScope,
                recyclerView = this
            ).also {
                it.setHasStableIds(true)
                mAdapter = it
            }
            scheduleLayoutAnimation()

            if (style.enableDateSeparator)
                StickyDateHeaderUpdater(this, parent as ViewGroup, mAdapter, style)

            val swipeController = MessageSwipeController(
                context = context,
                style = style.messageItemStyle
            ) { position ->
                Handler(Looper.getMainLooper()).postDelayed({
                    mAdapter.getData().getOrNull(position)?.let {
                        swipeToReplyListener?.invoke(it)
                    }
                }, 100)
            }.also { messageSwipeController = it }
            swipeController.setSwipeEnabled(enableSwipe)

            val itemTouchHelper = ItemTouchHelper(swipeController)
            itemTouchHelper.attachToRecyclerView(this)
        } else if (force)
            mAdapter.forceUpdate(messages)
        else
            mAdapter.notifyUpdate(messages)
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

    fun getData(): List<MessageListItem> {
        return if (::mAdapter.isInitialized)
            mAdapter.getData()
        else emptyList()
    }

    fun addNextPageMessages(
        messages: List<MessageListItem>,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        if (::mAdapter.isInitialized.not())
            setData(messages, lifecycleScope)
        else
            mAdapter.addNextPageMessagesList(messages)
    }

    fun addPrevPageMessages(
        messages: List<MessageListItem>,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        if (::mAdapter.isInitialized.not())
            setData(messages, lifecycleScope)
        else
            mAdapter.addPrevPageMessagesList(messages)
    }

    fun addNewMessages(
        vararg items: MessageListItem,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        if (::mAdapter.isInitialized.not())
            setData(items.toList(), lifecycleScope)
        else {
            mAdapter.addNewMessages(items.toList())
            var outGoing = true
            items.find { it is MessageListItem.MessageItem }?.let {
                outGoing = (it as MessageListItem.MessageItem).message.incoming.not()
            }
            checkScrollToEnd(items.size, outGoing)
        }
    }

    fun setScrollStateChangeListener(listener: (Int) -> Unit) {
        scrollStateChangeListener = listener
    }

    fun setNeedLoadPrevMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        needLoadPrevMessagesListener = listener
    }

    fun setNeedLoadNextMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        needLoadNextMessagesListener = listener
    }

    fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        reachToStartListener = listener
    }

    fun setReachToEndListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        reachToEndListener = listener
    }

    fun setMessageListener(listener: MessageClickListeners) {
        viewHolderFactory.setMessageListener(listener)
    }

    fun setMessageDisplayedListener(listener: (message: MessageListItem) -> Unit) {
        viewHolderFactory.setMessageDisplayedListener(listener)
    }

    fun setVoicePlayPauseListener(listener: (FileListItem, SceytMessage, playing: Boolean) -> Unit) {
        viewHolderFactory.setVoicePlayPauseListener(listener)
    }

    fun setScrollDownControllerListener(listener: (Boolean) -> Unit) {
        showHideDownScroller = listener
    }

    fun setSwipeToReplyListener(listener: (MessageListItem) -> Unit) {
        swipeToReplyListener = listener
    }

    /** Call this function to customise MessageViewHolderFactory and set your own.
     * Note: Call this function before initialising messages adapter.*/
    fun setViewHolderFactory(factory: MessageViewHolderFactory) {
        check(::mAdapter.isInitialized.not()) { "Adapter was already initialized, please set MessageViewHolderFactory first" }
        viewHolderFactory = factory
    }

    fun getViewHolderFactory() = viewHolderFactory

    fun sortMessages() {
        if (::mAdapter.isInitialized.not()) return
        mAdapter.sort(this)
    }

    fun deleteMessageByTid(vararg tid: Long) {
        if (::mAdapter.isInitialized)
            mAdapter.deleteMessageByTIds(tid.toList())
    }

    fun hideLoadingPrevItem() {
        if (::mAdapter.isInitialized)
            mAdapter.removeLoadingPrev()
    }

    fun hideLoadingNextItem() {
        if (::mAdapter.isInitialized)
            mAdapter.removeLoadingNext()
    }

    fun removeUnreadMessagesSeparator() {
        if (::mAdapter.isInitialized)
            mAdapter.removeUnreadMessagesSeparator()
    }

    fun clearData() {
        if (::mAdapter.isInitialized)
            mAdapter.clearData()
    }

    fun deleteAllMessagesBefore(predicate: Predicate<MessageListItem>) {
        if (::mAdapter.isInitialized)
            mAdapter.deleteAllMessagesBefore(predicate)
    }

    fun setSwipeToReplyEnabled(enabled: Boolean) {
        if (::mAdapter.isInitialized)
            messageSwipeController?.setSwipeEnabled(enabled)
        enableSwipe = enabled
    }

    fun getMessagesAdapter() = if (::mAdapter.isInitialized) mAdapter else null

    fun updateItemAt(index: Int, updatedItem: MessageListItem.MessageItem) {
        if (::mAdapter.isInitialized)
            mAdapter.updateItemAt(index, updatedItem)
    }
}
