package com.sceyt.chatuikit.presentation.components.channel_info.uri

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
import com.sceyt.chatuikit.databinding.SceytFragmentChannelInfoUriBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoStyleApplier
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelUpdateListener
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.ChannelInfoStyle

open class ChannelInfoURIFragment : Fragment(), ChannelUpdateListener, ChannelInfoStyleApplier {
    protected lateinit var binding: SceytFragmentChannelInfoUriBinding
        private set
    protected lateinit var channel: SceytChannel
        private set
    protected lateinit var style: ChannelInfoStyle
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentChannelInfoUriBinding.inflate(layoutInflater, container, false)
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
        channel = requireNotNull(arguments?.parcelable(ChannelInfoLinksFragment.CHANNEL))
    }

    private fun SceytFragmentChannelInfoUriBinding.initViews() {
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

    override fun setStyle(style: ChannelInfoStyle) {
        this.style = style
    }

    private fun SceytFragmentChannelInfoUriBinding.applyStyle() {
        link.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections))
        link.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
        space.layoutParams.height = style.spaceBetweenSections
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): ChannelInfoURIFragment {
            val fragment = ChannelInfoURIFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}