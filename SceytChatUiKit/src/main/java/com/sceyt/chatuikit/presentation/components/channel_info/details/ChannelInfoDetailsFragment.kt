package com.sceyt.chatuikit.presentation.components.channel_info.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoDetailsBinding
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDetailStyle
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle

open class ChannelInfoDetailsFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoDetailsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var infoStyle: ChannelInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var isSelf: Boolean = false

    protected val style: ChannelInfoDetailStyle
        get() = infoStyle.detailsStyle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoDetailsBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViews()
        setChannelDetails(channel)
        binding.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelInfoLinksFragment.CHANNEL))
        isSelf = channel.isSelf()
    }

    private fun initViews() {
        binding.avatar.setOnClickListener {
            onAvatarClick(channel)
        }
    }

    private fun setChannelDetails(channel: SceytChannel) {
        setSubtitle(channel)
        setChannelTitle(channel)
        setChannelAvatar(channel)
    }

    open fun setSubtitle(channel: SceytChannel) {
        with(binding) {
            if (channel.isPeerDeleted() || isSelf) {
                tvSubtitle.isVisible = false
                return
            }
            tvSubtitle.text = style.channelSubtitleFormatter.format(requireContext(), channel)
        }
    }

    open fun setChannelTitle(channel: SceytChannel) {
        binding.title.text = style.channelNameFormatter.format(requireContext(), channel)
    }

    open fun setChannelAvatar(channel: SceytChannel) {
        with(binding) {
            avatar.setChannelAvatar(
                channel = channel,
                defaultAvatarProvider = style.channelDefaultAvatarProvider,
                isSelf = isSelf
            )
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        buttonsListener?.invoke(ClickActionsEnum.Avatar)
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        if (isSelf) return
        setChannelAvatar(channel)
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    enum class ClickActionsEnum {
        Avatar
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDetails(channel)
    }

    override fun setStyle(style: ChannelInfoStyle) {
        this.infoStyle = style
    }

    private fun SceytFragmentChannelInfoDetailsBinding.applyStyle() {
        layoutDetails.setBackgroundColor(style.backgroundColor)
        style.titleTextStyle.apply(title)
        style.subtitleTextStyle.apply(tvSubtitle)
        dividerTop.dividerColor = infoStyle.borderColor
        space.layoutParams.height = infoStyle.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelInfoDetailsFragment {
            val fragment = ChannelInfoDetailsFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}