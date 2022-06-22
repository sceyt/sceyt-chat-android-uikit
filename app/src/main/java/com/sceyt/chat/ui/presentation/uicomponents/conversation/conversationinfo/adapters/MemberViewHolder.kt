package com.sceyt.chat.ui.presentation.uicomponents.conversation.conversationinfo.adapters

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelMemberBinding
import com.sceyt.chat.ui.extensions.getPresentableName
import java.nio.file.Files.setOwner

class MemberViewHolder (itemView: ItemChannelMemberBinding, private val callbacks: Callbacks) :
        RecyclerView.ViewHolder(itemView.root){
    private lateinit var member: Member

    init {
        with(itemView) {
          //  onlineStatus.visibility = View.GONE
        }
    }

   /* fun bindTo(member: Member?) {
        this.member = member!!

        with(itemView) {

            avatar.user = member

            memberName.text = if (member == ChatClient.getClient().user) {
                val text = SpannableStringBuilder()
                    .append(member.fullName)
                    .color(youColor) { append(" " + context.getString(R.string.member_name_you)) }
                text
            } else {
                member.getPresentableName()
            }
            roleName.text = member.role.name

            setOnCreateContextMenuListener { menu, v, _ ->
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
            }
            onlineStatus.visibility =
                    if (member.presence.state == PresenceState.Online) View.VISIBLE else View.GONE
        }
    }*/

    interface Callbacks {
        fun removeMember(member: Member)
        fun changeRole(member: Member)
        fun blockAndRemoveMember(member: Member)
        fun setOwner(userId: Member)
    }
}