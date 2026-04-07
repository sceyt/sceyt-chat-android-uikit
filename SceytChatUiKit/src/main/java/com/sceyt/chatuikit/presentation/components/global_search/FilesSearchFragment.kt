package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.extensions.setBundleArguments

open class FilesSearchFragment : GlobalSearchListTabFragment() {
    final override val tab: GlobalSearchTab = GlobalSearchTab.Files

    companion object {
        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = FilesSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}
