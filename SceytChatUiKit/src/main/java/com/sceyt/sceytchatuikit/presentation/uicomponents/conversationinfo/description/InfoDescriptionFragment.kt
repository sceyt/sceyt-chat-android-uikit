package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.description

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytFragmentInfoDescriptionBinding
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.setBundleArguments
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment

open class InfoDescriptionFragment : Fragment() {
    protected lateinit var binding: SceytFragmentInfoDescriptionBinding
        private set
    protected lateinit var channel: SceytChannel
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentInfoDescriptionBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        setChannelDescription(channel)
    }

    private fun getBundleArguments() {
        channel = requireNotNull(arguments?.parcelable(ChannelLinksFragment.CHANNEL))
    }

    open fun setChannelDescription(channel: SceytChannel) {
        with(binding) {
            if (channel.isDirect()) {
                val status = channel.getFirstMember()?.user?.presence?.status
                        ?: com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.presenceStatusText
                if (status.isNotNullOrBlank()) {
                    tvTitle.text = getString(com.sceyt.sceytchatuikit.R.string.sceyt_about)
                    tvDescription.text = status
                } else binding.root.isVisible = false
            } else {
                /*todo need to set channel description
                if (channel.label.isNotNullOrBlank()) {
                     tvTitle.text = getString(R.string.sceyt_description)
                     tvDescription.text = channel.label
                     groupChannelDescription.isVisible = true
                 } else*/ binding.root.isVisible = false
            }
        }
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun newInstance(channel: SceytChannel): InfoDescriptionFragment {
            val fragment = InfoDescriptionFragment()
            fragment.setBundleArguments {
                putParcelable(CHANNEL, channel)
            }
            return fragment
        }
    }
}