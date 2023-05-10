package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse

internal interface ProfileRepository {
    suspend fun updateProfile(firstName: String?, lastName: String?, avatarUri: String?): SceytResponse<User>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<UserSettings>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
}