package com.sceyt.chatuikit.styles.extensions.search

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytChannelListEmptyStateBinding
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.extensions.common.applyStyle
import com.sceyt.chatuikit.styles.search.ChannelsSearchPageStyle
import com.sceyt.chatuikit.styles.search.ChatsSearchPageStyle
import com.sceyt.chatuikit.styles.search.FilesSearchPageStyle
import com.sceyt.chatuikit.styles.search.LinksSearchPageStyle
import com.sceyt.chatuikit.styles.search.MediaSearchPageStyle
import com.sceyt.chatuikit.styles.search.VoiceSearchPageStyle

internal fun PageStateView.setPageStatesView(style: ChatsSearchPageStyle) {
    setEmptyState(style)
    setEmptySearchState(style)
}

internal fun PageStateView.setPageStatesView(style: MediaSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
        setEmptySearchStateView(style.emptyState)
    }
}

internal fun PageStateView.setPageStatesView(style: FilesSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
        setEmptySearchStateView(style.emptyState)
    }
}

internal fun PageStateView.setPageStatesView(style: LinksSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
        setEmptySearchStateView(style.emptyState)
    }
}

internal fun PageStateView.setPageStatesView(style: VoiceSearchPageStyle) {
    if (style.emptyState == R.layout.sceyt_channel_list_empty_state) {
        setEmptyStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
        setEmptySearchStateView(
            SceytChannelListEmptyStateBinding.inflate(layoutInflater, this, false)
                .also { it.applyStyle(style.emptyStateStyle) }.root
        )
    } else {
        setEmptyStateView(style.emptyState)
        setEmptySearchStateView(style.emptyState)
    }
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