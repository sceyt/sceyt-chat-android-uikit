package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.diff

class MemberDiffUtil(private var oldList: List<MemberItem>,
                     private var newList: List<MemberItem>,
                     private var showMoreIconChanged: Boolean) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return (oldItem == newItem)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is MemberItem.Member && newItem is MemberItem.Member)
            return oldItem.member.diff(newItem.member, showMoreIconChanged).hasDifference().not()
        return (oldItem == newItem)
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldMessage = oldList[oldItemPosition]
        val newMessage = newList[newItemPosition]
        if (oldMessage is MemberItem.Member && newMessage is MemberItem.Member)
            return oldMessage.member.diff(newMessage.member, showMoreIconChanged)
        return MemberItemPayloadDiff.DEFAULT
    }
}