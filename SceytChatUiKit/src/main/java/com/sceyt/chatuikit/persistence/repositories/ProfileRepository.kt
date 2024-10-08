package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.SetProfileRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser

internal interface ProfileRepository {
    suspend fun updateProfile(request: SetProfileRequest): SceytResponse<SceytUser>
    suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean>
    suspend fun unMuteNotifications(): SceytResponse<Boolean>
    suspend fun getSettings(): SceytResponse<UserSettings>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
}