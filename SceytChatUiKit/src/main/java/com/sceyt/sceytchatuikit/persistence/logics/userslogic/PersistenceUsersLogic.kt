package com.sceyt.sceytchatuikit.persistence.logics.userslogic

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

interface PersistenceUsersLogic {
    suspend fun getSceytUsers(ids: List<String>): SceytResponse<List<User>>
    suspend fun getCurrentUser(): User?
}