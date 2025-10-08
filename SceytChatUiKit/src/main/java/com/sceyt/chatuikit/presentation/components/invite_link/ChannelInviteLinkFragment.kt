package com.sceyt.chatuikit.presentation.components.invite_link

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInviteLinkBinding
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.invite_link.shareqr.BottomSheetShareInviteQR
import com.sceyt.chatuikit.presentation.components.invite_link.shareqr.LinkQrData
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.invite_link.ChannelInviteLinkStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class ChannelInviteLinkFragment : Fragment(), SceytKoinComponent {
    protected lateinit var binding: SceytFragmentChannelInviteLinkBinding
    protected lateinit var style: ChannelInviteLinkStyle
    protected lateinit var channel: SceytChannel
    protected val viewModel: ChannelInviteLinkViewModel by viewModel {
        parametersOf(channel.id)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            ChannelInviteLinkStyle.Builder(context, null).build()
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

        applyStyle()
        getBundleArguments()
        initViewModel()
        initViews()
    }

    protected open fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(CHANNEL_KEY))
    }

    private fun initViewModel() {
        viewModel.uiState.onEach {
            setLinkDetails(
                link = it.inviteLink.orEmpty(),
                showPrevMessagesAllowed = it.showPreviousMessages
            )
            if (it.isLoading)
                SceytLoader.showLoading(requireContext())
            else SceytLoader.hideLoading()
        }.launchIn(lifecycleScope)
    }

    protected fun initViews() = with(binding) {
        switchShowPrevMessages.setOnlyClickable()

        switchShowPrevMessages.setOnClickListener {
            onSwitchShowPrevMessagesClick()
        }

        icCopyLink.setOnClickListener {
            onCopyLinkClick()
        }

        tvShare.setOnClickListener {
            onShareClick()
        }

        tvResetLink.setOnClickListener {
            onResetLinkClick()
        }

        tvOpenQR.setOnClickListener {
            onOpenQrClick()
        }
    }

    protected fun setLinkDetails(
            link: String,
            showPrevMessagesAllowed: Boolean,
    ) = with(binding) {
        tvInviteLink.text = link
        switchShowPrevMessages.isChecked = showPrevMessagesAllowed
    }

    protected open fun onResetLinkClick() {
        SceytDialog.showDialog(
            context = requireContext(),
            title = getString(R.string.sceyt_reset_link),
            description = getString(R.string.reset_link_desc),
            positiveBtnTitle = getString(R.string.reset),
            positiveCb = {
                viewModel.resetInviteLink()
            }
        )
    }

    protected open fun onSwitchShowPrevMessagesClick() {
        viewModel.toggleShowPreviousMessages()
    }

    protected open fun onCopyLinkClick() {
        context?.setClipboard(binding.tvInviteLink.text.toString())
    }

    protected open fun onShareClick() {
        viewModel.shareInviteLink(requireActivity())
    }

    protected open fun onOpenQrClick() {
        BottomSheetShareInviteQR.Companion.show(
            fragmentManager = childFragmentManager,
            linkQrData = LinkQrData(link = linkUrl)
        )
    }

    protected open val linkUrl: String
        get() = SceytChatUIKit.config.channelDeepLinkDomain + "join/" + channel.uri

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

        style.linkPreviewBackgroundStyle.apply(layoutInviteLink)
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