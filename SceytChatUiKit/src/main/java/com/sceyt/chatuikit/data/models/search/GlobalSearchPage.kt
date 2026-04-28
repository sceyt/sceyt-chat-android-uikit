package com.sceyt.chatuikit.data.models.search

data class GlobalSearchPage<T>(
    val data: List<T>,
    val hasMore: Boolean,
) {
    companion object {
        fun <T> empty() = GlobalSearchPage<T>(emptyList(), false)
    }
}
