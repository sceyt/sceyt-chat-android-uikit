package com.sceyt.sceytchatuikit.presentation.root

import com.sceyt.sceytchatuikit.data.models.SceytResponse

sealed class PageState {
    data class StateError(val response: SceytResponse<*>?, val query: String?, val wasLoadingMore: Boolean) : PageState() {
        val errorMessage = response?.message
        val code = response?.code
    }

    data class StateEmpty(val query: String? = null, val wasLoadingMore: Boolean = false) : PageState() {
        val isSearch get() = !query.isNullOrBlank()
    }

    data class StateLoading(val isLoading: Boolean = true) : PageState()
    data class StateLoadingMore(val isLoading: Boolean = true) : PageState()
    object Nothing : PageState()
}
