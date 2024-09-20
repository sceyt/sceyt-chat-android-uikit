package com.sceyt.chatuikit.presentation.components.channel_info.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoDetailsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.ChannelInfoStyle
import java.util.Date

open class ChannelInfoDetailsFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoDetailsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ChannelInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var isSelf: Boolean = false

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
            val title: String = when (channel.getChannelType()) {
                ChannelTypeEnum.Direct -> {
                    val member = channel.getPeer() ?: return
                    if (member.user.blocked) {
                        ""
                    } else {
                        if (member.user.presence?.state == PresenceState.Online) {
                            getString(R.string.sceyt_online)
                        } else {
                            member.user.presence?.lastActiveAt?.let {
                                if (it != 0L)
                                    DateTimeUtil.getPresenceDateFormatData(requireContext(), Date(it))
                                else ""
                            } ?: ""
                        }
                    }
                }

                ChannelTypeEnum.Group -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_members_count, memberCount)
                    else getString(R.string.sceyt_member_count, memberCount)
                }

                ChannelTypeEnum.Public -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_subscribers_count, memberCount)
                    else getString(R.string.sceyt_subscriber_count, memberCount)
                }
            }
            tvSubtitle.text = title
        }
    }

    open fun setChannelTitle(channel: SceytChannel) {
        with(binding) {
            title.text = when {
                isSelf -> {
                    getString(R.string.sceyt_self_notes)
                }

                channel.isPeerDeleted() -> {
                    getString(R.string.sceyt_deleted_user)
                }

                else -> channel.channelSubject
            }
        }
    }

    open fun setChannelAvatar(channel: SceytChannel) {
        with(binding) {
            avatar.setChannelAvatar(channel, isSelf)
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        buttonsListener?.invoke(ClickActionsEnum.Avatar)
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        if (isSelf) return
        binding.avatar.setChannelAvatar(channel, isSelf)
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
        this.style = style
    }

    private fun SceytFragmentChannelInfoDetailsBinding.applyStyle() {
        val theme = SceytChatUIKit.theme
        layoutDetails.setBackgroundColor(requireContext().getCompatColor(theme.backgroundColorSections))
        title.setTextColor(requireContext().getCompatColor(theme.textPrimaryColor))
        tvSubtitle.setTextColor(requireContext().getCompatColor(theme.textSecondaryColor))
        dividerTop.setDividerColorResource(theme.borderColor)
        space.layoutParams.height = style.spaceBetweenSections
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