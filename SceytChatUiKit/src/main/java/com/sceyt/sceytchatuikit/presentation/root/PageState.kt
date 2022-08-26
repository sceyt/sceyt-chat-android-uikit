package com.sceyt.sceytchatuikit.presentation.root

sealed class PageState {
    data class StateError(val errorMessage: String?, val query: String?, val wasLoadingMore: Boolean) : PageState()

    data class StateEmpty(val query: String? = null, val wasLoadingMore: Boolean = false) : PageState() {
        val isSearch get() = !query.isNullOrBlank()
    }

    data class StateLoading(val isLoading: Boolean = true) : PageState()
    data class StateLoadingMore(val isLoading: Boolean = true) : PageState()
    object Nothing : PageState()
}
