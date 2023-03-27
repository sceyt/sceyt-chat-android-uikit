package com.sceyt.chat.ui.presentation.conversationinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.ui.presentation.addmembers.AddMembersActivity
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum

class CustomConversationInfoActivity : ConversationInfoActivity() {

    override fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): ChannelMembersFragment {
        return CustomMembersFragment.newInstance(channel, memberType)
    }

    override fun onAddSubscribersClick(channel: SceytChannel) {
        addMembersActivityLauncher.launch(AddMembersActivity.newInstance(this, MemberTypeEnum.Subscriber))
        overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
    }

    class CustomMembersFragment : ChannelMembersFragment() {
        private lateinit var addMembersActivityLauncher: ActivityResultLauncher<Intent>

        override fun onAddMembersClick(memberType: MemberTypeEnum) {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext(), memberType))
            requireContext().asActivity().overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
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

    private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.parcelableArrayList<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                addMembers(users)
            }
        }
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomConversationInfoActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asActivity().overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
        }
    }
}