package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

internal class MessageScrollCoordinator {
    private var nextRequestId = 0L
    private var activeRequest: Request? = null

    fun beginNewestMessageRequest(targetMessageId: Long): Request {
        return beginRequest(
            type = RequestType.NewestMessage,
            targetMessageId = targetMessageId,
            keepUntilLoadingSettles = true,
        )
    }

    fun beginMessageRequest(targetMessageId: Long): Request {
        return beginRequest(
            type = RequestType.Message,
            targetMessageId = targetMessageId,
            keepUntilLoadingSettles = false,
        )
    }

    fun activeRequestFor(requestId: Long?): Request? {
        if (requestId == null)
            return null

        return activeRequest?.takeIf { it.id == requestId }
    }

    fun activeNewestMessageRequest(): Request? {
        return activeRequest?.takeIf {
            it.type == RequestType.NewestMessage && it.targetMessageId != null
        }
    }

    fun clearIfSettled(request: Request, loadingInProgress: Boolean) {
        if (activeRequest?.id != request.id)
            return

        if (!request.keepUntilLoadingSettles || !loadingInProgress)
            activeRequest = null
    }

    fun clear(request: Request) {
        if (activeRequest?.id == request.id)
            activeRequest = null
    }

    fun cancelActiveRequest() {
        activeRequest = null
    }

    fun canRunDelayedWorkFor(request: Request): Boolean {
        return activeRequest?.id == request.id || activeRequest == null
    }

    private fun beginRequest(
        type: RequestType,
        targetMessageId: Long?,
        keepUntilLoadingSettles: Boolean,
    ): Request {
        return Request(
            id = ++nextRequestId,
            type = type,
            targetMessageId = targetMessageId,
            keepUntilLoadingSettles = keepUntilLoadingSettles,
        ).also {
            activeRequest = it
        }
    }

    internal enum class RequestType {
        NewestMessage,
        Message,
    }

    internal data class Request(
        val id: Long,
        val type: RequestType,
        val targetMessageId: Long?,
        val keepUntilLoadingSettles: Boolean,
    )
}
