package com.sceyt.chat.ui.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.data.ChannelConfig
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.SceytResponse

class ChannelListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var mChannelsView: ChannelsView

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ChannelListView)
            ChannelConfig.updateWithAttributes(a)
            a.recycle()
        }

        mChannelsView = ChannelsView(context)
        addView(mChannelsView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun setChannelsList(channels: List<Channel>) {
        mChannelsView.setData(channels)
    }
}