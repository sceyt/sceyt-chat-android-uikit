package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.extensions.setBundleArguments

open class VoiceSearchFragment : GlobalSearchListTabFragment() {
    final override val tab: GlobalSearchTab = GlobalSearchTab.Voice

    companion object {
        fun newInstance(
            styleId: String,
            sessionId: String,
        ) = VoiceSearchFragment().setBundleArguments {
            putString(GlobalSearchActivity.STYLE_ID_KEY, styleId)
            putString(GlobalSearchActivity.SESSION_ID_KEY, sessionId)
        }
    }
}
