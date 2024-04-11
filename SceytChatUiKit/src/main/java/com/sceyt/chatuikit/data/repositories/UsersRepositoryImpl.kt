package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.BlockUserRequest
import com.sceyt.chat.models.user.UnBlockUserRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chat.models.user.UserListQueryByIds
import com.sceyt.chat.sceyt_callbacks.UsersCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig.USERS_LOAD_SIZE
import kotlinx.coroutines.suspendCancellableCoroutine

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
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Success(users))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadUsers error: ${e?.message}")
                }
            })
        }
    }


    override suspend fun loadMoreUsers(): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            usersQuery.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Success(users))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadMoreUsers error: ${e?.message}")
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
                    continuation.safeResume(SceytResponse.Success(users))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getSceytUsersByIds error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getSceytUserById(id: String): SceytResponse<User> {
        return suspendCancellableCoroutine { continuation ->
            val builder = UserListQueryByIds.Builder()
                .setIds(listOf(id))
                .build()

            builder.load(object : UsersCallback {
                override fun onResult(users: MutableList<User>) {
                    if (users.isNotEmpty())
                        continuation.safeResume(SceytResponse.Success(users[0]))
                    else continuation.safeResume(SceytResponse.Error(SceytException(0, "User not found")))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getSceytUserById error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun blockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            BlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "blockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun unblockUser(userId: String): SceytResponse<List<User>> {
        return suspendCancellableCoroutine { continuation ->
            UnBlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unblockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }
}