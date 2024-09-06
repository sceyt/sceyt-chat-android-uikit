package com.sceyt.chatuikit.presentation.uicomponents.locationpreview.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytFragmentLocationPreviewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBackgroundTint

open class LocationPreviewFragment : Fragment() {
    protected var binding: SceytFragmentLocationPreviewBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentLocationPreviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.applyStyle()
        initViews()
    }

    private fun initViews() {

    }

    private fun SceytFragmentLocationPreviewBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.primaryColor))
        tvLocation.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
        btnShareLocation.setBackgroundTint(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
        btnShareLocation.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.onPrimaryColor))
    }

}