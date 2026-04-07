package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.extensions.setBundleArguments

open class ChatsSearchFragment : GlobalSearchListTabFragment() {
    final override val tab: GlobalSearchTab = GlobalSearchTab.Chats

    companion object {
        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = ChatsSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}
