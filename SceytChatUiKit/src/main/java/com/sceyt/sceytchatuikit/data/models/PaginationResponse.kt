package com.sceyt.sceytchatuikit.data.models

sealed class PaginationResponse<T> {

    /**
     * @param data is items from database.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in database or not, to load next page.
     * */
    data class DBResponse<T>(
            val data: List<T>,
            val loadKey: LoadKeyData?,
            val offset: Int,
            val hasNext: Boolean = false,
            val hasPrev: Boolean = false,
            val loadType: LoadType = LoadType.LoadNext
    ) : PaginationResponse<T>()

    /**
     * @param data is items from server.
     * @param cashData is items from database or from cash, include elements from start.
     * @param loadKey is the the helper key, which has been set when request started.
     * @param offset is the offset, which has been set when request started.
     * @param hasDiff is the difference between database/cash items and server items.
     * @param hasNext shows, are items in server/database or not, to load next page.
     * @param hasPrev shows, are items in server/database or not, to load prev page.
     * @param loadType is pointed which type of request is was current request.
     * @param ignoredDb shows was loaded items from database or not, before server request is received.
     * */
    data class ServerResponse<T>(
            val data: SceytResponse<List<T>>,
            val cashData: List<T>,
            val loadKey: LoadKeyData?,
            val offset: Int,
            val hasDiff: Boolean,
            val hasNext: Boolean,
            val hasPrev: Boolean,
            val loadType: LoadType,
            val ignoredDb: Boolean
    ) : PaginationResponse<T>()


    /** Default value */
    class Nothing<T> : PaginationResponse<T>()

    enum class LoadType {
        LoadPrev, LoadNext, LoadNear, LoadNewest
    }
}

data class LoadKeyData(
        val key: Long = -1,
        val value: Long = -1
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
