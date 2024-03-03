package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.sceytchatuikit.persistence.differs.diff

class MediaDiffUtil(private var oldList: List<MediaItem>,
                    private var newList: List<MediaItem>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.file.id == newItem.file.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.file.diff(newItem.file).hasDifference().not()

    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.file.diff(newItem.file)
    }
}