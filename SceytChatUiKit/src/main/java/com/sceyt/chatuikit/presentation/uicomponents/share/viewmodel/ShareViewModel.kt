package com.sceyt.chatuikit.presentation.uicomponents.share.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message.MessageBuilder
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.copyFile
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.mappers.getAttachmentType
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.shared.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger

class ShareViewModel : BaseViewModel(), SceytKoinComponent {
    private val messageInteractor by inject<MessageInteractor>()
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
                            setAttachments(arrayOf(buildAttachment("", links[0],
                                AttachmentTypeEnum.Link, "", 0,false)))
                    }
                    .build()

                launch(Dispatchers.IO) {
                    messageInteractor.sendMessageAsFlow(channelId, message).collect {
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

            channelIds.forEach { channelId ->
                val attachments = paths.map { path ->
                    val fileName = File(path).name
                    buildAttachment(path, "", getAttachmentType(path), fileName, getFileSize(path),true)
                }
                attachments.mapIndexed { index, attachment ->
                    val message = MessageBuilder(channelId)
                        .setBody(if (index == 0) messageBody else "")
                        .apply {
                            if (index == 0 && isContainsLink) {
                                val link = buildAttachment("", links[0], AttachmentTypeEnum.Link, "", 0,false)
                                setAttachments(arrayOf(attachment, link))
                            } else setAttachments(arrayOf(attachment))
                        }
                        .setTid(ClientWrapper.generateTid())
                        .setType(MessageTypeEnum.Media.value())
                        .build()

                    messageInteractor.sendSharedFileMessage(channelId, message)
                }
            }
        }

        trySend(State.Finish)
        awaitClose()
    }

    private fun buildAttachment(path: String, url: String,
                                typeEnum: AttachmentTypeEnum,
                                fileName: String, fileSize: Long,
                                upload: Boolean) =
            Attachment.Builder(path, url, typeEnum.value())
                .setName(fileName)
                .withTid(ClientWrapper.generateTid())
                .setFileSize(fileSize)
                .setMetadata("")
                .setUpload(upload)
                .build()

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
                            val copiedFile = copyFile(application, uri.toString(), name)
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