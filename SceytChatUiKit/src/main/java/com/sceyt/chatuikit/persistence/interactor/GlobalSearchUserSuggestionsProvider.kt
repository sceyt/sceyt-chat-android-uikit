package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.messages.SceytUser

fun interface GlobalSearchUserSuggestionsProvider {
    suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser>
}
