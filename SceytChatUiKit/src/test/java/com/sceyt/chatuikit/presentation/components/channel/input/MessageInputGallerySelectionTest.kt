package com.sceyt.chatuikit.presentation.components.channel.input

import android.net.Uri
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.DraftAttachment
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentsAdapter
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.MessageInputActionCallback
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowToast
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MessageInputGallerySelectionTest {
    @Test
    fun `gallery deselection preserves private captures and removes public media`() {
        val context = RuntimeEnvironment.getApplication()
        val photo = attachment(File(context.filesDir, "Photos/capture.jpg"), AttachmentTypeEnum.Image)
        val video = attachment(File(context.filesDir, "Videos/capture.mp4"), AttachmentTypeEnum.Video)
        val gallery = attachment(File("/storage/emulated/0/DCIM/photo.jpg"), AttachmentTypeEnum.Image)
        val document = attachment(File("/storage/emulated/0/document.pdf"), AttachmentTypeEnum.File)
        val attachments = mutableListOf(photo, video, gallery, document)

        applyGalleryResult(attachments)

        assertThat(attachments).containsExactly(photo, video, document).inOrder()
    }

    @Test
    fun `gallery selection keeps both captured and selected gallery photos`() {
        val context = RuntimeEnvironment.getApplication()
        val photo = attachment(File(context.filesDir, "Photos/capture.jpg"), AttachmentTypeEnum.Image)
        val gallery = attachment(File("/storage/emulated/0/DCIM/photo.jpg"), AttachmentTypeEnum.Image)
        val attachments = mutableListOf(photo, gallery)
        val selected = BottomSheetMediaPicker.SelectedMediaData(
            Uri.parse("content://media/external/images/media/1"),
            gallery.filePath,
            BottomSheetMediaPicker.MediaType.Image,
        )

        applyGalleryResult(attachments, listOf(selected))

        assertThat(attachments).containsExactly(photo, gallery).inOrder()
    }

    @Test
    fun `directory name prefix alone does not identify private media`() {
        val context = RuntimeEnvironment.getApplication()
        val attachment = attachment(File(context.filesDir.path + "-other/photo.jpg"), AttachmentTypeEnum.Image)
        val attachments = mutableListOf(attachment)

        applyGalleryResult(attachments)

        assertThat(attachments).isEmpty()
    }

    @Test
    fun `draft update excludes deselected gallery attachment`() {
        val context = RuntimeEnvironment.getApplication()
        val photo = attachment(File(context.filesDir, "Photos/capture.jpg"), AttachmentTypeEnum.Image)
        val gallery = attachment(File("/storage/emulated/0/DCIM/photo.jpg"), AttachmentTypeEnum.Image)
        val attachments = mutableListOf(photo, gallery)

        val attachmentsAtDraftUpdate = applyGalleryResult(attachments)

        assertThat(attachmentsAtDraftUpdate).containsExactly(photo)
    }

    @Test
    fun `deselecting final gallery attachment saves an empty attachment list`() {
        val gallery = attachment(File("/storage/emulated/0/DCIM/photo.jpg"), AttachmentTypeEnum.Image)

        val attachmentsAtDraftUpdate = applyGalleryResult(mutableListOf(gallery))

        assertThat(attachmentsAtDraftUpdate).isEmpty()
    }

    @Test
    fun `removing camera photo clears retained paths used after activity recreation`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val context = controller.get()
            val photo = attachment(File(context.filesDir, "Photos/capture.jpg"), AttachmentTypeEnum.Image)
            val video = attachment(File(context.filesDir, "Videos/capture.mp4"), AttachmentTypeEnum.Video)
            val attachments = mutableListOf(photo, video)
            val retainedPaths = mutableSetOf(
                AttachmentTypeEnum.Image to photo.filePath,
                AttachmentTypeEnum.Video to video.filePath,
            )
            val input = mock<MessageInputView>(defaultAnswer = Answers.CALLS_REAL_METHODS)
            MessageInputView::class.java.getDeclaredField("allAttachments").apply {
                isAccessible = true
                set(input, attachments)
            }
            MessageInputView::class.java.getDeclaredField("attachmentsAdapter").apply {
                isAccessible = true
                set(input, mock<AttachmentsAdapter>())
            }
            MessageInputView::class.java.getDeclaredField("filePickerHelper").apply {
                isAccessible = true
                set(input, FilePickerHelper(context))
            }
            input.setSaveUrlsPlace(retainedPaths)

            // The remove button and Gallery both use this reconciliation before saving the draft.
            MessageInputView::class.java.getDeclaredMethod("removeAttachment", AttachmentItem::class.java).apply {
                isAccessible = true
                invoke(input, AttachmentItem(photo))
            }
            controller.recreate()
            FilePickerHelper(controller.get()).setSaveUrlsPlace(retainedPaths)

            assertThat(attachments).containsExactly(video)
            assertThat(retainedPaths).containsExactly(AttachmentTypeEnum.Video to video.filePath)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `resume check removes attachments whose files were deleted and saves draft`() {
        withInputView { input, activity ->
            val kept = File(activity.filesDir, "kept.jpg").apply { writeText("kept") }
            val deleted = File(activity.filesDir, "deleted.jpg").apply { writeText("deleted") }
            val callback = mock<MessageInputActionCallback>()
            input.setInputActionsCallback(callback)
            input.addAttachment(
                AttachmentTypeEnum.Image to kept.path,
                AttachmentTypeEnum.Image to deleted.path,
            )
            deleted.delete()
            assertThat(ShadowToast.shownToastCount()).isEqualTo(0)

            runBlocking { input.removeMissingAttachments() }

            assertThat(input.attachmentPaths()).containsExactly(kept.path)
            val draftAttachments = argumentCaptor<List<Attachment>>()
            verify(callback, atLeastOnce()).updateDraftMessage(
                anyOrNull(), draftAttachments.capture(), anyOrNull(), any(), anyOrNull(), anyOrNull(), any()
            )
            assertThat(draftAttachments.lastValue.map { it.filePath }).containsExactly(kept.path)
            assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
            assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(activity.getString(R.string.sceyt_missing_attachments_removed))
        }
    }

    @Test
    fun `resume check with all files present shows no toast`() {
        withInputView { input, activity ->
            val kept = File(activity.filesDir, "kept.jpg").apply { writeText("kept") }
            input.addAttachment(AttachmentTypeEnum.Image to kept.path)

            runBlocking { input.removeMissingAttachments() }

            assertThat(input.attachmentPaths()).containsExactly(kept.path)
            assertThat(ShadowToast.shownToastCount()).isEqualTo(0)
        }
    }

    @Test
    fun `draft restore drops missing attachments with one missing files toast`() {
        withInputView { input, activity ->
            val kept = File(activity.filesDir, "kept.jpg").apply { writeText("kept") }
            val missing = File(activity.filesDir, "missing.jpg")
            val missingVideo = File(activity.filesDir, "missing.mp4")
            input.setDraftMessage(
                DraftMessage(
                    channelId = 1,
                    body = null,
                    createdAt = 0,
                    mentionUsers = null,
                    replyOrEditMessage = null,
                    isReply = false,
                    bodyAttributes = null,
                    attachments = listOf(
                        DraftAttachment(1, kept.path, AttachmentTypeEnum.Image),
                        DraftAttachment(1, missing.path, AttachmentTypeEnum.Image),
                        DraftAttachment(1, missingVideo.path, AttachmentTypeEnum.Video),
                    ),
                    voiceAttachment = null,
                    viewOnce = false,
                )
            )

            assertThat(input.attachmentPaths()).containsExactly(kept.path)
            assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
            assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(activity.getString(R.string.sceyt_missing_attachments_removed))
        }
    }

    private fun withInputView(block: (MessageInputView, ComponentActivity) -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        val clientWrapper = Mockito.mockStatic(ClientWrapper::class.java)
        clientWrapper.`when`<Long> { ClientWrapper.generateTid() }.thenReturn(1L)
        try {
            val activity = controller.get()
            activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
            val input = MessageInputView(activity)
            activity.setContentView(input)
            controller.start().resume()
            block(input, activity)
        } finally {
            clientWrapper.close()
            controller.pause().stop().destroy()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun MessageInputView.attachmentPaths(): List<String> =
        (MessageInputView::class.java.getDeclaredField("allAttachments").apply {
            isAccessible = true
        }.get(this) as List<Attachment>).map { it.filePath }

    private fun applyGalleryResult(
        attachments: MutableList<Attachment>,
        selected: List<BottomSheetMediaPicker.SelectedMediaData> = emptyList(),
    ): List<Attachment> {
        // Isolate attachment reconciliation from resource inflation and draft persistence.
        val input = mock<MessageInputView>(defaultAnswer = Answers.CALLS_REAL_METHODS)
        doReturn(RuntimeEnvironment.getApplication()).whenever(input).context
        var attachmentsAtDraftUpdate: List<Attachment>? = null
        // addAttachment saves the draft synchronously; capture its input before later mutations.
        doAnswer {
            attachmentsAtDraftUpdate = attachments.toList()
            null
        }.whenever(input).addAttachment(*selected.map {
            it.mediaType.value to it.realPath
        }.toTypedArray())
        MessageInputView::class.java.getDeclaredField("allAttachments").apply {
            isAccessible = true
            set(input, attachments)
        }
        MessageInputView::class.java.getDeclaredField("attachmentsAdapter").apply {
            isAccessible = true
            set(input, mock<AttachmentsAdapter>())
        }
        MessageInputView::class.java.getDeclaredMethod("onMediaPicked", List::class.java).apply {
            isAccessible = true
            invoke(input, selected)
        }
        return checkNotNull(attachmentsAtDraftUpdate)
    }

    private fun attachment(file: File, type: AttachmentTypeEnum): Attachment =
        Attachment.Builder(file.path, "", type.value).setName(file.name).build()
}
