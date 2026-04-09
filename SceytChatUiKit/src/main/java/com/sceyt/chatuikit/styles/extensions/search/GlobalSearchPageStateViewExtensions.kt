package com.sceyt.chatuikit.styles.extensions.search

import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytChannelListEmptyStateBinding
import com.sceyt.chatuikit.databinding.SceytPageLoadingStateBinding
import com.sceyt.chatuikit.extensions.setProgressColorRes
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.search.ChatsSearchPageStyle

internal fun PageStateView.setPageStatesView(style: ChatsSearchPageStyle) {
    setEmptyState(style)
    setEmptySearchState(style)
    setLoadingState(style.loadingState)
}

private fun PageStateView.setEmptyState(style: ChatsSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
    }
}

private fun PageStateView.setEmptySearchState(style: ChatsSearchPageStyle) {
    if (style.emptySearchState == R.layout.sceyt_channel_list_empty_state) {
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptySearchStateView(style.emptySearchState)
    }
}

private fun PageStateView.setLoadingState(@LayoutRes layoutResId: Int) {
    if (layoutResId == R.layout.sceyt_page_loading_state) {
        setLoadingStateView(
            SceytPageLoadingStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle() }.root
        )
    } else {
        setLoadingStateView(layoutResId)
    }
}

private fun SceytChannelListEmptyStateBinding.applyStyle(style: EmptyStateStyle) {
    image.isVisible = style.icon != null
    if (style.icon != null) {
        image.setImageDrawable(style.icon)
        if (style.iconTint != UNSET_COLOR) {
            image.setColorFilter(style.iconTint)
        } else {
            image.clearColorFilter()
        }
    }
    tvTitle.text = style.titleText
    style.titleStyle.apply(tvTitle)
    tvDescription.isVisible = !style.subtitleText.isNullOrEmpty()
    if (!style.subtitleText.isNullOrEmpty()) {
        tvDescription.text = style.subtitleText
        style.subtitleStyle.apply(tvDescription)
    }
}

private fun SceytPageLoadingStateBinding.applyStyle() {
    progressBar.setProgressColorRes(SceytChatUIKit.theme.colors.accentColor)
}
