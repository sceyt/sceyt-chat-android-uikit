package com.sceyt.chatuikit.presentation.root


sealed class PageState {
    data class StateError(val code: Int?,
                          val errorMessage: String?,
                          val wasLoadingMore: Boolean = false,
                          val query: String? = null,
                          val showMessage: Boolean = true) : PageState()

    data class StateEmpty(val query: String? = null,
                          val wasLoadingMore: Boolean = false) : PageState() {
        val isSearch get() = !query.isNullOrBlank()
    }

    data class StateLoading(val isLoading: Boolean = true) : PageState()
    data class StateLoadingMore(val isLoading: Boolean = true) : PageState()
    data object Nothing : PageState()
}
