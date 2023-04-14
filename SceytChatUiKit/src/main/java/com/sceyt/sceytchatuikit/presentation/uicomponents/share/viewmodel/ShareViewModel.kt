package com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message.MessageBuilder
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.extractLinks
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.extensions.transcodeVideo
import com.sceyt.sceytchatuikit.persistence.mappers.getAttachmentType
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.shared.utils.FileUtil
import com.sceyt.sceytchatuikit.shared.utils.ImageUriPathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger

class ShareViewModel : BaseViewModel(), SceytKoinComponent {
    private val messagesMiddleWare by inject<PersistenceMessagesMiddleWare>()
    private val application by inject<Application>()

    fun sendTextMessage(vararg channelIds: Long, body: String) = callbackFlow {
        trySend(State.Loading)

        val links = body.extractLinks()
        val isContainsLink = links.isNotEmpty()

        val count = AtomicInteger(0)
        withContext(Dispatchers.IO) {
            channelIds.forEach { channelId ->
                val message = MessageBuilder(channelId)
                    .setBody(body)
                    .setTid(ClientWrapper.generateTid())
                    .setType(MessageTypeEnum.Text.value())
                    .apply {
                        if (isContainsLink)
                            setAttachments(arrayOf(Attachment.Builder("", links[0], AttachmentTypeEnum.Link.value())
                                .withTid(ClientWrapper.generateTid())
                                .setName("")
                                .setMetadata("")
                                .build()))
                    }
                    .build()

                launch(Dispatchers.IO) {
                    messagesMiddleWare.sendMessageAsFlow(channelId, message).collect {
                        if (it.isServerResponse() || it is SendMessageResult.StartedSendingAttachment) {
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


    fun sendFilesMessage(vararg channelIds: Long, uris: List<Uri>, messageBody: String) = callbackFlow {
        trySend(State.Loading)
        val links = messageBody.extractLinks()
        val isContainsLink = links.isNotEmpty()

        withContext(Dispatchers.IO) {
            val paths = getPathFromFile(*uris.toTypedArray()).toMutableList()
            val resizedPaths = paths.map { path ->
                checkAndResizeMessageFile(application, path)
            }

            channelIds.forEach { channelId ->
                val attachments = resizedPaths.map { data ->
                    val fileName = File(data.originalFilePath).name
                    val path = data.resizedPath
                    Attachment.Builder(path, "", getAttachmentType(path).value())
                        .setName(fileName)
                        .withTid(ClientWrapper.generateTid())
                        .setFileSize(getFileSize(data.resizedPath))
                        .setUpload(false)
                        .build()
                }
                attachments.mapIndexed { index, attachment ->
                    val message = MessageBuilder(channelId)
                        .setBody(if (index == 0) messageBody else "")
                        .apply {
                            if (index == 0 && isContainsLink) {
                                setAttachments(arrayOf(attachment, Attachment.Builder("", links[0], AttachmentTypeEnum.Link.value())
                                    .withTid(ClientWrapper.generateTid())
                                    .setName("")
                                    .setMetadata("")
                                    .build()))
                            } else setAttachments(arrayOf(attachment))
                        }
                        .setTid(ClientWrapper.generateTid())
                        .setType(MessageTypeEnum.Media.value())
                        .build()

                    messagesMiddleWare.sendSharedFileMessage(channelId, message)
                }
            }
        }

        trySend(State.Finish)
        awaitClose()
    }

    data class ResizeFileInfo(
            var resizedPath: String,
            val originalFilePath: String,
            val type: AttachmentTypeEnum
    )

    private suspend fun checkAndResizeMessageFile(context: Context, path: String) = suspendCancellableCoroutine { continuation ->
        val type = getAttachmentType(path)
        val data = ResizeFileInfo(path, path, type)
        when (type.value()) {
            AttachmentTypeEnum.Image.value() -> {
                val result = resizeImage(context, path, 1080)
                if (result.isSuccess && result.getOrNull().isNotNullOrBlank())
                    data.resizedPath = result.getOrThrow()

                continuation.safeResume(data)
            }
            AttachmentTypeEnum.Video.value() -> {
                transcodeVideo(context, path) { result ->
                    if (result.isSuccess && result.getOrNull().isNotNullOrBlank())
                        data.resizedPath = result.getOrThrow()

                    continuation.safeResume(data)
                }
            }
            else -> continuation.safeResume(data)
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