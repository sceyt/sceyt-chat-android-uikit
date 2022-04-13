package com.sceyt.chat.ui.presentation.channels.adapter

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

object ChannelViewHolderFactory {
    private lateinit var asyncLayoutInflater: AsyncLayoutInflater
    private val cachedViews = Stack<ItemChannelBinding>()


    fun cash(context: AppCompatActivity) {
        asyncLayoutInflater = AsyncLayoutInflater(context)

        context.lifecycleScope.launch(Dispatchers.IO) {
            for (i in 0..20) {
                asyncLayoutInflater.inflate(R.layout.item_channel, null) { view, _, _ ->
                    cachedViews.push(ItemChannelBinding.bind(view))
                }
            }
        }
    }

    // Todo from ui performance
    fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ChannelType.Default.ordinal) {
            val view: ItemChannelBinding = if (cachedViews.isEmpty()) {
                cash(parent.context as AppCompatActivity)
                ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            } else {
                cachedViews.pop().also { it.root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
            }
            return ChannelViewHolder(view)

        } else LoadingViewHolder(ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false))


        /*   return when (viewType) {
               ChannelType.Default.ordinal -> {
                   ChannelViewHolder(ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
               }
               else -> LoadingViewHolder(ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
           }*/
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