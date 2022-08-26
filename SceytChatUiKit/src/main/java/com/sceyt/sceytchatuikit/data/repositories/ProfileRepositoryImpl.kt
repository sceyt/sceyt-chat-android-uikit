package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.sceyt_callbacks.UrlCallback
import com.sceyt.chat.sceyt_callbacks.UserCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun getCurrentUser(): SceytResponse<User> {
        return suspendCoroutine { continuation ->
            ClientWrapper.currentUser?.let {
                return@let continuation.resume(SceytResponse.Success(it))
            } ?: run {
                return@run continuation.resume(SceytResponse.Error("User not found"))
            }
        }
    }

    override suspend fun editProfile(displayName: String, avatarUri: String?): SceytResponse<User> {
        return suspendCoroutine { continuation ->
            val names = displayName.split(" ".toRegex(), 2)
            User.setProfileRequest().apply {
                avatarUri?.let { uri ->
                    setAvatar(uri)
                }
                setFirstName(names.getOrNull(0) ?: "")
                setLastName(names.getOrNull(1) ?: "")
            }.execute(object : UserCallback {
                override fun onResult(user: User) {
                    continuation.resume(SceytResponse.Success(user))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().unMute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(false))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().mute(muteUntil, object : ActionCallback {
                override fun onSuccess() {
                    continuation.resume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }

    override suspend fun getSettings(): SceytResponse<Settings> {
        return suspendCancellableCoroutine { continuation ->
            ChatClient.getClient().getSettings {
                if (it != null)
                    continuation.resume(SceytResponse.Success(it))
                else continuation.resume(SceytResponse.Error())
            }
        }
    }

    override suspend fun uploadAvatar(avatarUri: String): SceytResponse<String> {
        return suspendCoroutine { continuation ->
            ChatClient.getClient().upload(avatarUri, object : ProgressCallback {
                override fun onResult(pct: Float) {
                }

                override fun onError(e: SceytException?) {}
            }, object : UrlCallback {

                override fun onResult(url: String) {
                    continuation.resume(SceytResponse.Success(url))
                }

                override fun onError(e: SceytException?) {
                    continuation.resume(SceytResponse.Error(e?.message))
                }
            })
        }
    }
}