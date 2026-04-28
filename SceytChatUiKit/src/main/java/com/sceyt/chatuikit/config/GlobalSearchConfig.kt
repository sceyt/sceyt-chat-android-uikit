package com.sceyt.chatuikit.config

enum class GlobalSearchCloseBehavior {
    /** Never auto-close GlobalSearchActivity. */
    Never,

    /** Close when user navigates to a channel (channel click, message click, media/voice attachment click). */
    OnChannelOpen,

    /** Close when an outgoing message is detected on any channel. */
    OnMessageSent
}

class GlobalSearchConfig {
    var userSuggestionsLimit: Int = 8
    var searchInputDebounceMs: Long = 300
    var closeBehavior: GlobalSearchCloseBehavior = GlobalSearchCloseBehavior.OnMessageSent
}