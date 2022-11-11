package com.sceyt.sceytchatuikit.data.models

data class LoadNearData<T>(
        var data: List<T>,
        val hasNext: Boolean,
        val hasPrev: Boolean
)