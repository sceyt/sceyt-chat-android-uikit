package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.inlinequery

interface InlineQueryChangedListener {
    fun onQueryChanged(inlineQuery: InlineQuery)
    fun clearQuery() = onQueryChanged(InlineQuery.NoQuery)
}
