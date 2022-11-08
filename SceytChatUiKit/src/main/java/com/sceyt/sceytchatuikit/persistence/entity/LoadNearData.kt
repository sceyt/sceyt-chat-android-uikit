package com.sceyt.sceytchatuikit.persistence.entity

data class LoadNearData<T>(
        var data: List<T>,
        val hasNext: Boolean,
        val hasPrev: Boolean
)