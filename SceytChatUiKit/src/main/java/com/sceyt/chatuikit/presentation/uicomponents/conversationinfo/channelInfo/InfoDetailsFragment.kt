package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.channelInfo

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
import com.sceyt.chatuikit.databinding.SceytFragmentInfoDetailsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getDefaultAvatar
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoStyleApplier
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytstyles.ConversationInfoStyle
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

open class InfoDetailsFragment : Fragment(), ChannelUpdateListener, ConversationInfoStyleApplier {
    protected lateinit var binding: SceytFragmentInfoDetailsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ConversationInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var isSelf: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoDetailsBinding.inflate(layoutInflater, container, false)
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
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
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

                ChannelTypeEnum.Private, ChannelTypeEnum.Group -> {
                    val memberCount = channel.memberCount
                    if (memberCount > 1)
                        getString(R.string.sceyt_members_count, memberCount)
                    else getString(R.string.sceyt_member_count, memberCount)
                }

                ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> {
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
            if (isSelf) {
                avatar.setImageUrl(null, channel.getDefaultAvatar())
                avatar.setAvatarColor(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
            } else avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl, channel.getDefaultAvatar())
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        buttonsListener?.invoke(ClickActionsEnum.Avatar)
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        if (isSelf) return
        val user = presenceUser.user
        val userName = SceytChatUIKit.userNameFormatter?.format(user)
                ?: user.getPresentableName()
        binding.avatar.setNameAndImageUrl(userName, user.avatarURL, channel.getDefaultAvatar())
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

    override fun setStyle(style: ConversationInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentInfoDetailsBinding.applyStyle() {
        val theme = SceytChatUIKit.theme
        layoutDetails.setBackgroundColor(requireContext().getCompatColor(theme.backgroundColorSections))
        title.setTextColor(requireContext().getCompatColor(theme.textPrimaryColor))
        tvSubtitle.setTextColor(requireContext().getCompatColor(theme.textSecondaryColor))
        dividerTop.setDividerColorResource(theme.borderColor)
        space.layoutParams.height = style.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoDetailsFragment {
            val fragment = InfoDetailsFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}