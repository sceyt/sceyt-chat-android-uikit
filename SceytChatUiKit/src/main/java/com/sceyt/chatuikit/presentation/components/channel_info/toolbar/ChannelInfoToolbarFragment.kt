package com.sceyt.chatuikit.presentation.components.channel_info.toolbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoToolbarBinding
import com.sceyt.chatuikit.extensions.changeAlphaWithAnimation
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setOnClickListenerDisableClickViewForWhile
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoStyle
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoToolBarStyle

open class ChannelInfoToolbarFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoToolbarBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var infoStyle: ChannelInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var isSelf: Boolean = false

    protected val style: ChannelInfoToolBarStyle
        get() = infoStyle.toolBarStyle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoToolbarBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        setChannelDetails(channel)
        binding.initViews()
        binding.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelInfoLinksFragment.CHANNEL))
        isSelf = channel.isSelf
    }

    private fun SceytFragmentChannelInfoToolbarBinding.initViews() {
        icBack.setOnClickListener {
            buttonsListener?.invoke(ClickActionsEnum.Back)
        }

        icEdit.setOnClickListenerDisableClickViewForWhile {
            onEditClick()
        }

        icMore.setOnClickListenerDisableClickViewForWhile {
            onMoreClick()
        }
    }

    open fun setChannelDetails(channel: SceytChannel) {
        with(binding) {
            val myRole = channel.userRole
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.value || myRole == RoleTypeEnum.Admin.value

            icEdit.isVisible = !channel.isDirect() && isOwnerOrAdmin
            icMore.isVisible = channel.checkIsMemberInChannel()

            setChannelToolbarTitle(channel)
            setToolbarSubtitle(channel)
            setChannelToolbarAvatar(channel)
        }
    }

    open fun toggleToolbarViews(showDetails: Boolean) {
        val toolbarLayout = binding.layoutToolbar
        if (showDetails == toolbarLayout.isVisible) return
        val to = if (showDetails) 1f else 0f
        if (showDetails) toolbarLayout.isVisible = true
        binding.tvToolbarInfo.isVisible = !showDetails
        toolbarLayout.changeAlphaWithAnimation(toolbarLayout.alpha, to, 100) {
            if (showDetails.not()) {
                toolbarLayout.isInvisible = true
            }
        }
    }

    protected open fun setChannelToolbarTitle(channel: SceytChannel) {
        with(binding) {
            titleToolbar.text = when {
                isSelf -> {
                    getString(R.string.sceyt_self_notes)
                }

                channel.isPeerDeleted() -> {
                    getString(R.string.sceyt_deleted_user)
                }

                else -> {
                    if (channel.isGroup)
                        channel.subject.orEmpty()
                    else channel.getPeer()?.run {
                        SceytChatUIKit.formatters.userNameFormatter.format(requireContext(), user)
                    } ?: ""
                }
            }
        }
    }

    protected open fun setToolbarSubtitle(channel: SceytChannel) {
        if (channel.isPeerDeleted() || isSelf) {
            binding.subTitleToolbar.isVisible = false
            return
        }
        val title = SceytChatUIKit.formatters.channelSubtitleFormatter.format(requireContext(), channel)
        binding.subTitleToolbar.text = title
    }

    protected open fun setChannelToolbarAvatar(channel: SceytChannel) {
        style.channelAvatarRenderer.render(requireContext(), channel, style.avatarStyle, binding.toolbarAvatar)
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDetails(channel)
    }

    override fun setStyle(style: ChannelInfoStyle) {
        this.infoStyle = style
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        if (isSelf) return
        setChannelToolbarAvatar(channel)
    }

    protected open fun onBackClick() {
        buttonsListener?.invoke(ClickActionsEnum.Back)
    }

    protected open fun onEditClick() {
        buttonsListener?.invoke(ClickActionsEnum.Edit)
    }

    protected open fun onMoreClick() {
        buttonsListener?.invoke(ClickActionsEnum.More)
    }

    enum class ClickActionsEnum {
        Back, Edit, More
    }

    private fun SceytFragmentChannelInfoToolbarBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        titleToolbar.text = style.expandedStateTitle
        style.expandedStateTitleTextStyle.apply(titleToolbar)
        style.collapsedStateTitleTextStyle.apply(tvToolbarInfo)
        style.collapsedStateSubtitleTextStyle.apply(subTitleToolbar)
        style.avatarStyle.apply(toolbarAvatar)
        icBack.setImageDrawable(style.navigationIcon)
        icEdit.setImageDrawable(style.editIcon)
        icMore.setImageDrawable(style.moreIcon)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelInfoToolbarFragment {
            val fragment = ChannelInfoToolbarFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}