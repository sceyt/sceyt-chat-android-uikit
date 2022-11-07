package com.sceyt.sceytchatuikit.data.models

sealed class PaginationResponse<T> {

    /**
     * @param data is items from database.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in database or not, to load next page. */
    data class DBResponse<T>(
            val data: List<T>,
            val loadKey: Long,
            val offset: Int,
            val hasNext: Boolean = false,
            val hasPrev: Boolean = false,
            val loadType: LoadType = LoadType.LoadPrev
    ) : PaginationResponse<T>()

    /**
     * @param data is items from server.
     * @param dbData is items from database by offset, include elements from start.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in server or not, to load next page. */
    data class ServerResponse<T>(
            val data: SceytResponse<List<T>>,
            val dbData: List<T>,
            val offset: Int,
            val hasNext: Boolean = false,
    ) : PaginationResponse<T>()


    data class ServerResponse2<T>(
            val data: SceytResponse<List<T>>,
            val cashData: List<T>,
            val loadKey: Long,
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

fun PaginationResponse<*>.getLoadKey(): Long {
    return when (this) {
        is PaginationResponse.DBResponse -> loadKey
        is PaginationResponse.ServerResponse2 -> loadKey
        else -> 0L
    }
}

fun PaginationResponse<*>.getOffset(): Int {
    return when (this) {
        is PaginationResponse.DBResponse -> offset
        is PaginationResponse.ServerResponse2 -> offset
        else -> 0
    }
}