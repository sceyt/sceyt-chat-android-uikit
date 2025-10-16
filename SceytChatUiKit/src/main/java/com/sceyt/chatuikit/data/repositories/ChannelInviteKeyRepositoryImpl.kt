package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.ChannelInviteKey
import com.sceyt.chat.models.channel.CreateChannelInviteKeyRequest
import com.sceyt.chat.models.channel.DeleteRevokedInviteKeyRequest
import com.sceyt.chat.models.channel.GetChannelInviteKeyRequest
import com.sceyt.chat.models.channel.GetChannelInviteKeysRequest
import com.sceyt.chat.models.channel.RegenerateChannelInviteKeyRequest
import com.sceyt.chat.models.channel.RevokeChannelInviteKeyRequest
import com.sceyt.chat.models.channel.UpdateChannelInviteKeyRequest
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.InviteKeyCallback
import com.sceyt.chat.sceyt_callbacks.InviteKeysCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toChannelInviteKeyData
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import kotlinx.coroutines.suspendCancellableCoroutine

class ChannelInviteKeyRepositoryImpl : ChannelInviteKeyRepository {

    override suspend fun getChannelInviteKeys(
            channelId: Long,
    ): SceytResponse<List<ChannelInviteKeyData>> {
        return suspendCancellableCoroutine { continuation ->
            GetChannelInviteKeysRequest(channelId).execute(object : InviteKeysCallback {
                override fun onResult(inviteKeys: List<ChannelInviteKey>) {
                    val data = inviteKeys.map { it.toChannelInviteKeyData() }
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannelInviteKeys error for channelId: $channelId, error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun getChannelInviteKey(
            channelId: Long,
            key: String,
    ): SceytResponse<ChannelInviteKeyData> {
        return suspendCancellableCoroutine { continuation ->
            GetChannelInviteKeyRequest(channelId, key).execute(object : InviteKeyCallback {
                override fun onResult(inviteKey: ChannelInviteKey) {
                    val data = inviteKey.toChannelInviteKeyData()
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannelInviteKey error: ${e?.message}, code: ${e?.code}, channelId: $channelId, key: $key")
                }
            })
        }
    }

    override suspend fun createChannelInviteKey(
            channelId: Long,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<ChannelInviteKeyData> {
        return suspendCancellableCoroutine { continuation ->
            CreateChannelInviteKeyRequest.Builder(channelId)
                .setMaxUses(maxUses)
                .setExpiresAt(expireAt)
                .setAccessPriorHistory(accessPriorHistory)
                .build().execute(object : InviteKeyCallback {
                    override fun onResult(inviteKey: ChannelInviteKey) {
                        val data = inviteKey.toChannelInviteKeyData()
                        continuation.safeResume(SceytResponse.Success(data))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "createChannelInviteLink error: ${e?.message}, code: ${e?.code}, channelId: $channelId")
                    }
                })
        }
    }

    override suspend fun updateInviteKeySettings(
            channelId: Long,
            key: String,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            UpdateChannelInviteKeyRequest.Builder(channelId, key)
                .setAccessPriorHistory(accessPriorHistory)
                .setMaxUses(maxUses)
                .setExpiresAt(expireAt)
                .build().execute(object : ActionCallback {
                    override fun onSuccess() {
                        continuation.safeResume(SceytResponse.Success(true))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "updateInviteKeySettings error: ${e?.message}, code: ${e?.code}, channelId: $channelId, key: $key")
                    }
                })
        }
    }

    override suspend fun regenerateChannelInviteKey(
            channelId: Long,
            key: String,
    ): SceytResponse<ChannelInviteKeyData> {
        return suspendCancellableCoroutine { continuation ->
            RegenerateChannelInviteKeyRequest(
                channelId, key
            ).execute(object : InviteKeyCallback {
                override fun onResult(inviteKey: ChannelInviteKey) {
                    val data = inviteKey.toChannelInviteKeyData()
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "regenerateChannelInviteKey error: ${e?.message}, code: ${e?.code}, channelId: $channelId, key: $key")
                }
            })
        }
    }

    override suspend fun revokeChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            RevokeChannelInviteKeyRequest(
                channelId, keys.toTypedArray()
            ).execute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "revokeChannelInviteKeys error: ${e?.message}, code: ${e?.code}, channelId: $channelId, keys: $keys")
                }
            })
        }
    }

    override suspend fun deleteRevokedChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> {
        return suspendCancellableCoroutine { continuation ->
            DeleteRevokedInviteKeyRequest(
                channelId, keys.toTypedArray()
            ).execute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "deleteRevokedChannelInviteKeys error: ${e?.message}, code: ${e?.code}, channelId: $channelId, keys: $keys")
                }
            })
        }
    }
}