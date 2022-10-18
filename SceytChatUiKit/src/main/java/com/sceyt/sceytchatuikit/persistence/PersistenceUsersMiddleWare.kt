package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

interface PersistenceUsersMiddleWare {
    suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>>
    suspend fun getCurrentUser(): User?
}