package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytInfoPageLayoutButtonsDirectChannelBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class InfoButtonsDirectChatFragment : Fragment() {
    private lateinit var binding: SceytInfoPageLayoutButtonsDirectChannelBinding
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private lateinit var channel: SceytChannel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytInfoPageLayoutButtonsDirectChannelBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        binding.setupStyle()
        binding.initViews()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.getParcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun SceytInfoPageLayoutButtonsDirectChannelBinding.initViews() {
        muteUnMute.apply {
            if (channel.muted) {
                text = getString(R.string.sceyt_un_mute)
                setDrawableTop(R.drawable.sceyt_ic_un_mute, SceytKitConfig.sceytColorAccent)
            } else {
                text = getString(R.string.sceyt_mute)
                setDrawableTop(R.drawable.sceyt_ic_muted_channel, SceytKitConfig.sceytColorAccent)
            }
        }

        video.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(ClickActionsEnum.VideCall)
        }

        audio.setOnClickListenerDisableClickViewForWhile {
            buttonsListener?.invoke(ClickActionsEnum.AudioCall)
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
        Mute, UnMute, VideCall, AudioCall, CallOut, More
    }

    private fun SceytInfoPageLayoutButtonsDirectChannelBinding.setupStyle() {
        setTextViewsDrawableColor(listOf(video, audio, muteUnMute, more),
            requireContext().getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoButtonsDirectChatFragment {
            val fragment = InfoButtonsDirectChatFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}