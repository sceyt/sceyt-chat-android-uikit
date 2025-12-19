package com.sceyt.chatuikit.styles.extensions.channel_info.voice

import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytEmptyStateBinding
import com.sceyt.chatuikit.databinding.SceytPageLoadingStateBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.channel_info.voice.ChannelInfoVoiceStyle
import com.sceyt.chatuikit.styles.extensions.common.applyStyle

internal fun PageStateView.setPageStatesView(
        style: ChannelInfoVoiceStyle,
) {
    setEmptyState(style)
    setLoadingState(style.loadingState)
}

private fun PageStateView.setEmptyState(
        style: ChannelInfoVoiceStyle,
) {
    if (style.emptyState == R.layout.sceyt_empty_state) {
        setEmptyStateView(
            SceytEmptyStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
    }
}


private fun PageStateView.setLoadingState(
        @LayoutRes layoutResId: Int,
) {
    if (layoutResId == R.layout.sceyt_page_loading_state) {
        setLoadingStateView(
            SceytPageLoadingStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle() }.root
        )
    } else {
        setLoadingStateView(layoutResId)
    }
}

private fun SceytPageLoadingStateBinding.applyStyle() {
    progressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
}