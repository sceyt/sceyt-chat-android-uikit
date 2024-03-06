package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.channelInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytFragmentInfoDetailsBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.getDefaultAvatar
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.ConversationInfoMediaStyle
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.Date

open class InfoDetailsFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoDetailsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null


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
        binding.setupStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
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
            if (channel.isPeerDeleted()) {
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
            title.text = if (channel.isPeerDeleted()) {
                getString(R.string.sceyt_deleted_user)
            } else channel.channelSubject
        }
    }

    open fun setChannelAvatar(channel: SceytChannel) {
        with(binding) {
            avatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl, channel.getDefaultAvatar())
        }
    }

    open fun onAvatarClick(channel: SceytChannel) {
        buttonsListener?.invoke(ClickActionsEnum.Avatar)
    }

    open fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        val user = presenceUser.user
        val userName = SceytKitConfig.userNameBuilder?.invoke(user)
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
        setChannelDetails(channel)
    }

    private fun SceytFragmentInfoDetailsBinding.setupStyle() {
        divider.layoutParams.height = ConversationInfoMediaStyle.dividerHeight
        divider.setBackgroundColor(requireContext().getCompatColor(ConversationInfoMediaStyle.dividerColor))
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