package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.poll.AddPollVoteRequest
import com.sceyt.chat.models.poll.ClosePollRequest
import com.sceyt.chat.models.poll.DeletePollVoteRequest
import com.sceyt.chat.models.poll.RetractPollVoteRequest
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import kotlinx.coroutines.suspendCancellableCoroutine

class PollRepositoryImpl : PollRepository {

    override suspend fun addVote(
        channelId: Long,
        messageId: Long,
        pollId: String,
        optionId: String
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        AddPollVoteRequest(channelId, messageId, pollId, arrayOf(optionId))
            .execute(object : MessageCallback {
                override fun onResult(message: Message) {
                    continuation.safeResume(SceytResponse.Success(message.toSceytUiMessage()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "addVote error: ${e?.message}")
                }
            })
    }

    override suspend fun deleteVote(
        channelId: Long,
        messageId: Long,
        pollId: String,
        optionId: String
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        DeletePollVoteRequest(channelId, messageId, pollId, arrayOf(optionId))
            .execute(object : MessageCallback {
                override fun onResult(message: Message) {
                    continuation.safeResume(SceytResponse.Success(message.toSceytUiMessage()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "deleteVote error: ${e?.message}")
                }
            })
    }

    override suspend fun retractVote(
        channelId: Long,
        messageId: Long,
        pollId: String
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        RetractPollVoteRequest(channelId, messageId, pollId).execute(object : MessageCallback {
            override fun onResult(message: Message) {
                continuation.safeResume(SceytResponse.Success(message.toSceytUiMessage()))
            }

            override fun onError(e: SceytException?) {
                continuation.safeResume(SceytResponse.Error(e))
                SceytLog.e(TAG, "retractVote error: ${e?.message}")
            }
        })
    }

    override suspend fun endPoll(
        channelId: Long,
        messageId: Long,
        pollId: String
    ): SceytResponse<SceytMessage> = suspendCancellableCoroutine { continuation ->
        ClosePollRequest(channelId, messageId, pollId).execute(object : MessageCallback {
            override fun onResult(message: Message) {
                continuation.safeResume(SceytResponse.Success(message.toSceytUiMessage()))
            }

            override fun onError(e: SceytException?) {
                continuation.safeResume(SceytResponse.Error(e))
                SceytLog.e(TAG, "endPoll error: ${e?.message}")
            }
        })
    }
}

