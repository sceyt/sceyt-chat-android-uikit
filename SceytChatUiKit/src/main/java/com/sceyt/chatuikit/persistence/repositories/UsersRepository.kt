package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.SceytResponse

interface UsersRepository {
    suspend fun loadUsers(query: String): SceytResponse<List<User>>
    suspend fun loadMoreUsers(): SceytResponse<List<User>>
    suspend fun getSceytUsersByIds(ids: List<String>): SceytResponse<List<User>>
    suspend fun getSceytUserById(id: String): SceytResponse<User>
    suspend fun blockUser(userId: String): SceytResponse<List<User>>
    suspend fun unblockUser(userId: String): SceytResponse<List<User>>
}