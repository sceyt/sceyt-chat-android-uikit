package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chat.models.user.UserListQueryByIds
import com.sceyt.chat.sceyt_callbacks.UsersCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig.USERS_LOAD_SIZE
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UsersRepositoryImpl : UsersRepository {
    private lateinit var usersQuery: UserListQuery

    override suspend fun loadUsers(query: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            val userListQuery = UserListQuery.Builder()
                .order(UserListQuery.UserListQueryOrderKeyType.UserListQueryOrderKeyFirstName)
                .filter(UserListQuery.UserListFilterType.UserListFilterTypeAll)
                .limit(USERS_LOAD_SIZE)
                .query(query)
                .build().also { usersQuery = it }

            userListQuery.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.resume(SceytResponse.Success(users))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }


    override suspend fun loadMoreUsers(): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            usersQuery.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.resume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.resume(SceytResponse.Success(users))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }

    override suspend fun getSceytUsersByIds(ids: List<String>): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            val builder = UserListQueryByIds.Builder()
                .setIds(ids)
                .build()

            builder.load(object : UsersCallback {
                override fun onResult(users: MutableList<User>) {
                    continuation.resume(SceytResponse.Success(users))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e))
                }
            })
        }
    }
}