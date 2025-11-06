package com.sceyt.chatuikit.styles.extensions.channel_list

import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytChannelListEmptyStateBinding
import com.sceyt.chatuikit.databinding.SceytPageLoadingStateBinding
import com.sceyt.chatuikit.databinding.SceytSearchChannelsEmptyStateBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.presentation.custom_views.PageStateView

internal fun ChannelListView.setPageStateViews() {
    with(getPageStateView()) {
        setEmptyState(style.emptyState)
        setEmptySearchResultState(style.emptySearchState)
        setLoadingState(style.loadingState)
    }
}

private fun PageStateView.setEmptyState(
        @LayoutRes layoutResId: Int
) {
    if (layoutResId == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle() }.root
        )
    } else {
        setEmptyStateView(layoutResId)
    }
}

private fun PageStateView.setEmptySearchResultState(
        @LayoutRes layoutResId: Int
) {
    if (layoutResId == R.layout.sceyt_search_channels_empty_state) {
        setEmptySearchStateView(
            SceytSearchChannelsEmptyStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle() }.root
        )
    } else {
        setEmptySearchStateView(layoutResId)
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

private fun SceytChannelListEmptyStateBinding.applyStyle() {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    tvTitle.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    tvDescription.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
}


private fun SceytSearchChannelsEmptyStateBinding.applyStyle() {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    tvTitle.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    tvDescription.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
}

private fun SceytPageLoadingStateBinding.applyStyle() {
    progressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
}
