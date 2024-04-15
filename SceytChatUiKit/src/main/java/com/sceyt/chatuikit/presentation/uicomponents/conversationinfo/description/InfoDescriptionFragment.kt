package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoDescriptionBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.jsonToObject
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ConversationInfoMediaStyle

open class InfoDescriptionFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoDescriptionBinding
        private set
    protected lateinit var channel: SceytChannel
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoDescriptionBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        setChannelDescription(channel)
        binding.setupStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    open fun setChannelDescription(channel: SceytChannel) {
        with(binding) {
            val about = if (channel.isDirect()) {
                channel.getPeer()?.user?.presence?.status
                        ?: SceytKitConfig.presenceStatusText
            } else channel.metadata?.jsonToObject(ChannelDescriptionData::class.java)?.description

            tvDescription.text = about
            binding.root.isVisible = about.isNotNullOrBlank()
        }
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDescription(channel)
    }

    private fun SceytFragmentInfoDescriptionBinding.setupStyle() {
        divider.layoutParams.height = ConversationInfoMediaStyle.dividerHeight
        divider.setBackgroundColor(requireContext().getCompatColor(ConversationInfoMediaStyle.dividerColor))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoDescriptionFragment {
            val fragment = InfoDescriptionFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}