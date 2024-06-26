package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.specifications

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentInfoSpecificationsBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ConversationInfoStyleApplier
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.sceytstyles.ConversationInfoStyle

open class InfoSpecificationsFragment : Fragment(), ChannelUpdateListener, ConversationInfoStyleApplier {
    protected lateinit var binding: SceytFragmentInfoSpecificationsBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ConversationInfoStyle
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoSpecificationsBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        setChannelSpecification(channel)
        binding.applyStyle()
        binding.initViews()
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    private fun SceytFragmentInfoSpecificationsBinding.initViews() {
        link.setOnClickListener {
            onLinkClick(channel)
        }
    }

    @SuppressLint("SetTextI18n")
    open fun setChannelSpecification(channel: SceytChannel) {
        with(binding) {
            if (channel.isPublic()) {
                link.text = channel.uri
            } else binding.root.isVisible = false
        }
    }

    private fun onLinkClick(channel: SceytChannel) {
        val uri = channel.uri ?: return
        context?.setClipboard(uri)
        Toast.makeText(context, getString(R.string.sceyt_channel_uri_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        if (::binding.isInitialized.not()) return
        setChannelSpecification(channel)
    }

    override fun setStyle(style: ConversationInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentInfoSpecificationsBinding.applyStyle() {
        link.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
        link.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        space.layoutParams.height = style.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoSpecificationsFragment {
            val fragment = InfoSpecificationsFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}