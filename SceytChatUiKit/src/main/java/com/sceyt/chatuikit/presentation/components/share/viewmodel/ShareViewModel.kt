package com.sceyt.chatuikit.presentation.components.share.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message.MessageBuilder
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.copyFile
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.mappers.getAttachmentType
import com.sceyt.chatuikit.persistence.mappers.toMetadata
import com.sceyt.chatuikit.presentation.components.channel.input.link.SingleLinkDetailsProvider
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.shared.utils.FilePathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ShareViewModel : BaseViewModel(), SceytKoinComponent {
    private val messageInteractor by inject<MessageInteractor>()
    private val application by inject<Application>()
    private val linkDetailsProvider by lazy {
        SingleLinkDetailsProvider(application, viewModelScope)
    }

    fun sendTextMessage(vararg channelIds: Long, body: String) = callbackFlow {
        trySend(State.Loading)

        val links = body.extractLinks()
        val isContainsLink = links.isNotEmpty()

        val count = AtomicInteger(0)
        withContext(Dispatchers.IO) {
            val linkPreviewDetails = if (isContainsLink) loadLinkPreview(links[0]) else null

            channelIds.forEach { channelId ->
                val message = MessageBuilder(channelId)
                    .setBody(body)
                    .setTid(ClientWrapper.generateTid())
                    .setType(SceytMessageType.Text.value)
                    .apply {
                        if (isContainsLink) {
                            val link = links[0]
                            val details = linkPreviewDetails ?: LinkPreviewDetails.hiddenLink(link)
                            setAttachments(
                                arrayOf(
                                    buildAttachment(
                                        path = "",
                                        url = link,
                                        typeEnum = AttachmentTypeEnum.Link,
                                        fileName = "",
                                        fileSize = 0,
                                        metadata = details.toMetadata()
                                    )
                                )
                            )
                        }
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


    fun sendFilesMessage(
        vararg channelIds: Long,
        uris: List<Uri>,
        messageBody: String
    ) = callbackFlow {
        trySend(State.Loading)
        val links = messageBody.extractLinks()
        val isContainsLink = links.isNotEmpty()

        withContext(Dispatchers.IO) {
            val paths = getPathFromFile(*uris.toTypedArray()).toMutableList()
            val linkPreviewDetails = if (isContainsLink) loadLinkPreview(links[0]) else null

            channelIds.forEach { channelId ->
                val attachments = paths.map { path ->
                    val fileName = File(path).name
                    buildAttachment(
                        path = path,
                        url = "",
                        typeEnum = getAttachmentType(path),
                        fileName = fileName,
                        fileSize = getFileSize(path),
                        metadata = ""
                    )
                }
                attachments.mapIndexed { index, attachment ->
                    val message = MessageBuilder(channelId)
                        .setBody(if (index == 0) messageBody else "")
                        .apply {
                            if (index == 0 && isContainsLink) {
                                val linkUrl = links[0]
                                val details =
                                    linkPreviewDetails ?: LinkPreviewDetails.hiddenLink(linkUrl)
                                val link = buildAttachment(
                                    path = "",
                                    url = linkUrl,
                                    typeEnum = AttachmentTypeEnum.Link,
                                    fileName = "",
                                    fileSize = 0,
                                    metadata = details.toMetadata()
                                )
                                setAttachments(arrayOf(attachment, link))
                            } else setAttachments(arrayOf(attachment))
                        }
                        .setTid(ClientWrapper.generateTid())
                        .setType(SceytMessageType.Media.value)
                        .build()

                    messageInteractor.sendSharedFileMessage(channelId, message)
                }
            }
        }

        trySend(State.Finish)
        awaitClose()
    }

    private suspend fun loadLinkPreview(link: String): LinkPreviewDetails? {
        return withTimeoutOrNull(5.seconds) {
            linkDetailsProvider.loadLinkDetailsSuspend(link)
        }
    }

    private fun buildAttachment(
        path: String,
        url: String,
        typeEnum: AttachmentTypeEnum,
        fileName: String,
        fileSize: Long,
        metadata: String
    ) = Attachment.Builder(path, url, typeEnum.value)
        .setName(fileName)
        .withTid(ClientWrapper.generateTid())
        .setFileSize(fileSize)
        .setMetadata(metadata)
        .setUpload(false)
        .build()

    private fun getPathFromFile(vararg uris: Uri): List<String> {
        val paths = mutableListOf<String>()
        uris.forEach { uri ->
            try {
                var realFile: File? = null
                try {
                    val path = FilePathUtil.getFilePathFromUri(
                        context = application,
                        parentDirToCopy = application.filesDir,
                        uri = uri
                    ) ?: return@forEach
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