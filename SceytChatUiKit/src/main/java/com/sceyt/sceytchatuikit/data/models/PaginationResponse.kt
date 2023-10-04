package com.sceyt.sceytchatuikit.data.models

sealed class PaginationResponse<T> {

    /**
     * @param data is items from database.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in database or not, to load next page.
     * */
    data class DBResponse<T>(
            var data: List<T>,
            var loadKey: LoadKeyData?,
            var offset: Int,
            var hasNext: Boolean = false,
            var hasPrev: Boolean = false,
            var loadType: LoadType = LoadType.LoadNext
    ) : PaginationResponse<T>()

    /**
     * @param data is items from server.
     * @param cacheData is items from database or from cache, include elements from start.
     * @param loadKey is the the helper key, which has been set when request started.
     * @param offset is the offset, which has been set when request started.
     * @param hasDiff is the difference between database/cache items and server items.
     * @param hasNext shows, are items in server/database or not, to load next page.
     * @param hasPrev shows, are items in server/database or not, to load prev page.
     * @param loadType is pointed which type of request is was current request.
     * @param ignoredDb shows was loaded items from database or not, before server request is received.
     * */
    data class ServerResponse<T>(
            var data: SceytResponse<List<T>>,
            var cacheData: List<T>,
            var loadKey: LoadKeyData?,
            var offset: Int,
            var hasDiff: Boolean,
            var hasNext: Boolean,
            var hasPrev: Boolean,
            var loadType: LoadType,
            var ignoredDb: Boolean,
            var dbResultWasEmpty: Boolean = false
    ) : PaginationResponse<T>()


    /** Default value */
    class Nothing<T> : PaginationResponse<T>()

    enum class LoadType {
        LoadPrev, LoadNext, LoadNear, LoadNewest
    }
}

data class LoadKeyData(
        val key: Long = -1,
        val value: Long = -1,
        val data: Any? = null
)

fun PaginationResponse<*>.getLoadKey(): LoadKeyData? {
    return when (this) {
        is PaginationResponse.DBResponse -> loadKey
        is PaginationResponse.ServerResponse -> loadKey
        else -> null
    }
}

fun PaginationResponse<*>.getOffset(): Int {
    return when (this) {
        is PaginationResponse.DBResponse -> offset
        is PaginationResponse.ServerResponse -> offset
        else -> 0
    }
}
