package com.sceyt.chatuikit.notifications.receivers

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.receivers.NotificationActionsBuilder.ACTION_READ
import com.sceyt.chatuikit.notifications.receivers.NotificationActionsBuilder.ACTION_REPLY
import com.sceyt.chatuikit.notifications.receivers.NotificationActionsBuilder.KEY_PUSH_DATA
import com.sceyt.chatuikit.notifications.receivers.NotificationActionsBuilder.KEY_REPLY_TEXT
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.push.PushData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

internal class NotificationActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pushNotification by lazy {
        SceytChatUIKit.notifications.pushNotification
    }

    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.parcelable<PushData>(KEY_PUSH_DATA) ?: return
        scope.launch {
            when (intent.action) {
                ACTION_READ -> {
                    pushNotification.notificationHandler.onMarkAsReadAction()
                    cancelNotification(data)
                    markAsRead(data.channel.id, data.message.id)
                }

                ACTION_REPLY -> {
                    RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY_TEXT)?.let { message ->
                        pushNotification.notificationHandler.onReplyAction(message)
                        replyMessage(context, data, message)
                    }
                }
            }
        }
    }

    private suspend fun markAsRead(
            channelId: Long,
            messageId: Long
    ) = withContext(Dispatchers.IO) {
        SceytLog.i(TAG, "MarkAsRead: channelId->$channelId," +
                " messageId->$messageId isConnected ${ConnectionEventManager.isConnected}")

        if (ConnectionEventManager.isConnected) {
            markAsReadAlreadyConnected(channelId, messageId)
        } else {
            val token = SceytChatUIKit.chatTokenProvider?.provideToken().takeIf { !it.isNullOrBlank() }
                    ?: run {
                        val exception = Exception("Couldn't get token to connect to markAsRead: " +
                                "channelId->$channelId, messageId->$messageId")
                        SceytLog.e(TAG, exception.message)
                        return@withContext markUsReadActionFinished(Result.failure(exception))
                    }

            ChatClient.getClient().connect(token)

            if (ConnectionEventManager.awaitToConnectSceytWithTimeout(10.seconds.inWholeMilliseconds)) {
                markAsReadAlreadyConnected(channelId, messageId)
            } else {
                markUsReadActionFinished(Result.failure(Exception("Couldn't connect to markAsRead: " +
                        "channelId->$channelId, messageId->$messageId")))
            }
        }
    }

    private suspend fun markAsReadAlreadyConnected(channelId: Long, messageId: Long) {
        val result = SceytChatUIKit.chatUIFacade.messageInteractor.markMessagesAs(
            channelId = channelId,
            marker = MarkerType.Displayed,
            messageId
        ).getOrNull(0)

        if (result is SceytResponse.Success) {
            SceytLog.i(TAG, "MarkAsRead: channelId->$channelId, messageId->$messageId -> success!")
        } else SceytLog.e(TAG, "MarkAsRead:channelId->$channelId, messageId->$messageId -> error ${result?.message}")

        markUsReadActionFinished(Result.success(true))
    }

    private suspend fun replyMessage(
            context: Context,
            data: PushData,
            text: CharSequence
    ) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        val channelId = data.channel.id

        val message = Message.MessageBuilder(channelId)
            .setUser(ClientWrapper.currentUser ?: User(SceytChatUIKit.chatUIFacade.myId))
            .setBody(text.toString())
            .build()

        SceytLog.i(TAG, "Start replyMessage: $text, isConnected ${ConnectionEventManager.isConnected}")
        if (ConnectionEventManager.isConnected) {
            sendMessage(context, message, data)
        } else {
            val token = SceytChatUIKit.chatTokenProvider?.provideToken().takeIf { !it.isNullOrBlank() }
                    ?: run {
                        val exception = Exception("Couldn't get token to connect to replyMessage: $text")
                        SceytLog.e(TAG, exception.message)
                        return@withContext replyActionFinished(Result.failure(exception))
                    }

            ChatClient.getClient().connect(token)

            if (ConnectionEventManager.awaitToConnectSceytWithTimeout(10.seconds.inWholeMilliseconds)) {
                sendMessage(context, message, data)
            } else {
                replyActionFinished(Result.failure(Exception("Couldn't connect to replyMessage: $text")))
            }
        }
    }

    private suspend fun sendMessage(context: Context, message: Message, data: PushData) {
        val channelId = data.channel.id
        SceytLog.d(TAG, "Replied conversation $channelId with text ${message.body}")
        val sceytMessage = message.toSceytUiMessage()
        SceytChatUIKit.chatUIFacade.messageInteractor.sendMessage(message.channelId, message)

        val user = SceytChatUIKit.chatUIFacade.userInteractor.getCurrentUser()
                ?: SceytChatUIKit.currentUserId?.let { SceytUser(it) }

        if (user != null) {
            updateNotification(context = context, data = data.copy(
                channel = data.channel.copy(lastMessage = sceytMessage),
                message = sceytMessage,
                user = user
            ))
        }
        replyActionFinished(Result.success(true))
    }

    private fun cancelNotification(data: PushData) {
        pushNotification.notificationHandler.cancelNotification(
            data.channel.id.toInt()
        )
    }

    private suspend fun updateNotification(context: Context, data: PushData) {
        if (checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return

        val notificationId = data.channel.id.toInt()
        val notification = pushNotification.notificationBuilder.buildNotification(
            context = context,
            data = data,
            notificationId = notificationId,
            builderCustomizer = { setSilent(true) }
        )

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun markUsReadActionFinished(result: Result<Boolean>) {
        pushNotification.notificationHandler.markUsReadActionFinished(result)
    }

    private fun replyActionFinished(result: Result<Boolean>) {
        pushNotification.notificationHandler.replyActionFinished(result)
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}
