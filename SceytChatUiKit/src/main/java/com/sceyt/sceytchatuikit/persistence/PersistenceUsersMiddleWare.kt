package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow

interface PersistenceUsersMiddleWare {
    suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>>
}