package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytFragmentInfoSettingsBinding
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setOnlyClickable
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytstyles.ConversationInfoMediaStyle

open class InfoSettingsFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoSettingsBinding
        private set
    protected lateinit var channel: SceytChannel
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
        binding.setupStyle()
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

        autoDeleteMessages.setOnClickListener {
            onAutoDeleteClick(channel)
        }
    }

    private fun setChannelDetails(channel: SceytChannel) {
        binding.root.isVisible = channel.checkIsMemberInChannel()
        binding.notification.isChecked = channel.muted
    }

    open fun onAutoDeleteClick(channel: SceytChannel) {
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
        setChannelDetails(channel)
    }

    private fun SceytFragmentInfoSettingsBinding.setupStyle() {
        divider.layoutParams.height = ConversationInfoMediaStyle.dividerHeight
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