package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.poll.ChangePollVoteRequest
import com.sceyt.chat.models.poll.ChangedVotes
import com.sceyt.chat.models.poll.ClosePollRequest
import com.sceyt.chat.models.poll.GetPollVotesRequest
import com.sceyt.chat.models.poll.PollVote
import com.sceyt.chat.models.poll.RetractPollVoteRequest
import com.sceyt.chat.sceyt_callbacks.ChangePollVoteCallback
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.PollVotesCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toVote
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import kotlinx.coroutines.suspendCancellableCoroutine

class PollRepositoryImpl : PollRepository {

    override suspend fun changeVotes(
        channelId: Long,
        messageId: Long,
        pollId: String,
        addOptionIds: List<String>,
        removeOptionIds: List<String>
    ): SceytResponse<ChangeVoteResponseData> = suspendCancellableCoroutine { continuation ->
        ChangePollVoteRequest(
            /* channelId = */ channelId,
            /* messageId = */ messageId,
            /* pollId = */ pollId,
            /* addOptionIds = */ addOptionIds.toTypedArray(),
            /* removeOptionIds = */ removeOptionIds.toTypedArray()
        ).execute(object : ChangePollVoteCallback {
            override fun onResult(message: Message, changedVotes: ChangedVotes) {
                val responseData = ChangeVoteResponseData(
                    message = message.toSceytUiMessage(),
                    addedVotes = changedVotes.addedVotes?.map { it.toVote() }.orEmpty(),
                    removedVotes = changedVotes.removedVotes?.map { it.toVote() }.orEmpty()
                )
                continuation.safeResume(SceytResponse.Success(responseData))
            }

            override fun onError(e: SceytException?) {
                continuation.safeResume(SceytResponse.Error(e))
                SceytLog.e(TAG, "changeVotes error: ${e?.message}")
            }
        })
    }

    override suspend fun retractVote(
        channelId: Long,
        messageId: Long,
        pollId: String
    ): SceytResponse<ChangeVoteResponseData> = suspendCancellableCoroutine { continuation ->
        RetractPollVoteRequest(channelId, messageId, pollId).execute(object : ChangePollVoteCallback {
            override fun onResult(message: Message, changedVotes: ChangedVotes) {
                val responseData = ChangeVoteResponseData(
                    message = message.toSceytUiMessage(),
                    addedVotes = changedVotes.addedVotes?.map { it.toVote() }.orEmpty(),
                    removedVotes = changedVotes.removedVotes?.map { it.toVote() }.orEmpty()
                )
                continuation.safeResume(SceytResponse.Success(responseData))
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

    override suspend fun getPollVotes(
        messageId: Long,
        pollId: String,
        optionId: String,
        nextToken: String
    ): SceytPagingResponse<List<Vote>> {
        return suspendCancellableCoroutine { continuation ->
            val limit = SceytChatUIKit.config.queryLimits.votersListQueryLimit
            val request = GetPollVotesRequest(messageId, pollId, optionId)
                .setLimit(limit)
                .setNextToken(nextToken)

            request.execute(object : PollVotesCallback {
                override fun onResult(votes: MutableList<PollVote>?) {
                    val mappedVotes = votes?.map { it.toVote() } ?: emptyList()
                    val updatedNextToken = request.nextToken
                    val hasNext =
                        mappedVotes.size >= limit || updatedNextToken?.isNotEmpty() == true
                    continuation.safeResume(
                        SceytPagingResponse.Success(
                            mappedVotes,
                            hasNext,
                            updatedNextToken
                        )
                    )
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytPagingResponse.Error(e))
                    SceytLog.e(TAG, "getPollVotesPaginated error: ${e?.message}")
                }
            })
        }
    }
}

