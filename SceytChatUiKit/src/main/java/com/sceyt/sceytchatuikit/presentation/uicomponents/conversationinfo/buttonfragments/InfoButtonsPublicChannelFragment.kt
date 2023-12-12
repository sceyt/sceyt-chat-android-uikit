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
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.extensions.setDrawableTop
import com.sceyt.sceytchatuikit.extensions.setOnClickListenerDisableClickViewForWhile
import com.sceyt.sceytchatuikit.extensions.setTextViewsDrawableColor
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

open class InfoButtonsPublicChannelFragment : Fragment() {
    lateinit var binding: SceytInfoPageLayoutButtonsPublicChannelBinding
        private set
    private var buttonsListener: ((PublicChannelClickActionsEnum) -> Unit)? = null
    lateinit var channel: SceytChannel
        private set

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
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    open fun SceytInfoPageLayoutButtonsPublicChannelBinding.setOnClickListeners() {
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

    open fun determinateState() {
        val myRole = channel.userRole
        val isMember = myRole.isNotNullOrBlank()
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

            val enabledActions = myRole == RoleTypeEnum.Owner.toString() || myRole == RoleTypeEnum.Admin.toString()
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