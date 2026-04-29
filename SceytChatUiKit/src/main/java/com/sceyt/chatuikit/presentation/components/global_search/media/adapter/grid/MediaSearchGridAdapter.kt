package com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.common.recyclerview.GlobalSearchListItemDiffCallback
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.MediaStickHeaderItemDecoration
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import kotlinx.coroutines.CoroutineScope

class MediaSearchGridAdapter(
    scope: CoroutineScope,
    private val factory: MediaSearchGridViewHolderFactory,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    MediaStickHeaderItemDecoration.StickyHeaderInterface<SceytItemGlobalSearchSectionBinding> {

    private val differ = AsyncListDiffer(
        adapter = this,
        diffCallback = GlobalSearchListItemDiffCallback(),
        scope = scope,
    )

    val currentList get() = differ.currentList

    fun submitList(items: List<GlobalSearchListItem>, commitCallback: (() -> Unit)? = null) {
        differ.submitList(items, commitCallback)
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int =
        factory.getItemViewType(differ.currentList[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return factory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        factory.onBindViewHolder(holder, differ.currentList[position])
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as? BaseFileViewHolder<*>)?.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as? BaseFileViewHolder<*>)?.onViewDetachedFromWindow()
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return differ.currentList.getOrNull(itemPosition) is GlobalSearchListItem.DateSeparator
    }

    override fun bindHeaderData(
        recyclerView: RecyclerView,
        headerPosition: Int,
    ): SceytItemGlobalSearchSectionBinding {
        val binding = SceytItemGlobalSearchSectionBinding.inflate(
            LayoutInflater.from(recyclerView.context),
            recyclerView,
            false
        )
        val item = differ.currentList.getOrNull(headerPosition) ?: return binding
        factory.bindDateSeparatorHeader(binding, item)
        return binding
    }
}
