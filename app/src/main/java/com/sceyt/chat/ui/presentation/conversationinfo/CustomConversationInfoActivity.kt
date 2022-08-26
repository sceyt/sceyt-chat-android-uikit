package com.sceyt.chat.ui.presentation.conversationinfo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.CustomConversationInfoActivityBinding
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.extensions.asAppCompatActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ViewPagerAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment

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
        return CustomFragment.newInstance(channel)
    }

    class CustomFragment : ChannelMediaFragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return LayoutInflater.from(context).inflate(R.layout.custom_fragment_channel_media, container, false)
        }

        companion object {
            fun newInstance(channel: SceytChannel): CustomFragment {
                val fragment = CustomFragment()
                fragment.setBundleArguments {
                    putParcelable(CHANNEL, channel)
                }
                return fragment
            }
        }
    }

    companion object {
        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomConversationInfoActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asAppCompatActivity().overridePendingTransition(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }
}