package com.sceyt.chatuikit.data.models

sealed interface PaginationResponse<T> {

    /**
     * @param data is items from database.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in database or not, to load next page.
     * @param query is the search query, which has been set in request.
     * */
    data class DBResponse<T>(
        val data: List<T>,
        val loadKey: LoadKeyData?,
        val offset: Int,
        val hasNext: Boolean = false,
        val hasPrev: Boolean = false,
        val loadType: LoadType = LoadType.LoadNext,
        val query: String = "",
    ) : PaginationResponse<T>

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
     * @param query is the search query, which has been set in request.
     *
     * */
    data class ServerResponse<T>(
        val data: SceytResponse<List<T>>,
        val cacheData: List<T>,
        val loadKey: LoadKeyData?,
        val offset: Int,
        val hasDiff: Boolean,
        val hasNext: Boolean,
        val hasPrev: Boolean,
        val loadType: LoadType,
        val ignoredDb: Boolean,
        val dbResultWasEmpty: Boolean = false,
        val query: String = "",
        val nextToken: String = ""
    ) : PaginationResponse<T>


    /** Default value */
    class Nothing<T> : PaginationResponse<T>

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
        is PaginationResponse.Nothing<*> -> null
    }
}

fun PaginationResponse<*>.getOffset(): Int {
    return when (this) {
        is PaginationResponse.DBResponse -> offset
        is PaginationResponse.ServerResponse -> offset
        is PaginationResponse.Nothing<*> -> 0
    }
}
