package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytUiItemChannelBinding
import com.sceyt.chat.ui.databinding.SceytUiItemLoadingMoreBinding
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelListeners
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelsListenersImpl
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class ChannelViewHolderFactory(context: Context) {

    private var listeners = ChannelsListenersImpl()
    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ChannelListItem> {
        return when (viewType) {
            ChannelType.Default.ordinal -> {
                val view: SceytUiItemChannelBinding = if (cachedViews.isNullOrEmpty()) {
                    cashViews(parent.context)
                    SceytUiItemChannelBinding.inflate(layoutInflater, parent, false)
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
                SceytUiItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw Exception("Not supported view type")
        }
    }

    fun setChannelListener(listener: ChannelListeners) {
        listeners.setListener(listener)
    }

    companion object {
        private lateinit var asyncLayoutInflater: AsyncLayoutInflater
        private var cachedViews: Stack<SceytUiItemChannelBinding>? = Stack<SceytUiItemChannelBinding>()
        private var cashJob: Job? = null

        fun cashViews(context: Context, count: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE) {
            asyncLayoutInflater = AsyncLayoutInflater(context)

            cashJob = (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                for (i in 0..count) {
                    asyncLayoutInflater.inflate(R.layout.sceyt_ui_item_channel, null) { view, _, _ ->
                        cachedViews?.push(SceytUiItemChannelBinding.bind(view))
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
            is ChannelListItem.LoadingMoreItem -> return ChannelType.Loading.ordinal
        }
    }

    enum class ChannelType {
        Loading, Default
    }
}