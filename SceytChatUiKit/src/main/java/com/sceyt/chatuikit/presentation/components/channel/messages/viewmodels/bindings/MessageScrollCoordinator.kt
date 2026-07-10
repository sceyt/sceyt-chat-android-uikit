package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import com.sceyt.chatuikit.presentation.common.recyclerview.ScrollHandle

/**
 * Single arbiter for every message-list scroll command. Only one [Request] is active at a time;
 * beginning a new request [supersede]s the previous one — cancelling its in-flight physical scroll
 * ([ScrollHandle]) and its pending page load ([cancelPendingLoad]) — so a newer, different scroll
 * always wins over a stale one.
 */
internal class MessageScrollCoordinator(
    private val cancelPendingLoad: () -> Unit = {},
) {
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

    fun beginUnreadRequest(): Request {
        return beginRequest(
            type = RequestType.Unread,
            targetMessageId = null,
            keepUntilLoadingSettles = false,
        )
    }

    fun beginLastMessageRequest(targetMessageId: Long?): Request {
        return beginRequest(
            type = RequestType.LastMessage,
            targetMessageId = targetMessageId,
            keepUntilLoadingSettles = false,
        )
    }

    fun beginRealtimeScrollRequest(): Request {
        return beginRequest(
            type = RequestType.RealtimeScroll,
            targetMessageId = null,
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

    /** True while an explicit user jump is pending — used to suppress incoming realtime auto-scroll. */
    fun hasActiveExplicitJump(): Boolean {
        return activeRequest?.let {
            it.type == RequestType.Message ||
                    it.type == RequestType.Unread ||
                    it.type == RequestType.NewestMessage
        } ?: false
    }

    /** Attach the physical scroll handle so a later supersede can cancel it. */
    fun attachPhysicalHandle(requestId: Long, handle: ScrollHandle) {
        val request = activeRequest?.takeIf { it.id == requestId }
        if (request == null) {
            // Request was already superseded before the handle arrived — cancel it right away.
            handle.cancel()
            return
        }
        request.physicalHandle = handle
    }

    /** Mark that the given request kicked off a page load, so supersede cancels that load too. */
    fun markLoadStarted(requestId: Long) {
        activeRequest?.takeIf { it.id == requestId }?.hasPendingLoad = true
    }

    fun clearIfSettled(request: Request, loadingInProgress: Boolean) {
        if (activeRequest?.id != request.id)
            return

        if (!request.keepUntilLoadingSettles || !loadingInProgress) {
            request.physicalHandle = null
            activeRequest = null
        }
    }

    fun clear(request: Request) {
        if (activeRequest?.id == request.id) {
            request.physicalHandle = null
            activeRequest = null
        }
    }

    fun cancelActiveRequest() {
        supersede(activeRequest)
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
        supersede(activeRequest)
        return Request(
            id = ++nextRequestId,
            type = type,
            targetMessageId = targetMessageId,
            keepUntilLoadingSettles = keepUntilLoadingSettles,
        ).also {
            activeRequest = it
        }
    }

    private fun supersede(outgoing: Request?) {
        outgoing ?: return
        outgoing.physicalHandle?.cancel()
        outgoing.physicalHandle = null
        if (outgoing.hasPendingLoad)
            cancelPendingLoad()
    }

    internal enum class RequestType {
        NewestMessage,
        Message,
        Unread,
        LastMessage,
        RealtimeScroll,
    }

    internal class Request(
        val id: Long,
        val type: RequestType,
        val targetMessageId: Long?,
        val keepUntilLoadingSettles: Boolean,
    ) {
        var physicalHandle: ScrollHandle? = null
        var hasPendingLoad: Boolean = false
    }
}