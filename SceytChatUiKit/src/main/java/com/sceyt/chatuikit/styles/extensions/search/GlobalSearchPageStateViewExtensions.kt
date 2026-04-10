package com.sceyt.chatuikit.styles.extensions.search

import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytChannelListEmptyStateBinding
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.search.ChannelsSearchPageStyle
import com.sceyt.chatuikit.styles.search.ChatsSearchPageStyle

internal fun PageStateView.setPageStatesView(style: ChatsSearchPageStyle) {
    setEmptyState(style)
    setEmptySearchState(style)
}

internal fun PageStateView.setPageStatesView(style: ChannelsSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        val view = SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
            .also { it.applyStyle(style.emptyStateStyle) }.root
        setEmptyStateView(view)
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
        setEmptySearchStateView(style.emptyState)
    }
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
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptySearchStateView(style.emptyState)
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