package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.extensions.setBundleArguments

open class LinksSearchFragment : GlobalSearchListTabFragment() {
    final override val tab: GlobalSearchTab = GlobalSearchTab.Links

    companion object {
        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = LinksSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}
