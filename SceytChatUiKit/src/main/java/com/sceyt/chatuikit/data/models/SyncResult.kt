package com.sceyt.chatuikit.data.models

import com.sceyt.chat.models.SceytException

sealed class SyncResult<out T> {
    data object SuccessfullyFinished : SyncResult<Nothing>()
    data class Proportion<out T>(val items: List<T>) : SyncResult<T>()
    data class Error(val error: SceytException?) : SyncResult<Nothing>()
}
