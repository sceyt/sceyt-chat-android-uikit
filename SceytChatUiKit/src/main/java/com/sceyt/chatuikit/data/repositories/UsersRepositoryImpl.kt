package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.BlockUserRequest
import com.sceyt.chat.models.user.UnBlockUserRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chat.models.user.UserListQueryByIds
import com.sceyt.chat.sceyt_callbacks.UsersCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import kotlinx.coroutines.suspendCancellableCoroutine

class UsersRepositoryImpl : UsersRepository {
    private lateinit var usersQuery: UserListQuery

    override suspend fun loadUsers(query: UserListQuery): SceytResponse<List<SceytUser>> {
        return suspendCancellableCoroutine { continuation ->
            usersQuery = query
            query.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Success(users.map {
                            it.toSceytUser()
                        }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadUsers error: ${e?.message}")
                }
            })
        }
    }


    override suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>> {
        return suspendCancellableCoroutine { continuation ->
            usersQuery.loadNext(object : UsersCallback {
                override fun onResult(users: MutableList<User>?) {
                    if (users.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(arrayListOf()))
                    else {
                        continuation.safeResume(SceytResponse.Success(users.map {
                            it.toSceytUser()
                        }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadMoreUsers error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getSceytUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>> {
        return suspendCancellableCoroutine { continuation ->
            val builder = UserListQueryByIds.Builder()
                .setIds(ids)
                .build()

            builder.load(object : UsersCallback {
                override fun onResult(users: MutableList<User>) {
                    continuation.safeResume(SceytResponse.Success(users.map {
                        it.toSceytUser()
                    }))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getSceytUsersByIds error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getSceytUserById(id: String): SceytResponse<SceytUser> {
        return suspendCancellableCoroutine { continuation ->
            val builder = UserListQueryByIds.Builder()
                .setIds(listOf(id))
                .build()

            builder.load(object : UsersCallback {
                override fun onResult(users: MutableList<User>) {
                    if (users.isNotEmpty())
                        continuation.safeResume(SceytResponse.Success(users[0].toSceytUser()))
                    else continuation.safeResume(SceytResponse.Error(SceytException(0, "User not found")))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getSceytUserById error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun blockUser(userId: String): SceytResponse<List<SceytUser>> {
        return suspendCancellableCoroutine { continuation ->
            BlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data?.map {
                        it.toSceytUser()
                    }))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "blockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun unblockUser(userId: String): SceytResponse<List<SceytUser>> {
        return suspendCancellableCoroutine { continuation ->
            UnBlockUserRequest(userId).execute(object : UsersCallback {
                override fun onResult(data: MutableList<User>?) {
                    continuation.safeResume(SceytResponse.Success(data?.map {
                        it.toSceytUser()
                    }))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unblockUser error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }
}