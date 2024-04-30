package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoDescriptionBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.jsonToObject
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoStyleApplier
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytstyles.ConversationInfoStyle

open class InfoDescriptionFragment : Fragment(), ChannelUpdateListener, ConversationInfoStyleApplier {
    protected lateinit var binding: SceytFragmentInfoDescriptionBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ConversationInfoStyle
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
        binding.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    open fun setChannelDescription(channel: SceytChannel) {
        with(binding) {
            val about = if (channel.isDirect()) {
                channel.getPeer()?.user?.presence?.status
                        ?: SceytChatUIKit.config.presenceStatusText
            } else channel.metadata?.jsonToObject(ChannelDescriptionData::class.java)?.description

            tvDescription.text = about
            binding.root.isVisible = about.isNotNullOrBlank()
        }
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDescription(channel)
    }

    override fun setStyle(style: ConversationInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentInfoDescriptionBinding.applyStyle() {
        layoutDetails.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        tvTitle.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        tvDescription.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        border.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.borderColor))
        space.layoutParams.height = style.spaceBetweenSections
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