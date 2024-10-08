package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.SetProfileRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.SettingsCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chat.sceyt_callbacks.UserCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.repositories.ProfileRepository
import kotlinx.coroutines.suspendCancellableCoroutine

internal class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun updateProfile(request: SetProfileRequest): SceytResponse<SceytUser> {
        return suspendCancellableCoroutine { continuation ->
            request.execute(object : UserCallback {
                override fun onResult(user: User) {
                    continuation.safeResume(SceytResponse.Success(user.toSceytUser()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "updateProfile error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().unMute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(false))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "unMuteNotifications error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().mute(muteUntil, object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "muteNotifications error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getSettings(): SceytResponse<UserSettings> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().getUserSettings(object : SettingsCallback {
                override fun onResult(settings: UserSettings) {
                    continuation.safeResume(SceytResponse.Success(settings))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error())
                    SceytLog.e(TAG, "getSettings error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun uploadAvatar(avatarUri: String): SceytResponse<String> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().upload(avatarUri, object : ProgressCallback {
                override fun onResult(pct: Float) {
                }

                override fun onError(e: SceytException?) {}
            }, object : UrlCallback {

                override fun onResult(url: String) {
                    continuation.safeResume(SceytResponse.Success(url))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "uploadAvatar error: ${e?.message}")
                }
            })
        }
    }
}