package com.sceyt.chat.ui.data

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chat.sceyt_callbacks.UsersCallback
import com.sceyt.chat.ui.data.models.SceytResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UsersRepositoryImpl : UsersRepository {

    override suspend fun loadUsers(offset: Int, query: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            val userListQuery = UserListQuery.Builder()
                .order(UserListQuery.UserListQueryOrderKeyType.UserListQueryOrderKeyFirstName)
                .filter(UserListQuery.UserListFilterType.UserListFilterTypeAll)
                .limit(20)
                .query(query)
                .offset(offset)
                .build()

            userListQuery.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.resume(SceytResponse.Success(users))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}