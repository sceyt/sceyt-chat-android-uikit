package com.sceyt.chatuikit.presentation.components.channel.input.mention.query

interface InlineQueryChangedListener {
    fun onQueryChanged(inlineQuery: InlineQuery)
    fun clearQuery() = onQueryChanged(InlineQuery.NoQuery)
}
