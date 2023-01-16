package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytInfoPageLayoutButtonsPrivateChannelBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class InfoButtonsPrivateChatFragment : Fragment() {
    private lateinit var binding: SceytInfoPageLayoutButtonsPrivateChannelBinding
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytInfoPageLayoutButtonsPrivateChannelBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        binding.setOnClickListeners()
        binding.setupStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun SceytInfoPageLayoutButtonsPrivateChannelBinding.setOnClickListeners() {
        muteUnMute.apply {
            if (channel.muted) {
                text = getString(R.string.sceyt_un_mute)
                setDrawableTop(R.drawable.sceyt_ic_un_mute, SceytKitConfig.sceytColorAccent)
            } else {
                text = getString(R.string.sceyt_mute)
                setDrawableTop(R.drawable.sceyt_ic_muted_channel, SceytKitConfig.sceytColorAccent)
            }
        }

        call.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(ClickActionsEnum.Call)
        }

        video.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(ClickActionsEnum.VideoCall)
        }

        muteUnMute.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(if (channel.muted) ClickActionsEnum.UnMute else ClickActionsEnum.Mute)
        }

        more.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(ClickActionsEnum.More)
        }
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    enum class ClickActionsEnum {
        Call, VideoCall, Mute, UnMute, More
    }

    private fun SceytInfoPageLayoutButtonsPrivateChannelBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(muteUnMute, call, video, more),
            requireContext().getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoButtonsPrivateChatFragment {
            val fragment = InfoButtonsPrivateChatFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}