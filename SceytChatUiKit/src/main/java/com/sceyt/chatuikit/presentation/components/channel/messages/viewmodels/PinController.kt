package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.MessagePinInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns pin actions and prevents duplicate taps, like [PollController]. */
internal class PinController(
    private val scope: CoroutineScope,
    private val pinInteractor: MessagePinInteractor,
    private val channelId: () -> Long,
    private val notifyResponse: (SceytResponse<*>, showError: Boolean) -> Unit,
) {
    private var pinJob: Job? = null

    fun pin(message: SceytMessage, pinType: PinType) {
        if (pinJob?.isActive == true) return
        pinJob = scope.launch {
            val response = pinInteractor.pinMessage(
                channelId = channelId(),
                messageTid = message.tid,
                pinType = pinType
            )
            notifyResult(response)
        }
    }

    fun unpin(messageTid: Long) {
        if (pinJob?.isActive == true) return
        pinJob = scope.launch {
            val response = pinInteractor.unpinMessage(
                channelId = channelId(),
                messageTid = messageTid
            )
            notifyResult(response)
        }
    }

    private fun notifyResult(response: SceytResponse<*>) {
        // Retryable failures remain queued; terminal failures require user feedback.
        val showError = response is SceytResponse.Error &&
                SDKErrorTypeEnum.fromValue(response.exception?.type)?.isResendable == false
        notifyResponse(response, showError)
    }
}
