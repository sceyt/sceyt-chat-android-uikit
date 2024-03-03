package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.SettingsCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chat.sceyt_callbacks.UserCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import kotlinx.coroutines.suspendCancellableCoroutine

internal class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun updateProfile(firstName: String?, lastName: String?, avatarUri: String?): SceytResponse<User> {
        return suspendCancellableCoroutine { continuation ->
            User.setProfileRequest().apply {
                avatarUri?.let { uri ->
                    setAvatar(uri)
                }
                setFirstName(firstName ?: "")
                setLastName(lastName ?: "")
            }.execute(object : UserCallback {
                override fun onResult(user: User) {
                    continuation.safeResume(SceytResponse.Success(user))
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