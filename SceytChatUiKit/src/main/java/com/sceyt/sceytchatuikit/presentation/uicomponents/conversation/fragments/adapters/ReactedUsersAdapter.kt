package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.databinding.SceytItemReactedUserBinding
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle

class ReactedUsersAdapter(private var reactions: List<Reaction>,
                          private var clickListener: OnItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val binding = SceytItemReactedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsersViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return reactions.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as UsersViewHolder).bind(reactions[position])
    }

    inner class UsersViewHolder(val binding: SceytItemReactedUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Reaction) {
            with(binding) {
                val user = item.user
                val userPresentableName = SceytKitConfig.userNameBuilder?.invoke(item.user)
                        ?: user.getPresentableName()
                avatar.setNameAndImageUrl(userPresentableName, user.avatarURL, UserStyle.userDefaultAvatar)
                userName.text = userPresentableName

                reaction.setSmileText(item.key)

                root.setOnClickListener {
                    clickListener.onItemClick(item)
                }
            }
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick(item: Reaction)
    }
}