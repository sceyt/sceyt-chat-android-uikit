package com.sceyt.chat.ui.presentation.conversationinfo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.CustomConversationInfoActivityBinding
import com.sceyt.chat.ui.presentation.changerole.ChangeRoleActivity
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.asAppCompatActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ViewPagerAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment

class CustomConversationInfoActivity : ConversationInfoActivity() {
    private lateinit var binding: CustomConversationInfoActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.icBack.setOnClickListener {
            onBackPressed()
        }

        binding.clearHistory.setOnClickListener {
            clearHistory()
        }
    }

    override fun setActivityContentView() {
        setContentView(CustomConversationInfoActivityBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
    }

    override fun setPagerAdapter(pagerAdapter: ViewPagerAdapter) {
        binding.viewPager.adapter = pagerAdapter
        setupTabLayout(binding.tabLayout, binding.viewPager)
    }


    override fun onChannel(channel: SceytChannel) {
        with(binding) {
            Glide.with(this@CustomConversationInfoActivity).load(channel.iconUrl).into(avatar)
            subject.text = channel.channelSubject
        }
    }

    override fun getChannelMediaFragment(channel: SceytChannel): ChannelMediaFragment {
        return CustomMediaFragment.newInstance(channel)
    }

    override fun getChannelMembersFragment(channel: SceytChannel): ChannelMembersFragment {
        return CustomMembersFragment.newInstance(channel)
    }

    class CustomMediaFragment : ChannelMediaFragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return LayoutInflater.from(context).inflate(R.layout.custom_fragment_channel_media, container, false)
        }

        companion object {
            fun newInstance(channel: SceytChannel): CustomMediaFragment {
                val fragment = CustomMediaFragment()
                fragment.setBundleArguments {
                    putParcelable(CHANNEL, channel)
                }
                return fragment
            }
        }
    }

    class CustomMembersFragment : ChannelMembersFragment() {

        override fun changeRoleClick(member: SceytMember){
            changeRoleActivityLauncher.launch(ChangeRoleActivity.newInstance(requireContext(), member))
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