package com.sceyt.chatuikit.presentation.components.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CameraNavigatorTest {

    @Test
    fun `returns every selected gallery item and finishes camera activity`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val navigator = CameraNavigator(activity, mock<ActivityResultLauncher<Intent>>())
        val selectedMedia = listOf(
            selectedMedia("content://media/image/1", "/image.jpg", BottomSheetMediaPicker.MediaType.Image),
            selectedMedia("content://media/video/2", "/video.mp4", BottomSheetMediaPicker.MediaType.Video),
        )

        navigator.returnSelectedMedia(selectedMedia)

        val shadowActivity = shadowOf(activity)
        val returnedMedia = shadowActivity.resultIntent
            .parcelableArrayList<BottomSheetMediaPicker.SelectedMediaData>(
                CustomCameraActivity.EXTRA_RESULT_SELECTED_MEDIA
            )
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(returnedMedia).containsExactlyElementsIn(selectedMedia).inOrder()
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun `ignores an empty gallery selection`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val navigator = CameraNavigator(activity, mock<ActivityResultLauncher<Intent>>())

        navigator.returnSelectedMedia(emptyList())

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultIntent).isNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun `keeps captured media on the legacy single result contract`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val navigator = CameraNavigator(activity, mock<ActivityResultLauncher<Intent>>())

        navigator.returnResult("/capture.mp4", isVideo = true)

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(
            shadowActivity.resultIntent.getStringExtra(CustomCameraActivity.EXTRA_RESULT_URI)
        ).isEqualTo("/capture.mp4")
        assertThat(
            shadowActivity.resultIntent.getBooleanExtra(CustomCameraActivity.EXTRA_IS_VIDEO, false)
        ).isTrue()
        assertThat(activity.isFinishing).isTrue()
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
