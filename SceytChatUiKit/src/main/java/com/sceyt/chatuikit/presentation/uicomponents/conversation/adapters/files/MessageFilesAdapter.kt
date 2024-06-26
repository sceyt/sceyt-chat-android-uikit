package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.customviews.SceytVideoControllerView
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.chatuikit.shared.utils.MyDiffUtil

class MessageFilesAdapter(private var files: ArrayList<FileListItem>,
                          private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<FileListItem>>() {

    val videoControllersList = arrayListOf<SceytVideoControllerView>()

    init {
        observeToAppLifeCycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<FileListItem>, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun onItemDetached() {
        videoControllersList.forEach { it.release() }
        videoControllersList.clear()
    }

    fun getData() = files

    fun notifyUpdate(list: List<FileListItem>) {
        val myDiffUtil = MyDiffUtil(files, list)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesTo(this)
        val thumbs = files.map { Pair(it.thumbPath, it.file.messageTid) }
        files = list.map {
            if (it !is FileListItem.LoadingMoreItem) {
                thumbs.find { longPair -> longPair.second == it.file.messageTid }?.let { pair ->
                    it.thumbPath = pair.first
                }
            }
            it
        }.toArrayList()
    }

    private fun observeToAppLifeCycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY || event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)
                videoControllersList.forEach { it.pause() }
        })
    }
}