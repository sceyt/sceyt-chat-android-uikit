package com.sceyt.chatuikit.styles

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytChannelListEmptyStateBinding
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.presentation.custom_views.PageStateView

internal fun PageStateView.setChannelListViewStates(
        style: ChannelListViewStyle
) {
    setChannelListViewEmptyState(style)
}

private fun PageStateView.setChannelListViewEmptyState(
        style: ChannelListViewStyle
) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        SceytChannelListEmptyStateBinding.inflate(
            /* inflater = */ layoutInflater,
            /* parent = */ this,
            /* attachToParent = */ false
        ).applyStyle().root
    } else {
        setLoadingStateView(style.emptyState)
    }

    if (style.loadingState==R.layout.sceyt_channels_page_loading_state){

    }
}

private fun SceytChannelListEmptyStateBinding.applyStyle(): SceytChannelListEmptyStateBinding {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    tvTitle.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    tvDescription.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
    return this
}
