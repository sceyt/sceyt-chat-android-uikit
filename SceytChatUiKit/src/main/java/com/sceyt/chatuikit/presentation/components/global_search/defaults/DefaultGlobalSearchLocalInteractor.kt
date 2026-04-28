package com.sceyt.chatuikit.presentation.components.global_search.defaults

import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import org.koin.core.component.inject

object DefaultGlobalSearchLocalInteractor : SceytKoinComponent {
    private val dataSource by inject<GlobalSearchDataSource>()

    operator fun invoke() = dataSource
}