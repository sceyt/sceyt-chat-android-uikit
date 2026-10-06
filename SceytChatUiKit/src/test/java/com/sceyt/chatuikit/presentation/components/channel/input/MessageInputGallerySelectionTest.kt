package com.sceyt.chatuikit.presentation.components.channel.input

import android.net.Uri
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentsAdapter
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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
