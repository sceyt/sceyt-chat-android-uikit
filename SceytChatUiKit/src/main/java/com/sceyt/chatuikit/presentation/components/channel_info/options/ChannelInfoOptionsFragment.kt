package com.sceyt.chatuikit.presentation.components.channel_info.options

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
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoOptionsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.ChannelInfoStyle

open class ChannelInfoOptionsFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoOptionsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ChannelInfoStyle
        private set
    private var buttonsListener: ((ClickActionsEnum) -> Unit)? = null
    private var enableSearchMessages: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoOptionsBinding.inflate(layoutInflater, container, false)
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
        channel = requireNotNull(arguments?.parcelable(ChannelInfoLinksFragment.CHANNEL))
        enableSearchMessages = arguments?.getBoolean(ENABLE_SEARCH_MESSAGES) ?: false
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
            val isOwnerOrAdmin = myRole == RoleTypeEnum.Owner.value || myRole == RoleTypeEnum.Admin.value

            if (channel.isDirect() || (channel.isPublic() && !isOwnerOrAdmin)) {
                groupChannelAdmins.isVisible = false
                groupChannelMembers.isVisible = false

                if (!enableSearchMessages)
                    root.isVisible = false

                return
            }

            searchMessages.isVisible = enableSearchMessages

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

    private fun SceytFragmentChannelInfoOptionsBinding.applyStyle() {
        val textPrimaryColor = requireContext().getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
        val backgroundColorSections = requireContext().getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
        members.setTextColor(textPrimaryColor)
        members.setBackgroundColor(backgroundColorSections)
        admins.setTextColor(textPrimaryColor)
        admins.setBackgroundColor(backgroundColorSections)
        searchMessages.setTextColor(textPrimaryColor)
        searchMessages.setBackgroundColor(backgroundColorSections)
        borderBetweenMembersAndAdmins.setDividerColorResource(SceytChatUIKit.theme.colors.borderColor)
        borderBetweenAdminsAndSearch.setDividerColorResource(SceytChatUIKit.theme.colors.borderColor)
        space.layoutParams.height = style.spaceBetweenSections
    }

    companion object {
        private const val ENABLE_SEARCH_MESSAGES = "ENABLE_SEARCH_MESSAGES"
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel, enableSearchMessages: Boolean): ChannelInfoOptionsFragment {
            val fragment = ChannelInfoOptionsFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
                putBoolean(ENABLE_SEARCH_MESSAGES, enableSearchMessages)
            }
            return fragment
        }
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setDetails(channel)
    }

    override fun setStyle(style: ChannelInfoStyle) {
        this.style = style
    }
}