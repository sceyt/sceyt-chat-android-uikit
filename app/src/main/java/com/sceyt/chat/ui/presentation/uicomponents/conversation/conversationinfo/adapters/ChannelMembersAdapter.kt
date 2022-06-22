package com.sceyt.chat.ui.presentation.uicomponents.conversation.conversationinfo.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.customviews.SceytVideoControllerView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory

class ChannelMembersAdapter(private val files: ArrayList<FileListItem>,
                            private var viewHolderFactory: FilesViewHolderFactory
) /*: ListAdapter<>(DIFF_CALLBACK) {

    val videoControllersList = arrayListOf<SceytVideoControllerView>()

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

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Member>() {
            override fun areItemsTheSame(oldItem: Member, newItem: Member) =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Member, newItem: Member) =
                    oldItem.role.name == newItem.role.name
        }
    }
}*/