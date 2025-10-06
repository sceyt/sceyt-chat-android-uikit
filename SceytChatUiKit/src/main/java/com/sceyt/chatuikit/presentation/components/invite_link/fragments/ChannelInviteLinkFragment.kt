package com.sceyt.chatuikit.presentation.components.invite_link.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInviteLinkBinding
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.invite_link.ChannelInviteLinStyle

open class ChannelInviteLinkFragment : Fragment() {
    protected lateinit var binding: SceytFragmentChannelInviteLinkBinding
    protected lateinit var style: ChannelInviteLinStyle
    protected lateinit var channel: SceytChannel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            ChannelInviteLinStyle.Builder(context, null).build()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        binding = SceytFragmentChannelInviteLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        applyStyle()
        setLinkText("https://link.sceyt.com/abcdefg1234567")
    }

    protected open fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL_KEY))
    }

    protected fun initViews() = with(binding) {
        switchShowPrevMessages.setOnlyClickable()

        switchShowPrevMessages.setOnClickListener {
            switchShowPrevMessages.isChecked = !switchShowPrevMessages.isChecked
        }

        icCopyLink.setOnClickListener {
            context?.setClipboard(tvInviteLink.text.toString())
        }

        tvShare.setOnClickListener {

        }

        tvResetLink.setOnClickListener {

        }

        tvOpenQR.setOnClickListener {
            BottomSheetShareInviteQr.show(childFragmentManager)
        }
    }

    protected fun setLinkText(link: String) = with(binding) {
        tvInviteLink.text = link
    }

    protected fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
        icCopyLink.setImageDrawable(style.copyLinkIcon)

        // Set texts
        tvInviteLinkTitle.text = style.inviteLinkTitle
        switchShowPrevMessages.text = style.showPreviewMessagesTitle
        tvShowPrevMessagesDesc.text = style.showPreviewMessagesDescription
        tvShare.text = style.shareTitle
        tvResetLink.text = style.resetLinkTitle
        tvOpenQR.text = style.openQrTitle

        style.inviteLinkTitleTextStyle.apply(tvInviteLinkTitle)
        style.inviteLinkTextStyle.apply(tvInviteLink)
        style.showPreviewMessagesSwitchStyle.apply(switchShowPrevMessages)
        style.showPreviewMessagesSubtitleTextStyle.apply(tvShowPrevMessagesDesc)

        with(style.optionsTextStyle) {
            apply(tvShare)
            apply(tvResetLink)
            apply(tvOpenQR)
        }

        tvShare.setDrawableStart(style.shareIcon)
        tvResetLink.setDrawableStart(style.resetLinkIcon)
        tvOpenQR.setDrawableStart(style.openQrIcon)
    }

    companion object {
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"
        private const val CHANNEL_KEY = "CHANNEL"

        fun newInstance(
                styleId: String?,
                channel: SceytChannel,
        ) = ChannelInviteLinkFragment().setBundleArguments {
            putString(STYLE_ID_KEY, styleId)
            putParcelable(CHANNEL_KEY, channel)
        }
    }
}