package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchUserSuggestionsProvider
import org.koin.core.component.inject

open class DefaultUserSuggestionsProvider : GlobalSearchUserSuggestionsProvider, SceytKoinComponent {
    private val dataSource by inject<GlobalSearchDataSource>()

    override suspend fun provideSuggestions(
        query: String,
        limit: Int
    ): List<SceytUser> = dataSource.searchUsersLinkedToJoinedChannelsByDisplayName(
        query = query,
        limit = limit
    )
}