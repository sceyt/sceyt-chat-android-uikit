package com.sceyt.chat.ui.presentation.conversationinfo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.sceyt.chat.ui.presentation.addmembers.AddMembersActivity
import com.sceyt.chat.ui.presentation.changerole.ChangeRoleActivity
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.asAppCompatActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment

class CustomConversationInfoActivity : ConversationInfoActivity() {

    override fun getChannelMembersFragment(channel: SceytChannel): ChannelMembersFragment {
        return CustomMembersFragment.newInstance(channel)
    }

    class CustomMembersFragment : ChannelMembersFragment() {

        override fun changeRoleClick(member: SceytMember) {
            changeRoleActivityLauncher.launch(ChangeRoleActivity.newInstance(requireContext(), member))
            requireContext().asAppCompatActivity()
                .overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
        }

        override fun onAddMembersClick() {
            addMembersActivityLauncher.launch(Intent(requireContext(), AddMembersActivity::class.java))
            requireContext().asAppCompatActivity()
                .overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
        }

        companion object {
            fun newInstance(channel: SceytChannel): CustomMembersFragment {
                val fragment = CustomMembersFragment()
                fragment.setBundleArguments {
                    putParcelable(CHANNEL, channel)
                }
                return fragment
            }
        }

        private val changeRoleActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(ChangeRoleActivity.CHOSEN_ROLE)?.let { role ->
                    val member = result.data?.getParcelableExtra<SceytMember>(ChangeRoleActivity.MEMBER)
                            ?: return@let
                    if (role == "owner")
                        changeOwnerClick(member.id)
                    else
                        changeRoleClick(member, role)
                }
            }
        }

        private val addMembersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getParcelableArrayListExtra<SceytMember>(AddMembersActivity.SELECTED_USERS)?.let { users ->
                    addMembersToChannel(users)
                }
            }
        }
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomConversationInfoActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asAppCompatActivity().overridePendingTransition(anim.sceyt_anim_slide_in_right, anim.sceyt_anim_slide_hold)
        }
    }
}