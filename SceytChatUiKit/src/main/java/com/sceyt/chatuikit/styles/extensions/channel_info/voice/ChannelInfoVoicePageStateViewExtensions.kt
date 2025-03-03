package com.sceyt.chatuikit.styles.extensions.channel_info.voice

import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytEmptyStateBinding
import com.sceyt.chatuikit.databinding.SceytPageLoadingStateBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.components.channel_info.voice.ChannelInfoVoiceFragment
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.channel_info.voice.ChannelInfoVoiceStyle

internal fun ChannelInfoVoiceFragment.setPageStatesView(
        pageStateView: PageStateView
) {
    with(pageStateView) {
        setEmptyState(infoStyle.voiceStyle)
        setLoadingState(infoStyle.voiceStyle.loadingState)
    }
}

private fun PageStateView.setEmptyState(
        style: ChannelInfoVoiceStyle
) {
    if (style.emptyState == R.layout.sceyt_empty_state) {
        setEmptyStateView(
            SceytEmptyStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle(style.emptyStateTitle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
    }
}


private fun PageStateView.setLoadingState(
        @LayoutRes layoutResId: Int
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

private fun SceytEmptyStateBinding.applyStyle(emptyStateTitle: String) {
    title.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    title.text = emptyStateTitle
}

private fun SceytPageLoadingStateBinding.applyStyle() {
    progressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
}
