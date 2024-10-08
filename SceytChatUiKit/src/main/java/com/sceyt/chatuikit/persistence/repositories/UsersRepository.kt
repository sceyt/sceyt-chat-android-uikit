package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser

interface UsersRepository {
    suspend fun loadUsers(query: String): SceytResponse<List<SceytUser>>
    suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>>
    suspend fun getSceytUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>>
    suspend fun getSceytUserById(id: String): SceytResponse<SceytUser>
    suspend fun blockUser(userId: String): SceytResponse<List<SceytUser>>
    suspend fun unblockUser(userId: String): SceytResponse<List<SceytUser>>
}