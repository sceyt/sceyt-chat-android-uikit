package com.sceyt.chatuikit.styles.extensions.messages_list

import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytMessagesEmptyStateBinding
import com.sceyt.chatuikit.databinding.SceytMessagesEmptyStateSelfChannelBinding
import com.sceyt.chatuikit.databinding.SceytPageLoadingStateBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.custom_views.PageStateView

internal fun MessagesListView.setPageStateViews() {
    with(getPageStateView()) {
        setEmptyState(style.emptyState)
        setLoadingState(style.loadingState)
    }
}

internal fun MessagesListView.setEmptyStateForSelfChannel() {
    getPageStateView().setEmptyStateForSelfChannels(style.emptyStateForSelfChannel)
}

private fun PageStateView.setEmptyState(
        @LayoutRes layoutResId: Int
) {
    if (layoutResId == R.layout.sceyt_messages_empty_state) {
        setEmptyStateView(
            SceytMessagesEmptyStateBinding.inflate(
                layoutInflater, this, false
            ).also { it.applyStyle() }.root
        )
    } else {
        setEmptyStateView(layoutResId)
    }
}

private fun PageStateView.setEmptyStateForSelfChannels(
        @LayoutRes layoutResId: Int
) {
    if (layoutResId == R.layout.sceyt_messages_empty_state_self_channel) {
        setEmptySearchStateView(
            SceytMessagesEmptyStateSelfChannelBinding.inflate(
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

private fun SceytMessagesEmptyStateBinding.applyStyle() {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    title.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    description.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
}


private fun SceytMessagesEmptyStateSelfChannelBinding.applyStyle() {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    title.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    description.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
}

private fun SceytPageLoadingStateBinding.applyStyle() {
    progressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
}
