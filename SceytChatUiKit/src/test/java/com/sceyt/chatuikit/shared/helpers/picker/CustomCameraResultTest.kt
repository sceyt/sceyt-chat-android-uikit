package com.sceyt.chatuikit.shared.helpers.picker

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.presentation.components.camera.CustomCameraActivity
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomCameraResultTest {

    @Test
    fun `maps batch gallery result to ordered attachment paths and types`() {
        val intent = Intent().apply {
            putParcelableArrayListExtra(
                CustomCameraActivity.EXTRA_RESULT_SELECTED_MEDIA,
                arrayListOf(
                    selectedMedia("content://media/image/1", "/image.jpg", BottomSheetMediaPicker.MediaType.Image),
                    selectedMedia("content://media/video/2", "/video.mp4", BottomSheetMediaPicker.MediaType.Video),
                )
            )
        }

        assertThat(intent.toCustomCameraAttachments()).containsExactly(
            AttachmentTypeEnum.Image to "/image.jpg",
            AttachmentTypeEnum.Video to "/video.mp4",
        ).inOrder()
    }

    @Test
    fun `maps legacy captured photo result`() {
        val intent = Intent().apply {
            putExtra(CustomCameraActivity.EXTRA_RESULT_URI, "/capture.jpg")
            putExtra(CustomCameraActivity.EXTRA_IS_VIDEO, false)
        }

        assertThat(intent.toCustomCameraAttachments()).containsExactly(
            AttachmentTypeEnum.Image to "/capture.jpg"
        )
    }

    @Test
    fun `maps legacy captured video result`() {
        val intent = Intent().apply {
            putExtra(CustomCameraActivity.EXTRA_RESULT_URI, "/capture.mp4")
            putExtra(CustomCameraActivity.EXTRA_IS_VIDEO, true)
        }

        assertThat(intent.toCustomCameraAttachments()).containsExactly(
            AttachmentTypeEnum.Video to "/capture.mp4"
        )
    }

    @Test
    fun `prefers gallery batch when legacy extras are also present`() {
        val intent = Intent().apply {
            putExtra(CustomCameraActivity.EXTRA_RESULT_URI, "/capture.jpg")
            putExtra(CustomCameraActivity.EXTRA_IS_VIDEO, false)
            putParcelableArrayListExtra(
                CustomCameraActivity.EXTRA_RESULT_SELECTED_MEDIA,
                arrayListOf(
                    selectedMedia("content://media/video/1", "/selected.mp4", BottomSheetMediaPicker.MediaType.Video)
                )
            )
        }

        assertThat(intent.toCustomCameraAttachments()).containsExactly(
            AttachmentTypeEnum.Video to "/selected.mp4"
        )
    }

    @Test
    fun `returns no attachments for empty result`() {
        assertThat(Intent().toCustomCameraAttachments()).isEmpty()
    }

    private fun selectedMedia(
        contentUri: String,
        realPath: String,
        mediaType: BottomSheetMediaPicker.MediaType,
    ) = BottomSheetMediaPicker.SelectedMediaData(
        contentUri = Uri.parse(contentUri),
        realPath = realPath,
        mediaType = mediaType,
    )
}
