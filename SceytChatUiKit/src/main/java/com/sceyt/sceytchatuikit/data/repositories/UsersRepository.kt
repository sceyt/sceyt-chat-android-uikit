package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

interface UsersRepository {
    suspend fun loadUsers(query: String): SceytResponse<List<User>>
    suspend fun loadMoreUsers(): SceytResponse<List<User>>
    suspend fun getSceytUsersByIds(ids: List<String>): SceytResponse<List<User>>
    suspend fun getSceytUserById(id: String): SceytResponse<User>
}