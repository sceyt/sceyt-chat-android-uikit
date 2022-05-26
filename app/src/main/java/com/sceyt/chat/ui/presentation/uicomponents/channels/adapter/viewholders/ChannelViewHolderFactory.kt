package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytItemChannelBinding
import com.sceyt.chat.ui.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelsClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class ChannelViewHolderFactory(context: Context) {

    private var listeners = ChannelsClickListenersImpl()
    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ChannelListItem> {
        return when (viewType) {
            ChannelType.Default.ordinal -> {
                val view: SceytItemChannelBinding = if (cachedViews.isNullOrEmpty()) {
                    cashViews(parent.context)
                    SceytItemChannelBinding.inflate(layoutInflater, parent, false)
                } else {
                    if (cachedViews!!.size < SceytUIKitConfig.CHANNELS_LOAD_SIZE / 2)
                        cashViews(parent.context, SceytUIKitConfig.CHANNELS_LOAD_SIZE / 2)

                    cachedViews!!.pop().also {
                        it.root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                }
                return ChannelViewHolder(view, listeners)
            }
            ChannelType.Loading.ordinal -> LoadingViewHolder(
                SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw Exception("Not supported view type")
        }
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        listeners.setListener(listener)
    }

    companion object {
        private lateinit var asyncLayoutInflater: AsyncLayoutInflater
        private var cachedViews: Stack<SceytItemChannelBinding>? = Stack<SceytItemChannelBinding>()
        private var cashJob: Job? = null

        fun cashViews(context: Context, count: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE) {
            asyncLayoutInflater = AsyncLayoutInflater(context)

            cashJob = (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                for (i in 0..count) {
                    asyncLayoutInflater.inflate(R.layout.sceyt_item_channel, null) { view, _, _ ->
                        cachedViews?.push(SceytItemChannelBinding.bind(view))
                    }
                }
            }
        }

        fun clearCash() {
            cashJob?.cancel()
            cachedViews?.clear()
            cachedViews = null
        }
    }

    fun getItemViewType(item: ChannelListItem): Int {
        return when (item) {
            is ChannelListItem.ChannelItem -> ChannelType.Default.ordinal
            is ChannelListItem.LoadingMoreItem -> ChannelType.Loading.ordinal
        }
    }

    enum class ChannelType {
        Loading, Default
    }
}