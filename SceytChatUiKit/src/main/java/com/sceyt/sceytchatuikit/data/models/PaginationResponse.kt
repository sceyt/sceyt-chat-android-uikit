package com.sceyt.sceytchatuikit.data.models

sealed class PaginationResponse<T> {

    /**
     * @param data is items from database.
     * @param offset is the normalized offset, which has been set in in request.
     * @param hasNext shows, are items in database or not, to load next page. */
    data class DBResponse<T>(
            val data: List<T>,
            val offset: Int,
            val hasNext: Boolean = false
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
            val hasNext: Boolean = false
    ) : PaginationResponse<T>()

    /** Default value */
    class Nothing<T> : PaginationResponse<T>()
}