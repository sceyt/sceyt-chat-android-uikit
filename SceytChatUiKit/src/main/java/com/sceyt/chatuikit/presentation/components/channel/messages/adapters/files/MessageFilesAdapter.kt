package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.custom_views.VideoControllerView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseMessageFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.FilesViewHolderFactory
import com.sceyt.chatuikit.shared.utils.MyDiffUtil

class MessageFilesAdapter(
        private val message: SceytMessage,
        files: List<FileListItem>,
        private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<BaseMessageFileViewHolder>() {

    private var files = files.map { it.copy() }

    val videoControllersList = arrayListOf<VideoControllerView>()

    init {
        observeToAppLifeCycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMessageFileViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseMessageFileViewHolder, position: Int) {
        holder.bind(files[position], message)
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onViewAttachedToWindow(holder: BaseMessageFileViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseMessageFileViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun onItemDetached() {
        videoControllersList.forEach { it.release() }
        videoControllersList.clear()
    }

    fun getData() = files

    fun notifyUpdate(list: List<FileListItem>) {
        val unmatchedOld = files.toMutableList()
        val detached = list.map { item ->
            val oldIndex = unmatchedOld.indexOfFirst {
                it.attachment.hasSameFileIdentity(item.attachment)
            }
            val old = if (oldIndex == -1) null else unmatchedOld.removeAt(oldIndex)
            val thumbPath = item.thumbPath
                ?.takeIf { it.isNotBlank() }
                ?: old?.thumbPath

            item.copy(thumbPath = thumbPath)
        }

        val diff = DiffUtil.calculateDiff(MyDiffUtil(files, detached), true)
        files = detached
        diff.dispatchUpdatesTo(this)
    }

    private fun observeToAppLifeCycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY || event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)
                videoControllersList.forEach { it.pause() }
        })
    }
}

private fun SceytAttachment.hasSameFileIdentity(other: SceytAttachment): Boolean {
    if (messageTid != other.messageTid) return false

    val firstId = id?.takeIf { it > 0 }
    val secondId = other.id?.takeIf { it > 0 }
    if (firstId != null && secondId != null)
        return firstId == secondId

    return url.sameNonBlank(other.url) ||
            originalFilePath.sameNonBlank(other.originalFilePath) ||
            originalFilePath.sameNonBlank(other.filePath) ||
            filePath.sameNonBlank(other.originalFilePath) ||
            filePath.sameNonBlank(other.filePath)
}

private fun String?.sameNonBlank(other: String?) =
    !isNullOrBlank() && this == other
