package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser

interface UsersRepository {
    suspend fun loadUsers(query: UserListQuery): SceytResponse<List<SceytUser>>
    suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>>
    suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>>
    suspend fun getUserById(id: String): SceytResponse<SceytUser>
    suspend fun blockUser(userId: String): SceytResponse<List<SceytUser>>
    suspend fun unblockUser(userId: String): SceytResponse<List<SceytUser>>
}