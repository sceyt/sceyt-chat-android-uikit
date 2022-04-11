package com.sceyt.chat.ui.presentation

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.extencions.dpToPx
import com.sceyt.chat.ui.presentation.adapter.ChannelsAdapter
import com.sceyt.chat.ui.utils.BounceEdgeEffectFactory

class ChannelsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    private lateinit var mAdapter: ChannelsAdapter

    init {
        init()
    }

    private fun init() {
        val paddingSize = dpToPx(10f)
        setPadding(paddingSize, 0, paddingSize, 0)
        clipToPadding = false
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        edgeEffectFactory = BounceEdgeEffectFactory()
    }

    fun setData(channels: List<Channel>) {
        if (!this::mAdapter.isInitialized) {
            mAdapter = ChannelsAdapter(channels)
            adapter = ChannelsAdapter(channels).also { mAdapter = it }
        } else {
            mAdapter.notifyUpdate(channels)
        }
    }
}