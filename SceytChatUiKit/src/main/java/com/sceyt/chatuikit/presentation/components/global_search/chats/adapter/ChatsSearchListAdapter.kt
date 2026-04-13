package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.common.recyclerview.GlobalSearchListItemDiffCallback
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import kotlinx.coroutines.CoroutineScope

open class ChatsSearchListAdapter(
    scope: CoroutineScope,
    private val viewHolderFactory: ChatsSearchViewHolderFactory,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val differ = AsyncListDiffer(
        adapter = this,
        diffCallback = GlobalSearchListItemDiffCallback(),
        scope = scope
    )

    private var highlightQuery: String = ""

    val currentList get() = differ.currentList

    open fun submitList(
        items: List<GlobalSearchListItem>,
        query: String,
        commitCallback: (() -> Unit)? = null,
    ) {
        highlightQuery = query
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
        payloads: MutableList<Any>,
    ) {
        val diff = payloads.find { it is ChannelDiff } as? ChannelDiff
        if (diff != null) {
            viewHolderFactory.onBindViewHolder(
                holder = holder,
                item = differ.currentList[position],
                diff = diff
            )
        } else {
            onBindViewHolder(holder, position)
        }
    }
}
