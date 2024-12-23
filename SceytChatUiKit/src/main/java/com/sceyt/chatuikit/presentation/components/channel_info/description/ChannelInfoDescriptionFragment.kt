package com.sceyt.chatuikit.presentation.components.channel_info.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelDescriptionData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoDescriptionBinding
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.jsonToObject
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDescriptionStyle
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle

open class ChannelInfoDescriptionFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoDescriptionBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var infoStyle: ChannelInfoStyle
        private set

    protected val style: ChannelInfoDescriptionStyle
        get() = infoStyle.descriptionStyle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoDescriptionBinding.inflate(layoutInflater, container, false)
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
        channel = requireNotNull(arguments?.parcelable(ChannelInfoLinksFragment.CHANNEL))
    }

    open fun setChannelDescription(channel: SceytChannel) {
        with(binding) {
            if (channel.isSelf) {
                root.isVisible = false
                return
            }
            val about = if (channel.isDirect()) {
                channel.getPeer()?.user?.presence?.status.takeIf { !it.isNullOrBlank() }
                        ?: SceytChatUIKit.config.presenceConfig.defaultPresenceStatus
            } else channel.metadata?.jsonToObject(ChannelDescriptionData::class.java)?.description

            tvDescription.text = about
            root.isVisible = about.isNotNullOrBlank()
        }
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDescription(channel)
    }

    override fun setStyle(style: ChannelInfoStyle) {
        this.infoStyle = style
    }

    private fun SceytFragmentChannelInfoDescriptionBinding.applyStyle() {
        layoutDetails.setBackgroundColor(style.backgroundColor)
        style.titleTextStyle.apply(tvTitle)
        style.descriptionTextStyle.apply(tvDescription)
        tvTitle.text = style.titleText
        border.setBackgroundColor(infoStyle.borderColor)
        space.layoutParams.height = infoStyle.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelInfoDescriptionFragment {
            val fragment = ChannelInfoDescriptionFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}