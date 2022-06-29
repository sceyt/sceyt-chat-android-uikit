package com.sceyt.chat.ui.data

import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.SceytResponse

interface UsersRepository {
    suspend fun loadUsers(offset: Int, query: String): SceytResponse<List<User>>
}