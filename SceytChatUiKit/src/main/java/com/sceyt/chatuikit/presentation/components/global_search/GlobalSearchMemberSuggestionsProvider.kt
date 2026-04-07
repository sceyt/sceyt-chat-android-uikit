package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.data.models.messages.SceytUser

fun interface GlobalSearchMemberSuggestionsProvider {
    suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser>
}
