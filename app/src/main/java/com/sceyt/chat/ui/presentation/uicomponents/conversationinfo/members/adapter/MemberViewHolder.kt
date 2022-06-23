package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter

import android.text.SpannableStringBuilder
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelMemberBinding
import com.sceyt.chat.ui.extensions.getPresentableName

class MemberViewHolder(val binding: ItemChannelMemberBinding) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var member: Member
    private val youColor = itemView.context.getColor(R.color.sceyt_color_gray_400)

    fun bind(member: Member) {
        this.member = member

        with(binding) {

            avatar.setNameAndImageUrl(member.fullName, member.avatarURL)

            memberName.text = if (member == ChatClient.getClient().user) {
                val text = SpannableStringBuilder()
                    .append(member.fullName)
                    .color(youColor) { append(" " + itemView.context.getString(R.string.member_name_you)) }
                text
            } else member.getPresentableName()

            roleName.text = member.role.name
            onlineStatus.isVisible = member.presence.state == PresenceState.Online

            /*setOnCreateContextMenuListener { menu, v, _ ->
                val kikMemberSp = SpannableString(resources.getString(R.string.remove_member))
                val blockAndKikMemberSp =
                        SpannableString(resources.getString(R.string.block_and_remove_member))
                val colorRedSpan = ForegroundColorSpan(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.colorFontWarning,
                        context.theme
                    )
                )
                kikMemberSp.setSpan(colorRedSpan, 0, kikMemberSp.length, 0)
                blockAndKikMemberSp.setSpan(colorRedSpan, 0, blockAndKikMemberSp.length, 0)

                if (!member.role.name.equals("owner", true)) {
                    menu.add(
                        0,
                        R.id.setOwner,
                        0,
                        R.string.set_owner
                    ).setOnMenuItemClickListener(this@ChannelMemberViewHolder)
                }

                menu.add(
                    0,
                    R.id.changeRole,
                    0,
                    R.string.change_role
                ).setOnMenuItemClickListener(this@ChannelMemberViewHolder)
                menu.add(
                    0,
                    R.id.removeMember,
                    0,
                    kikMemberSp
                ).setOnMenuItemClickListener(this@ChannelMemberViewHolder)
                menu.add(
                    0,
                    R.id.blockAndRemoveMember,
                    0,
                    blockAndKikMemberSp
                ).setOnMenuItemClickListener(this@ChannelMemberViewHolder)
            }*/
        }
    }

    interface Callbacks {
        fun removeMember(member: Member)
        fun changeRole(member: Member)
        fun blockAndRemoveMember(member: Member)
        fun setOwner(userId: Member)
    }
}