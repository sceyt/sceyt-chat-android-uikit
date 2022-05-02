package com.sceyt.chat.ui.presentation.channels.components.channels

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extencions.getCompatColor
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle

class ChannelsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var mChannelsRV: ChannelsRV

    init {
        setBackgroundColor(context.getCompatColor(R.color.colorBackground))

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelsListView)
            ChannelStyle.updateWithAttributes(a)
            a.recycle()
        }

        mChannelsRV = ChannelsRV(context)
        addView(mChannelsRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun showHideLoading(isLoading: Boolean) {

    }

    fun setReachToEndListener(listener: (offset: Int) -> Unit) {
        mChannelsRV.setRichToEndListeners(listener)
    }

    fun setChannelsList(channels: List<ChannelListItem>) {
        mChannelsRV.setData(channels)
    }

    fun addNewChannels(channels: List<ChannelListItem>) {
        mChannelsRV.addNewChannels(channels)
    }
}