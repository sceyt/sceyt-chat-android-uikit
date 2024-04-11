package com.sceyt.chat.demo.presentation.conversationinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.sceyt.chat.demo.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum

class CustomConversationInfoActivity : ConversationInfoActivity() {

    override fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): ChannelMembersFragment {
        return CustomMembersFragment.newInstance(channel, memberType)
    }

    class CustomMembersFragment : ChannelMembersFragment() {
        private lateinit var addMembersActivityLauncher: ActivityResultLauncher<Intent>

        override fun onAddMembersClick(memberType: MemberTypeEnum) {
            val animOptions = ActivityOptionsCompat.makeCustomAnimation(requireContext(),
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold)
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext(), memberType, true), animOptions)
        }

        override fun onAddedMember(data: List<SceytMember>) {
            if (memberType == MemberTypeEnum.Admin)
                viewModel.changeRole(channel.id, *data.toTypedArray())
        }

        companion object {
            fun newInstance(channel: SceytChannel, membersType: MemberTypeEnum): CustomMembersFragment {
                val fragment = CustomMembersFragment()
                fragment.setBundleArguments {
                    putParcelable(CHANNEL, channel)
                    putInt(MEMBER_TYPE, membersType.ordinal)
                }
                return fragment
            }
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.parcelableArrayList<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                        if (memberType == MemberTypeEnum.Admin) {
                            users.map { it.role = Role(RoleTypeEnum.Admin.toString()) }
                            changeRole(*users.toTypedArray())
                        }
                        addMembersToChannel(users)
                    }
                }
            }
        }
    }

    companion object {
        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomConversationInfoActivity>(com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
            }
        }

        fun startWithLauncher(context: Context, channel: SceytChannel, launcher: ActivityResultLauncher<Intent>) {
            val intent = context.createIntent<CustomConversationInfoActivity>().apply {
                putExtra(CHANNEL, channel)
            }
            val animOptions = ActivityOptionsCompat.makeCustomAnimation(context, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold)
            launcher.launch(intent, animOptions)
        }
    }
}