package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.specifications

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytFragmentInfoSpecificationsBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.extensions.setClipboard
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelUpdateListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.sceytstyles.ConversationInfoMediaStyle

open class InfoSpecificationsFragment : Fragment(), ChannelUpdateListener {
    protected lateinit var binding: SceytFragmentInfoSpecificationsBinding
        private set
    protected lateinit var channel: SceytChannel
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
        binding.setupStyle()
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
                link.text = "@${channel.uri}"
            } else binding.root.isVisible = false
        }
    }

    private fun onLinkClick(channel: SceytChannel) {
        val uri = channel.uri?.removePrefix("@") ?: return
        context?.setClipboard(uri)
        Toast.makeText(context, getString(R.string.channel_uri_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onChannelUpdated(channel: SceytChannel) {
        setChannelSpecification(channel)
    }

    private fun SceytFragmentInfoSpecificationsBinding.setupStyle() {
        divider.layoutParams.height = ConversationInfoMediaStyle.dividerHeight
        divider.setBackgroundColor(requireContext().getCompatColor(ConversationInfoMediaStyle.dividerColor))
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