package com.sceyt.chatuikit.presentation.components.select_users.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.databinding.SceytItemSelectedUserBinding
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.select_users.SelectedUsersListItemStyle

class SelectedUsersAdapter(
        data: List<UserItem.User>,
        private val style: SelectedUsersListItemStyle,
        private val listener: RemoveListener,
) : RecyclerView.Adapter<BaseViewHolder<UserItem.User>>() {
    private val users: ArrayList<UserItem.User> = data.toArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem.User> {
        val itemView = SceytItemSelectedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectedUserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<UserItem.User>, position: Int) {
        holder.bind(users[position])
    }

    inner class SelectedUserViewHolder(
            private val binding: SceytItemSelectedUserBinding,
    ) : BaseViewHolder<UserItem.User>(binding.root) {

        init {
            binding.applyStyle()
        }

        override fun bind(item: UserItem.User) {
            val presentableName = item.user.getPresentableName()
            binding.userName.text = presentableName
            binding.avatar.setUserAvatar(item.user)
            binding.onlineStatus.isVisible = item.user.presence?.state == PresenceState.Online
            binding.onlineStatus.setIndicatorColor(
                style.presenceStateColorProvider.provide(context, item.user.presence?.state
                        ?: PresenceState.Offline))
            binding.icRemove.setOnClickListener {
                listener.onRemoveClick(item)
                removeItem(item)
            }
        }

        private fun SceytItemSelectedUserBinding.applyStyle() {
            style.avatarStyle.apply(avatar)
            style.textStyle.apply(userName)
            icRemove.setImageDrawable(style.removeIcon)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun removeItem(item: UserItem.User) {
        users.findIndexed { it.user.id == item.user.id }?.let {
            users.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
    }

    fun addItem(item: UserItem.User) {
        users.add(item)
        notifyItemInserted(users.lastIndex)
    }

    fun interface RemoveListener {
        fun onRemoveClick(item: UserItem.User)
    }
}