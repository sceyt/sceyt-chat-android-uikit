package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.toolbar

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoToolbarBinding
import com.sceyt.chatuikit.extensions.changeAlphaWithAnimation
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setOnClickListenerDisableClickViewForWhile
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getDefaultAvatar
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ConversationInfoStyle
import com.sceyt.chatuikit.sceytstyles.UserStyle
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

open class InfoToolbarFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoToolbarBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var isSelf: Boolean = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoToolbarBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        setChannelDetails(channel)
        binding.initViews()
        binding.setupStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
        isSelf = channel.isSelf()
    }

    private fun SceytFragmentInfoToolbarBinding.initViews() {
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
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.toString() || myRole == RoleTypeEnum.Admin.toString()

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

    open fun setChannelToolbarTitle(channel: SceytChannel) {
        with(binding) {
            titleToolbar.text = when {
                isSelf -> {
                    getString(R.string.self_notes)
                }

                channel.isPeerDeleted() -> {
                    getString(R.string.sceyt_deleted_user)
                }

                else -> channel.channelSubject
            }
        }
    }

    open fun setToolbarSubtitle(channel: SceytChannel) {
        if (channel.isPeerDeleted() || isSelf) {
            binding.subTitleToolbar.isVisible = false
            return
        }
        val title: String = when (channel.getChannelType()) {
            ChannelTypeEnum.Direct -> {
                val member = channel.getPeer() ?: return
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
        binding.subTitleToolbar.text = title
    }

    open fun setChannelToolbarAvatar(channel: SceytChannel) {
        if (isSelf) {
            binding.toolbarAvatar.setAvatarColor(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
            binding.toolbarAvatar.setImageUrl(null, UserStyle.notesAvatar)
        }
        else
            binding.toolbarAvatar.setNameAndImageUrl(channel.channelSubject, channel.iconUrl, channel.getDefaultAvatar())
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelDetails(channel)
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    fun onUserPresenceUpdated(presenceUser: SceytPresenceChecker.PresenceUser) {
        if (isSelf) return
        val user = presenceUser.user
        val userName = SceytKitConfig.userNameBuilder?.invoke(user)
                ?: user.getPresentableName()
        binding.toolbarAvatar.setNameAndImageUrl(userName, user.avatarURL, UserStyle.userDefaultAvatar)
    }

    open fun onBackClick() {
        buttonsListener?.invoke(ClickActionsEnum.Back)
    }

    open fun onEditClick() {
        buttonsListener?.invoke(ClickActionsEnum.Edit)
    }

    open fun onMoreClick() {
        buttonsListener?.invoke(ClickActionsEnum.More)
    }

    enum class ClickActionsEnum {
        Back, Edit, More
    }

    private fun SceytFragmentInfoToolbarBinding.setupStyle() {
        icBack.setImageResource(ConversationInfoStyle.navigationIcon)
        icEdit.setImageResource(ConversationInfoStyle.editIcon)
        icMore.setImageResource(ConversationInfoStyle.moreIcon)
        icBack.imageTintList = ColorStateList.valueOf(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
        icEdit.imageTintList = ColorStateList.valueOf(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
        icMore.imageTintList = ColorStateList.valueOf(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoToolbarFragment {
            val fragment = InfoToolbarFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}