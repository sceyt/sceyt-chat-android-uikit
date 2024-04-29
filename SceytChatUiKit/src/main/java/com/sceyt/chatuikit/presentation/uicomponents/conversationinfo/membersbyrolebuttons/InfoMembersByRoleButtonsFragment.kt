package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.membersbyrolebuttons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoMembersByRoleBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment

open class InfoMembersByRoleButtonsFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoMembersByRoleBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoMembersByRoleBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViews()
        setDetails(channel)
        binding.applyStyle()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun initViews() {
        binding.members.setOnClickListener {
            buttonsListener?.invoke(ClickActionsEnum.Members)
        }

        binding.admins.setOnClickListener {
            buttonsListener?.invoke(ClickActionsEnum.Admins)
        }

        binding.searchMessages.setOnClickListener {
            buttonsListener?.invoke(ClickActionsEnum.SearchMessages)
        }
    }

    open fun setDetails(channel: SceytChannel) {
        with(binding) {
            val myRole = channel.userRole
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.toString() || myRole == RoleTypeEnum.Admin.toString()

            if (channel.isDirect() || (channel.isPublic() && !isOwnerOrAdmin)) {
                groupChannelAdmins.isVisible = false
                groupChannelMembers.isVisible = false
                return
            }

            members.text = if (channel.isPublic())
                getString(R.string.sceyt_subscribers) else getString(R.string.sceyt_members)

            groupChannelAdmins.isVisible = isOwnerOrAdmin
        }
    }

    fun setClickActionsListener(listener: (ClickActionsEnum) -> Unit) {
        buttonsListener = listener
    }

    enum class ClickActionsEnum {
        Members, Admins, SearchMessages
    }

    private fun SceytFragmentInfoMembersByRoleBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        members.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        admins.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        searchMessages.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        borderBetweenMembersAndAdmins.setDividerColorResource(SceytChatUIKit.theme.borderColor)
        borderBetweenAdminsAndSearch.setDividerColorResource(SceytChatUIKit.theme.borderColor)
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoMembersByRoleButtonsFragment {
            val fragment = InfoMembersByRoleButtonsFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setDetails(channel)
    }
}