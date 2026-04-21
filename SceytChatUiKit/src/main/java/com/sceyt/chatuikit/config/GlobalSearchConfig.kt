package com.sceyt.chatuikit.config

enum class GlobalSearchCloseBehavior {
    /** Never auto-close GlobalSearchActivity. */
    Never,

    /** Close when user navigates to a channel. */
    OnChannelClick,
}

class GlobalSearchConfig {
    var userSuggestionsLimit: Int = 8
    var searchInputDebounceMs: Long = 300
    var closeBehavior: GlobalSearchCloseBehavior = GlobalSearchCloseBehavior.OnChannelClick
}