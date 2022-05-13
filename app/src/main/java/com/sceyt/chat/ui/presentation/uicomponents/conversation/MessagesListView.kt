package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.binding.BindingUtil

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var messagesRV: MessagesRV
    private var pageStateView: PageStateView?

    init {
        setBackgroundColor(context.getCompatColor(R.color.colorBackground))
        BindingUtil.themedBackgroundColor(this, R.color.colorBackground)

        val attributes = intArrayOf(android.R.attr.paddingLeft, android.R.attr.paddingTop, android.R.attr.paddingBottom, android.R.attr.paddingRight)
        val a = context.obtainStyledAttributes(attrs, attributes)
        a.recycle()

        /* if (attrs != null) {
             val a = context.obtainStyledAttributes(attrs, R.styleable.MessagesListView)
             ChannelStyle.updateWithAttributes(a)
             a.recycle()
         }*/

        messagesRV = MessagesRV(context)
        messagesRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        messagesRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        addView(messagesRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addView(PageStateView(context).also {
            pageStateView = it
            it.setLoadingStateView(ChannelStyle.loadingState)
            it.setEmptyStateView(ChannelStyle.emptyState)
            it.setEmptySearchStateView(ChannelStyle.emptySearchState)
        })
    }

    fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToStartListener(listener)
    }

    fun setMessagesList(data: List<MessageListItem>) {
        messagesRV.setData(data)
    }

    fun addNewMessages(data: List<MessageListItem>) {
        messagesRV.addNewChannels(data)
    }

    fun updateState(state: BaseViewModel.PageState) {
        pageStateView?.updateState(state, messagesRV.isEmpty())
    }

    /* fun setMessageListListener(listener: ChannelListeners) {
         messagesRV.setChannelListener(listener)
     }*/
}