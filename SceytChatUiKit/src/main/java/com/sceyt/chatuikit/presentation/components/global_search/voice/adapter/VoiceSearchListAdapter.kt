package com.sceyt.chatuikit.presentation.components.global_search.voice.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.AttachmentDiff
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.common.recyclerview.GlobalSearchListItemDiffCallback
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import kotlinx.coroutines.CoroutineScope

open class VoiceSearchListAdapter(
    scope: CoroutineScope,
    private val viewHolderFactory: VoiceSearchViewHolderFactory,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val differ = AsyncListDiffer(
        adapter = this,
        diffCallback = GlobalSearchListItemDiffCallback(),
        scope = scope
    )

    val currentList get() = differ.currentList

    open fun submitList(
        items: List<GlobalSearchListItem>,
        commitCallback: (() -> Unit)? = null,
    ) {
        differ.submitList(items, commitCallback)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(differ.currentList[position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        viewHolderFactory.onBindViewHolder(
            holder = holder,
            item = differ.currentList[position],
        )
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any?>
    ) {
        val diff = payloads.find { it is AttachmentDiff } as? AttachmentDiff

        if (diff != null) {
            viewHolderFactory.onBindViewHolder(
                holder = holder,
                item = differ.currentList[position],
                diff = diff
            )
        } else onBindViewHolder(holder, position)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as? BaseFileViewHolder<*>)?.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as? BaseFileViewHolder<*>)?.onViewDetachedFromWindow()
    }
}
