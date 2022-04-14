package com.sceyt.chat.ui.presentation.channels.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelBinding
import com.sceyt.chat.ui.databinding.ItemLoadingBinding
import com.sceyt.chat.ui.presentation.channels.adapter.viewholders.ChannelViewHolder
import com.sceyt.chat.ui.presentation.channels.adapter.viewholders.LoadingViewHolder
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object ChannelViewHolderFactory {
    private lateinit var asyncLayoutInflater: AsyncLayoutInflater
    private val cachedViews = Stack<ItemChannelBinding>()

    fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ChannelType.Default.ordinal -> {
                val view: ItemChannelBinding = if (cachedViews.isEmpty()) {
                    cashViews(parent.context)
                    ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                } else {
                    if (cachedViews.size < SceytUIKitConfig.CHANNELS_LOAD_SIZE / 2)
                        cashViews(parent.context, SceytUIKitConfig.CHANNELS_LOAD_SIZE / 2)

                    cachedViews.pop().also { it.root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
                }
                return ChannelViewHolder(view)

            }
            ChannelType.Loading.ordinal -> LoadingViewHolder(ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw Exception("Not supported view type")
        }
    }

    fun cashViews(context: Context, count: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE) {
        asyncLayoutInflater = AsyncLayoutInflater(context)

        (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
            for (i in 0..count) {
                asyncLayoutInflater.inflate(R.layout.item_channel, null) { view, _, _ ->
                    cachedViews.push(ItemChannelBinding.bind(view))
                }
            }
        }
    }

    fun getItemViewType(item: ChannelListItem): Int {
        return if (item is ChannelListItem.LoadingMoreItem)
            return ChannelType.Loading.ordinal
        else ChannelType.Default.ordinal
    }

    fun clearCash() {
        cachedViews.clear()
    }

    enum class ChannelType {
        Loading, Default
    }
}