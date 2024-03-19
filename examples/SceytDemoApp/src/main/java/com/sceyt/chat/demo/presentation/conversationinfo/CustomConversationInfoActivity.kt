package com.sceyt.chat.demo.presentation.conversationinfo

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.sceyt.chat.demo.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.models.role.Role
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.createIntent
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelableArrayList
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.MemberTypeEnum

class CustomConversationInfoActivity : ConversationInfoActivity() {

    override fun getChannelMembersFragment(channel: SceytChannel, memberType: MemberTypeEnum): ChannelMembersFragment {
        return CustomMembersFragment.newInstance(channel, memberType)
    }

    class CustomMembersFragment : ChannelMembersFragment() {
        private lateinit var addMembersActivityLauncher: ActivityResultLauncher<Intent>

        override fun onAddMembersClick(memberType: MemberTypeEnum) {
            addMembersActivityLauncher.launch(AddMembersActivity.newInstance(requireContext(), memberType, true))
            requireContext().asActivity().overrideTransitions(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold, true)
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
            context.launchActivity<CustomConversationInfoActivity>(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold) {
                putExtra(CHANNEL, channel)
            }
        }

        fun startWithLauncher(context: Context, channel: SceytChannel, launcher: ActivityResultLauncher<Intent>) {
            val intent = context.createIntent<CustomConversationInfoActivity>().apply {
                putExtra(CHANNEL, channel)
            }
            val animOptions = ActivityOptionsCompat.makeCustomAnimation(context, anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
            launcher.launch(intent, animOptions)
        }
    }
}