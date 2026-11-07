package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import android.os.Process
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentUploadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.attachment.AttachmentsCache
import com.sceyt.chatuikit.persistence.logicimpl.attachment.PersistenceAttachmentLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toAttachmentDb
import com.sceyt.chatuikit.persistence.repositories.AttachmentsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy

@RunWith(AndroidJUnit4::class)
class AttachmentUploadPreparationAndroidTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory = File(context.filesDir, "pre-upload-restart-test")
    private val source = File(directory, "source.bin")
    private val expectedCiphertext = File(directory, "expected.bin")
    private val checkpointFile = File(directory, "checkpoint.txt")
    private val processFile = File(directory, "process.txt")
    private val gson = Gson()

    @Test
    fun savedPreparationSurvivesRestart() = runBlocking(Dispatchers.IO) {
        // The two phases can also run in separate instrumentation processes.
        val phase = InstrumentationRegistry.getArguments().getString("preUploadPhase")
        if (phase != "resume") {
            context.deleteDatabase(DATABASE)
            directory.deleteRecursively()
            check(directory.mkdirs())
            source.writeBytes(ByteArray(128 * 1024 + 19) { (it % 251).toByte() })
            processFile.writeText(Process.myPid().toString())
            upload(resuming = false)
        }

        if (phase != "prepare") {
            if (phase == "resume") {
                assertThat(processFile.readText().toInt()).isNotEqualTo(Process.myPid())
            }
            try {
                upload(resuming = true)
            } finally {
                context.deleteDatabase(DATABASE)
                directory.deleteRecursively()
            }
        }
    }

    private suspend fun upload(resuming: Boolean) {
        val previous = SceytChatUIKit.fileTransfer
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val database = Room.databaseBuilder(context, SceytDatabase::class.java, DATABASE).build()
        try {
            if (!resuming) database.messageDao().insertMessagesIgnored(listOf(message()))
            val saved = checkNotNull(database.messageDao().getMessageByTid(MESSAGE_TID))
                .attachments.orEmpty().single().toAttachment()
            val originalData = gson.fromJson(saved.metadata, JsonObject::class.java)
            var preparations = 0
            val config = SceytChatUIKitFileTransfer().apply {
                encryptionEnabled = true
                provider = object : FileTransferProvider {
                    override suspend fun prepareUpload(request: FileUploadRequest): Map<String, String> {
                        val metadata = gson.fromJson(request.attachment.metadata, JsonObject::class.java)
                        return if (resuming) {
                            assertThat(metadata).isEqualTo(originalData)
                            mapOf(
                                "provider" to metadata.get("provider").asString,
                                "encryption" to metadata.get("encryption").asString,
                            )
                        } else {
                            assertThat(metadata.has("encryption")).isFalse()
                            preparations++
                            encryption.prepareEncryption(request.sourceFile).use { session ->
                                val checkpoint = session.checkpoint()
                                checkpointFile.writeText(checkpoint.data)
                                File(checkpointFile.path + ".header").writeBytes(checkpoint.header)
                                mapOf(
                                    "provider" to "custom-app",
                                    "encryption" to session.attachmentKey,
                                )
                            }
                        }
                    }

                    override suspend fun upload(
                        request: FileUploadRequest,
                        callback: FileTransferCallback,
                    ): FileTransferResult<String> {
                        val persisted =
                            checkNotNull(database.messageDao().getMessageByTid(MESSAGE_TID))
                                .attachments.orEmpty().single().toAttachment()
                        assertThat(persisted.metadata).isEqualTo(request.attachment.metadata)
                        assertThat(persisted.filePath).isEqualTo(request.sourceFile.path)
                        assertThat(persisted.fileSize).isEqualTo(request.sourceFile.length())
                        val metadata = gson.fromJson(request.attachment.metadata, JsonObject::class.java)
                        val checkpoint = FileEncryptionCheckpoint(
                            metadata.get("encryption").asString,
                            File(checkpointFile.path + ".header").readBytes(),
                            checkpointFile.readText(),
                        )
                        encryption.restoreEncryption(request.sourceFile, checkpoint)
                            .use { session ->
                                val bytes = session.read(0, session.ciphertextSize.toInt())
                                checkpointFile.writeText(session.checkpoint().data)
                                assertThat(checkpointFile.readText()).doesNotContain(session.attachmentKey)
                                if (resuming) assertThat(bytes).isEqualTo(expectedCiphertext.readBytes())
                                else expectedCiphertext.writeBytes(bytes)
                            }
                        return if (resuming) FileTransferResult.Success("custom-object")
                        else FileTransferResult.Interrupted(IOException("Test network interruption"))
                    }

                    override suspend fun download(
                        request: FileDownloadRequest,
                        callback: FileTransferCallback,
                    ): FileTransferResult<String> = error("This test only uploads")

                    override fun cancel(operationId: String) = Unit
                }
            }
            SceytChatUIKit.fileTransfer = config
            val repository = Proxy.newProxyInstance(
                AttachmentsRepository::class.java.classLoader,
                arrayOf(AttachmentsRepository::class.java),
            ) { _, _, _ -> error("This test must not call the chat server") } as AttachmentsRepository
            val logic = PersistenceAttachmentLogicImpl(
                database.messageDao(),
                database.attachmentsDao(),
                database.userDao(),
                database.fileChecksumDao(),
                database.linkDao(),
                MessagesCache(),
                AttachmentsCache(),
                repository,
            )
            val task = TransferTask(saved, saved.messageTid, TransferState.PendingUpload)
            val result = CompletableDeferred<SceytResponse<String>>()
            AttachmentUploadCoordinator(context, logic, scope).upload(
                saved, task, { _, response -> result.complete(response) }, {},
            )

            val response = withTimeout(20_000) { result.await() }
            if (resuming) {
                assertThat(response).isInstanceOf(SceytResponse.Success::class.java)
                assertThat(gson.fromJson(task.attachment.metadata, JsonObject::class.java))
                    .isEqualTo(originalData)
                assertThat(preparations).isEqualTo(0)
            } else {
                assertThat(response).isInstanceOf(SceytResponse.Error::class.java)
                assertThat(preparations).isEqualTo(1)
            }
            assertThat(
                gson.fromJson(task.attachment.metadata, JsonObject::class.java)
                    .get("caption").asString
            )
                .isEqualTo("keep this caption")
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
            database.close()
            SceytChatUIKit.fileTransfer = previous
        }
    }

    private fun message(): MessageDb {
        val attachment = SceytAttachment(
            id = null, messageId = 0, messageTid = MESSAGE_TID, userId = null,
            name = source.name, type = "file", metadata = """{"caption":"keep this caption"}""",
            fileSize = source.length(), createdAt = 0, url = null, filePath = source.path,
            transferState = TransferState.PendingUpload, progressPercent = 0f,
            originalFilePath = source.path, linkPreviewDetails = null,
        )
        val entity = MessageEntity(
            tid = MESSAGE_TID,
            id = null,
            channelId = 1,
            body = "",
            type = "file",
            metadata = null,
            createdAt = 0,
            updatedAt = 0,
            incoming = false,
            isTransient = false,
            silent = false,
            viewOnce = false,
            deliveryStatus = MessageDeliveryStatus.Pending,
            state = MessageState.Unmodified,
            fromId = null,
            markerCount = null,
            mentionedUsersIds = null,
            parentId = null,
            replyCount = 0,
            displayCount = 0,
            autoDeleteAt = null,
            forwardingDetailsDb = null,
            bodyAttribute = null,
            disableMentionsCount = false,
            unList = false,
        )
        return MessageDb(
            messageEntity = entity,
            from = null,
            parent = null,
            attachments = listOf(attachment.toAttachmentDb(0, MESSAGE_TID, 1, false)),
            userMarkers = null,
            reactions = null,
            reactionsTotals = null,
            pendingReactions = null,
            forwardingUser = null,
            mentionedUsers = null,
            poll = null,
            pinnedMessage = null,
            pendingPin = null,
        )
    }

    private companion object {
        const val DATABASE = "pre-upload-restart-test.db"
        const val MESSAGE_TID = 91001L
    }
}
