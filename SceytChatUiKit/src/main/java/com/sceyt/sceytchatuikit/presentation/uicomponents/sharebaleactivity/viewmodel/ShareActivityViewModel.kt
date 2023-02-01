package com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message.MessageBuilder
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.mappers.getAttachmentType
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.shared.utils.FileUtil
import com.sceyt.sceytchatuikit.shared.utils.ImageUriPathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger

class ShareActivityViewModel : BaseViewModel(), SceytKoinComponent {
    private val messagesMiddleWare by inject<PersistenceMessagesMiddleWare>()
    private val application by inject<Application>()

    fun sendTextMessage(vararg channelIds: Long, body: String): Flow<State> {
        return callbackFlow {
            trySend(State.Loading)

            val count = AtomicInteger(0)
            viewModelScope.launch {
                channelIds.forEach { channelId ->
                    val message = MessageBuilder(channelId)
                        .setBody(body)
                        .setTid(ClientWrapper.generateTid())
                        .setType(MessageTypeEnum.Text.value())
                        .build()

                    launch(Dispatchers.IO) {
                        messagesMiddleWare.sendMessageAsFlow(channelId, message).collect {
                            if (it is SendMessageResult.Response) {
                                val resultCount = count.addAndGet(1)

                                if (resultCount == channelIds.size)
                                    trySend(State.Finish)
                            }
                        }
                    }
                }
            }
            awaitClose()
        }
    }

    fun sendFilesMessage(vararg channelIds: Long, uris: List<Uri>, messageBody: String): Flow<State> {
        return callbackFlow {
            trySend(State.Loading)
            val paths = getPathFromFile(*uris.toTypedArray())

            channelIds.forEach { channelId ->
                val attachments = paths.map { path ->
                    Attachment.Builder(path, "", getAttachmentType(path).value())
                        .setName(File(path).name ?: "File")
                        .withTid(ClientWrapper.generateTid())
                        .setFileSize(getFileSize(path))
                        .setUpload(false)
                        .build()
                }
                attachments.mapIndexed { index, attachment ->
                    val message = MessageBuilder(channelId)
                        .setBody(if (index == 0) messageBody else "")
                        .setAttachments(arrayOf(attachment))
                        .setTid(ClientWrapper.generateTid())
                        .setType(MessageTypeEnum.Media.value())
                        .build()

                    messagesMiddleWare.sendMessage(channelId, message)
                }
            }

            trySend(State.Finish)
            awaitClose()
        }
    }

    private fun getPathFromFile(vararg uris: Uri): List<String> {
        val paths = mutableListOf<String>()
        uris.forEach { uri ->
            try {
                var realFile: File? = null
                try {
                    val path = FileUtil(application).getPath(uri)
                    FileInputStream(File(path))
                    realFile = File(path)
                } catch (ex: Exception) {
                    Log.e(TAG, "error to get path with reason ${ex.message}")
                } finally {
                    if (realFile != null && realFile.exists()) {
                        paths.add(realFile.path)
                    } else {
                        val name = DocumentFile.fromSingleUri(application, uri)?.name
                        if (name != null) {
                            val copiedFile = ImageUriPathUtil.copyFile(application, uri.toString(), name)
                            paths.add(copiedFile.path)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "error to copy file with reason ${e.message}")
            }
        }
        return paths
    }

    enum class State {
        Loading,
        Finish
    }
}