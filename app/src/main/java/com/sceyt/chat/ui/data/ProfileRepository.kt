package com.sceyt.chat.ui.data

import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.SceytResponse

interface ProfileRepository {
    suspend fun getCurrentUser(): SceytResponse<User>
    suspend fun editProfile(displayName: String, avatarUri: String?): SceytResponse<User>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
}