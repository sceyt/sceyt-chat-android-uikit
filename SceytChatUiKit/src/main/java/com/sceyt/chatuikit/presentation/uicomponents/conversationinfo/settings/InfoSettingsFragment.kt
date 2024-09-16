package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoSettingsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytstyles.ChannelInfoStyle

open class InfoSettingsFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentInfoSettingsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ChannelInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoSettingsBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        binding.initViews()
        binding.applyStyle()
        setChannelDetails(channel)
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun SceytFragmentInfoSettingsBinding.initViews() {
        notification.setOnlyClickable()

        notification.setOnClickListener {
            onMuteUnMuteClick(channel)
        }

        autoDeleteMessages.setOnlyClickable()

        autoDeleteMessages.setOnClickListener {
            onAutoDeleteClick(channel)
        }
    }

    private fun setChannelDetails(channel: SceytChannel) {
        binding.root.isVisible = channel.checkIsMemberInChannel() && !channel.isSelf()
        binding.notification.isChecked = !channel.muted
        binding.autoDeleteMessages.isChecked = channel.autoDeleteEnabled
    }

    open fun onAutoDeleteClick(channel: SceytChannel) {
        buttonsListener?.invoke(if (channel.autoDeleteEnabled) ClickActionsEnum.AutoDeleteOff else ClickActionsEnum.AutoDeleteOn)
    }

    open fun onMuteUnMuteClick(channel: SceytChannel) {
        buttonsListener?.invoke(if (channel.muted) ClickActionsEnum.UnMute else ClickActionsEnum.Mute)
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    enum class ClickActionsEnum {
        Mute, UnMute, AutoDeleteOn, AutoDeleteOff
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        this.channel = channel
        if (::binding.isInitialized.not()) return
        setChannelDetails(channel)
    }

    override fun setStyle(style: ChannelInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentInfoSettingsBinding.applyStyle() {
        layoutDetails.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        notification.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        autoDeleteMessages.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        border.setDividerColorResource(SceytChatUIKit.theme.borderColor)
        space.layoutParams.height = style.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoSettingsFragment {
            val fragment = InfoSettingsFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}