package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.buttonfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytInfoPageLayoutButtonsPublicChannelBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.getMyRole
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class InfoButtonsPublicChannelFragment : Fragment() {
    private lateinit var binding: SceytInfoPageLayoutButtonsPublicChannelBinding
    private var buttonsListener: ((PublicChannelClickActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytInfoPageLayoutButtonsPublicChannelBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        binding.setOnClickListeners()
        binding.setupStyle()
        determinateState()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun SceytInfoPageLayoutButtonsPublicChannelBinding.setOnClickListeners() {
        muteUnMute.apply {
            if (channel.muted) {
                text = getString(R.string.sceyt_un_mute)
                setDrawableTop(R.drawable.sceyt_ic_un_mute, SceytKitConfig.sceytColorAccent)
            } else {
                text = getString(R.string.sceyt_mute)
                setDrawableTop(R.drawable.sceyt_ic_muted_channel, SceytKitConfig.sceytColorAccent)
            }
        }

        muteUnMute.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(if (channel.muted) PublicChannelClickActionsEnum.UnMute else PublicChannelClickActionsEnum.Mute)
        }

        join.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(PublicChannelClickActionsEnum.Join)
        }

        add.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(PublicChannelClickActionsEnum.Add)
        }

        leave.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(PublicChannelClickActionsEnum.Leave)
        }

        more.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(PublicChannelClickActionsEnum.More)
        }
    }

    private fun determinateState() {
        val myRole = channel.getMyRole()
        val isMember = myRole != null
        with(binding) {
            if (!isMember) {
                join.isVisible = true
                muteUnMute.isVisible = false
                more.isVisible = false
                add.isVisible = false
                leave.isVisible = false
                return
            }

            join.isVisible = false
            binding.muteUnMute.isVisible = true

            val enabledActions = myRole?.name == RoleTypeEnum.Owner.toString() || myRole?.name == "admin"
            more.isVisible = enabledActions
            add.isVisible = enabledActions
            leave.isVisible = !enabledActions

            binding.add.isVisible = enabledActions
        }
    }

    fun setClickActionsListener(listener: (PublicChannelClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    enum class PublicChannelClickActionsEnum {
        Mute, UnMute, Leave, Report, Join, Add, More
    }

    private fun SceytInfoPageLayoutButtonsPublicChannelBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(muteUnMute, join, add, more, leave),
            requireContext().getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoButtonsPublicChannelFragment {
            val fragment = InfoButtonsPublicChannelFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}